using System.Net;
using System.Net.Security;
using System.Net.Sockets;
using System.Security.Authentication;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Text.Json;

namespace PocketPad;

public sealed class RemoteServer : IDisposable
{
    readonly IInputSink sink;
    readonly TextFocusWatcher? watcher;
    readonly Func<bool>? textFocus;
    readonly Func<long>? focusRevision;
    readonly X509Certificate2 cert;
    readonly CancellationTokenSource stop=new();
    readonly SemaphoreSlim slots=new(4), controller=new(1);
    readonly TcpListener listener;
    readonly HashSet<Task> clients=new();
    readonly object gate=new();
    public string Token {get;}=Convert.ToHexString(RandomNumberGenerator.GetBytes(24)).ToLowerInvariant();
    public string Fingerprint {get;}
    public int Port => ((IPEndPoint)listener.LocalEndpoint).Port;
    public event Action<string>? Status;
    Task? accept;
    public RemoteServer(IPAddress address,int port,IInputSink sink,Func<bool>? textFocus=null,Func<long>? focusRevision=null)
    {
        this.sink=sink; listener=new(address,port);
        this.focusRevision=focusRevision;
        if(textFocus!=null)this.textFocus=textFocus;
        else if(sink is WindowsInput){watcher=new();this.textFocus=()=>watcher.Editable;this.focusRevision=()=>watcher.Revision;}
        using var rsa=RSA.Create(2048);
        var req=new CertificateRequest("CN=PocketPad",rsa,HashAlgorithmName.SHA256,RSASignaturePadding.Pkcs1);
        using var generated=req.CreateSelfSigned(DateTimeOffset.UtcNow.AddMinutes(-5),DateTimeOffset.UtcNow.AddDays(7));
        cert=X509CertificateLoader.LoadPkcs12(generated.Export(X509ContentType.Pfx),null);
        Fingerprint=Convert.ToHexString(SHA256.HashData(cert.RawData)).ToLowerInvariant();
    }
    public void Start(){listener.Start();accept=Accept();}
    async Task Accept()
    {
        try { while(!stop.IsCancellationRequested) {
            var tcp=await listener.AcceptTcpClientAsync(stop.Token);
            if(!await slots.WaitAsync(0)){tcp.Dispose();continue;}
            var task=Handle(tcp); lock(gate) clients.Add(task);
            _=task.ContinueWith(t=>{lock(gate) clients.Remove(t);},TaskScheduler.Default);
        }} catch(OperationCanceledException){} catch(SocketException) when(stop.IsCancellationRequested){}
    }
    async Task Handle(TcpClient tcp)
    {
        bool owns=false;
        try {
            using(tcp) using(var tls=new SslStream(tcp.GetStream(),false)) {
                tcp.NoDelay=true;
                using var auth=CancellationTokenSource.CreateLinkedTokenSource(stop.Token); auth.CancelAfter(8000);
                await tls.AuthenticateAsServerAsync(new SslServerAuthenticationOptions {ServerCertificate=cert,EnabledSslProtocols=SslProtocols.Tls12|SslProtocols.Tls13},auth.Token);
                var hello=JsonDocument.Parse(await ReadLine(tls,auth.Token));
                using(hello) {
                    var root=hello.RootElement;
                    if(root.GetProperty("type").GetString()!="auth"||root.GetProperty("version").GetInt32()!=1) return;
                    string token=root.GetProperty("token").GetString()??"";
                    if(!CryptographicOperations.FixedTimeEquals(Encoding.UTF8.GetBytes(Token),Encoding.UTF8.GetBytes(token))) return;
                }
                if(!await controller.WaitAsync(0)){await Write(tls,"{\"ok\":false,\"error\":\"Another phone is connected\"}\n",auth.Token);return;}
                owns=true;
                await Write(tls,"{\"ok\":true,\"version\":1}\n",auth.Token);
                Status?.Invoke("Phone connected · controls active");
                using var session=CancellationTokenSource.CreateLinkedTokenSource(stop.Token);
                long clickedAt=0;
                if(watcher!=null)watcher.Enabled=true;
                async Task PublishFocus(){
                    bool? previous=null;long sentClick=0,sentFocus=-1;
                    try{
                        while(!session.IsCancellationRequested){
                            long click=Interlocked.Read(ref clickedAt);
                            if(click==0 || Environment.TickCount64-click>=350){
                                long focus=focusRevision?.Invoke()??0;
                                bool editable=textFocus!();
                                if(previous!=editable || sentClick!=click || sentFocus!=focus){
                                    await Write(tls,editable ? "{\"type\":\"focus\",\"editable\":true}\n" : "{\"type\":\"focus\",\"editable\":false}\n",session.Token);
                                    FocusTrace.Write("sent editable="+editable+" repeatedClick="+(sentClick!=click));
                                    previous=editable;sentClick=click;sentFocus=focus;
                                }
                            }
                            await Task.Delay(100,session.Token);
                        }
                    }catch(Exception ex) when(ex is IOException or OperationCanceledException or ObjectDisposedException){session.Cancel();}
                }
                var focusTask=textFocus==null ? Task.CompletedTask : PublishFocus();
                try{
                    int packets=0; var window=System.Diagnostics.Stopwatch.StartNew();
                    while(!session.IsCancellationRequested){
                        using var idle=CancellationTokenSource.CreateLinkedTokenSource(session.Token); idle.CancelAfter(5000);
                        string line=await ReadLine(tls,idle.Token);
                        if(window.ElapsedMilliseconds>=1000){window.Restart();packets=0;}
                        if(++packets>300) throw new InvalidDataException("Too many input events");
                        using var doc=JsonDocument.Parse(line);sink.Apply(doc.RootElement);
                        var e=doc.RootElement;
                        if(e.TryGetProperty("type",out var type)&&type.GetString()=="button" &&
                           e.TryGetProperty("button",out var button)&&button.GetString()=="left" &&
                           e.TryGetProperty("down",out var down)&&down.ValueKind==JsonValueKind.False)
                            Interlocked.Exchange(ref clickedAt,Environment.TickCount64);
                    }
                }finally{session.Cancel();await focusTask;if(watcher!=null)watcher.Enabled=false;}

            }
        } catch(Exception ex) when(ex is IOException or SocketException or AuthenticationException or OperationCanceledException or JsonException or InvalidOperationException or KeyNotFoundException or ArgumentException or FormatException or OverflowException) {
            if(owns&&!stop.IsCancellationRequested) Status?.Invoke(ex is OperationCanceledException ? "Phone disconnected · connect again" : "Disconnected: "+ex.Message);
        } finally {if(owns){try{sink.Release();}catch(IOException){} controller.Release();if(!stop.IsCancellationRequested)Status?.Invoke("Ready · scan to connect");}slots.Release();}
    }
    public static async Task<string> ReadLine(Stream stream,CancellationToken ct)
    {
        // Bound memory before parsing; malformed peers cannot allocate an unlimited line.
        byte[] bytes=new byte[16384],one=new byte[1]; int count=0;
        while(count<bytes.Length){int n=await stream.ReadAsync(one,ct);if(n==0)throw new EndOfStreamException();if(one[0]==10)return new UTF8Encoding(false,true).GetString(bytes,0,count);bytes[count++]=one[0];}
        throw new InvalidDataException("Packet too large");
    }
    static Task Write(Stream s,string text,CancellationToken ct)=>s.WriteAsync(Encoding.UTF8.GetBytes(text),ct).AsTask();
    public async Task StopAsync(){stop.Cancel();listener.Stop();if(accept!=null)await accept;Task[] all;lock(gate)all=clients.ToArray();await Task.WhenAll(all);}
    public void Dispose(){watcher?.Dispose();cert.Dispose();stop.Dispose();slots.Dispose();controller.Dispose();}
}
