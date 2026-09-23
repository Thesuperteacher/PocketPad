using System.Net;
using System.Net.Security;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace PocketPad;
static class SelfTest
{
    sealed class Recorder:IInputSink {public List<string> Events=new();public int Releases;public void Apply(JsonElement e){lock(Events)Events.Add(e.GetRawText());}public void Release()=>Interlocked.Increment(ref Releases);}
    public static async Task RunBridge()
    {
        var sink=new Recorder();using var server=new RemoteServer(IPAddress.Loopback,19876,sink,()=>File.Exists(Path.Combine(AppContext.BaseDirectory,"test-focus.flag")));server.Start();
        string dir=AppContext.BaseDirectory;
        File.WriteAllText(Path.Combine(dir,"test-pairing.txt"),$"pocketpad://127.0.0.1:19876?key={server.Token}&fp={server.Fingerprint}");
        using var data=QRCoder.QRCodeGenerator.GenerateQrCode(File.ReadAllText(Path.Combine(dir,"test-pairing.txt")),QRCoder.QRCodeGenerator.ECCLevel.M);
        using var qr=new QRCoder.PngByteQRCode(data);File.WriteAllBytes(Path.Combine(dir,"test-qr.png"),qr.GetGraphic(6));
        for(int i=0;i<240&&!File.Exists(Path.Combine(dir,"test-stop.flag"));i++)await Task.Delay(1000);
        await server.StopAsync();lock(sink.Events)File.WriteAllLines(Path.Combine(dir,"bridge-events.jsonl"),sink.Events);
        File.WriteAllText(Path.Combine(dir,"bridge-release.txt"),sink.Releases.ToString());
    }
    static async Task FocusProtocol(List<string> log)
    {
        int editable=0;long focusId=0;var sink=new Recorder();
        using var server=new RemoteServer(IPAddress.Loopback,0,sink,()=>Volatile.Read(ref editable)!=0,()=>Interlocked.Read(ref focusId));server.Start();
        using var client=new TcpClient();await client.ConnectAsync(IPAddress.Loopback,server.Port);
        using var tls=new SslStream(client.GetStream(),false,(_,c,_,_)=>c!=null&&Convert.ToHexString(SHA256.HashData(c.GetRawCertData())).ToLowerInvariant()==server.Fingerprint);
        await tls.AuthenticateAsClientAsync("PocketPad");
        await tls.WriteAsync(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new{type="auth",version=1,token=server.Token})+"\n"));
        using var timeout=new CancellationTokenSource(10000);
        await RemoteServer.ReadLine(tls,timeout.Token);
        async Task Expect(bool expected){using var doc=JsonDocument.Parse(await RemoteServer.ReadLine(tls,timeout.Token));var e=doc.RootElement;if(e.GetProperty("type").GetString()!="focus"||e.GetProperty("editable").GetBoolean()!=expected||e.EnumerateObject().Count()!=2)throw new Exception("Incorrect or excessive focus payload");}
        await Expect(false);Volatile.Write(ref editable,1);await Expect(true);
        Interlocked.Increment(ref focusId);await Expect(true);
        await tls.WriteAsync(Encoding.UTF8.GetBytes("{\"type\":\"button\",\"button\":\"left\",\"down\":false}\n"));await Expect(true);
        Volatile.Write(ref editable,0);await Expect(false);
        tls.Close();await server.StopAsync();
        log.Add("PASS: authenticated editable-focus transitions, switching between editable fields, repeated click and minimal boolean payload");
    }
    public static async Task<int> Run()
    {
        string report=Path.Combine(AppContext.BaseDirectory,"self-test.txt");var log=new List<string>();
        try {
            var sink=new Recorder();using var server=new RemoteServer(IPAddress.Loopback,0,sink);server.Start();
            async Task<(TcpClient,SslStream)> Connect(string token,bool pin=true){var tcp=new TcpClient();await tcp.ConnectAsync(IPAddress.Loopback,server.Port);var tls=new SslStream(tcp.GetStream(),false,(_,c,_,_)=>c!=null&&(!pin||Convert.ToHexString(SHA256.HashData(c.GetRawCertData())).ToLowerInvariant()==server.Fingerprint));await tls.AuthenticateAsClientAsync("PocketPad");await tls.WriteAsync(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new{type="auth",version=1,token})+"\n"));return(tcp,tls);}
            using var timeout=new CancellationTokenSource(15000);
            var (bad,badTls)=await Connect("wrong");try{await RemoteServer.ReadLine(badTls,timeout.Token);throw new Exception("Unauthenticated client accepted");}catch(EndOfStreamException){log.Add("PASS: invalid pairing token rejected");}badTls.Dispose();bad.Dispose();
            var (client,tls)=await Connect(server.Token);var ok=await RemoteServer.ReadLine(tls,timeout.Token);if(!ok.Contains("true"))throw new Exception("Auth failed");log.Add("PASS: TLS certificate pin and valid pairing accepted");
            var (second,secondTls)=await Connect(server.Token);if(!(await RemoteServer.ReadLine(secondTls,timeout.Token)).Contains("false"))throw new Exception("Multiple controllers accepted");secondTls.Dispose();second.Dispose();log.Add("PASS: concurrent controller rejected");
            string[] events={"{\"type\":\"move\",\"x\":12,\"y\":-8}","{\"type\":\"button\",\"button\":\"left\",\"down\":true}","{\"type\":\"text\",\"text\":\"Hola ñ 🙂\"}","{\"type\":\"key\",\"key\":\"Enter\"}"};
            await tls.WriteAsync(Encoding.UTF8.GetBytes(string.Join("\n",events)+"\n"));
            for(int i=0;i<100&&sink.Events.Count<4;i++)await Task.Delay(10);
            if(sink.Events.Count!=4||!sink.Events[2].Contains("Hola"))throw new Exception("Input delivery failed");log.Add("PASS: ordered movement, drag, Unicode text and key delivery");
            tls.Dispose();client.Dispose();for(int i=0;i<100&&sink.Releases==0;i++)await Task.Delay(10);if(sink.Releases==0)throw new Exception("Release missing");log.Add("PASS: disconnect releases held input");
            var (large,largeTls)=await Connect(server.Token);await RemoteServer.ReadLine(largeTls,timeout.Token);await largeTls.WriteAsync(Encoding.UTF8.GetBytes(new string('x',16385)+"\n"));try{await RemoteServer.ReadLine(largeTls,timeout.Token);throw new Exception("Oversize accepted");}catch(IOException){log.Add("PASS: oversized packet disconnected");}largeTls.Dispose();large.Dispose();
            await server.StopAsync();log.Add("PASS: server shutdown completes");await FocusProtocol(log);File.WriteAllLines(report,log);return 0;
        }catch(Exception ex){log.Add("FAIL: "+ex);File.WriteAllLines(report,log);return 1;}
    }
}
