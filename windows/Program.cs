using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using QRCoder;

namespace PocketPad;

internal static class Program
{
    [STAThread] static void Main(string[] args)
    {
        if(args.Length==2&&args[0]=="--focus-diagnose"){TextFocusWatcher.Diagnose(args[1]);return;}
        if(args.Contains("--self-test")){Environment.Exit(SelfTest.Run().GetAwaiter().GetResult());return;}
        if(args.Contains("--test-bridge")){SelfTest.RunBridge().GetAwaiter().GetResult();return;}
        ApplicationConfiguration.Initialize(); Application.Run(args.Contains("--focus-test") ? new FocusProbe() : args.Contains("--input-test") ? new InputProbe() : new HostForm(args.Contains("--preview"), args.Length==2 && args[0]=="--usb-autostart" ? args[1] : null));
    }
}

public sealed class HostForm : Form
{
    readonly ComboBox networks=new(){DropDownStyle=ComboBoxStyle.DropDownList,Width=400};
    readonly PictureBox qr=new(){Size=new(292,292),SizeMode=PictureBoxSizeMode.Zoom,BackColor=Color.White};
    readonly Label state=new(){Text="Stopped · your PC is not accepting controls",AutoSize=true,ForeColor=Color.FromArgb(56,220,172)};
    readonly Button toggle=new(){Text="Start connection",AutoSize=true};
    readonly TextBox pairing=new(){ReadOnly=true,Multiline=true,Width=440,Height=54};
    RemoteServer? server; bool closing;
    record NetworkChoice(string Label,IPAddress Address){public override string ToString()=>Label;}
    public HostForm(bool preview=false,string? setupDirectory=null)
    {
        Text="PocketPad · PC companion";ClientSize=new(540,810);MinimumSize=new(560,850);StartPosition=FormStartPosition.CenterScreen;
        BackColor=Color.FromArgb(17,24,35);ForeColor=Color.White;Font=new("Segoe UI",10);
        var panel=new FlowLayoutPanel {Dock=DockStyle.Fill,FlowDirection=FlowDirection.TopDown,WrapContents=false,AutoScroll=true,Padding=new(32)};Controls.Add(panel);
        panel.Controls.Add(new Label {Text="PocketPad",Font=new("Segoe UI",27,FontStyle.Bold),AutoSize=true});
        panel.Controls.Add(new Label {Text="Your phone. Your trackpad.",AutoSize=true,Margin=new(3,0,0,22)});
        panel.Controls.Add(new Label {Text="1  Choose a connection",AutoSize=true});
        networks.Items.Add(new NetworkChoice("USB cable / this PC only",IPAddress.Loopback));
        foreach(var n in NetworkInterface.GetAllNetworkInterfaces().Where(n=>n.OperationalStatus==OperationalStatus.Up))
            foreach(var a in n.GetIPProperties().UnicastAddresses.Where(a=>a.Address.AddressFamily==AddressFamily.InterNetwork&&!IPAddress.IsLoopback(a.Address)))
                networks.Items.Add(new NetworkChoice(n.Name+" · "+a.Address,a.Address));
        networks.ForeColor=Color.Black;pairing.ForeColor=Color.Black;
        networks.DrawMode=DrawMode.OwnerDrawFixed;networks.DrawItem+=(_,e)=>{e.DrawBackground();if(e.Index>=0)TextRenderer.DrawText(e.Graphics,networks.Items[e.Index]?.ToString()??"",networks.Font,e.Bounds,Color.Black,TextFormatFlags.VerticalCenter|TextFormatFlags.Left);};
        qr.Paint+=(_,e)=>{if(qr.Image==null){e.Graphics.Clear(Color.FromArgb(27,39,54));TextRenderer.DrawText(e.Graphics,"Start a connection\nto show your QR code",Font,qr.ClientRectangle,Color.LightGray,TextFormatFlags.HorizontalCenter|TextFormatFlags.VerticalCenter);}};
        networks.SelectedIndex=0; panel.Controls.Add(networks);panel.Controls.Add(toggle);toggle.Click+=async(_,_)=>await Toggle();
        panel.Controls.Add(new Label {Text="2  In the phone app, tap Scan PC code",AutoSize=true,Margin=new(3,18,0,8)});
        panel.Controls.Add(qr);panel.Controls.Add(state);
        panel.Controls.Add(new Label {Text="Or copy this pairing link to the phone:",AutoSize=true,Margin=new(3,14,0,4)});panel.Controls.Add(pairing);
        var copy=new Button {Text="Copy pairing link",AutoSize=true};copy.Click+=(_,_)=>{if(pairing.Text.Length>0)Clipboard.SetText(pairing.Text);};panel.Controls.Add(copy);
        panel.Controls.Add(new Label {Text="Wi-Fi: choose your network above. Allow PocketPad on\nyour private network if Windows asks.\nUSB: enable USB tethering and select its network, or use\nthe included USB helper with USB debugging.\nBluetooth: pair from the phone app; this companion can stay closed.",AutoSize=true,Margin=new(3,16,0,0),ForeColor=Color.LightGray});
        FormClosing+=async(_,e)=>{if(server!=null&&!closing){e.Cancel=true;closing=true;await Stop();Close();}};
        if(setupDirectory!=null)
        {
            var directory=Path.GetFullPath(setupDirectory);Directory.CreateDirectory(directory);
            var tray=new NotifyIcon {Icon=SystemIcons.Application,Text="PocketPad USB companion",Visible=true};
            var menu=new ContextMenuStrip();menu.Items.Add("Open PocketPad",null,(_,_)=>{Show();WindowState=FormWindowState.Normal;Activate();});menu.Items.Add("Stop and exit",null,(_,_)=>Close());tray.ContextMenuStrip=menu;
            tray.DoubleClick+=(_,_)=>{Show();WindowState=FormWindowState.Normal;Activate();};FormClosed+=(_,_)=>{tray.Visible=false;tray.Dispose();menu.Dispose();};
            Shown+=async(_,_)=>{await Toggle();if(server!=null){File.WriteAllText(Path.Combine(directory,"pairing.txt"),pairing.Text);File.WriteAllText(Path.Combine(directory,"status.txt"),state.Text);server.Status+=value=>File.WriteAllText(Path.Combine(directory,"status.txt"),value);}};
        }
        if(preview) Shown+=(_,_)=>{var timer=new System.Windows.Forms.Timer {Interval=300};timer.Tick+=(_,_)=>{timer.Stop();using var bmp=new Bitmap(Width,Height);DrawToBitmap(bmp,new Rectangle(0,0,Width,Height));bmp.Save(Path.Combine(AppContext.BaseDirectory,"host-preview.png"));timer.Dispose();Close();};timer.Start();};
    }
    async Task Toggle()
    {
        toggle.Enabled=false;
        try {
            if(server!=null){await Stop();return;}
            var choice=(NetworkChoice)networks.SelectedItem!;
            server=new RemoteServer(choice.Address,19876,new WindowsInput());
            server.Status+=s=>{if(!IsDisposed&&IsHandleCreated)BeginInvoke(()=>state.Text=s);};server.Start();
            pairing.Text=$"pocketpad://{choice.Address}:19876?key={server.Token}&fp={server.Fingerprint}";
            using var data=QRCodeGenerator.GenerateQrCode(pairing.Text,QRCodeGenerator.ECCLevel.M);using var code=new QRCode(data);
            qr.Image?.Dispose();qr.Image=code.GetGraphic(6,Color.Black,Color.White,true);
            state.Text="Ready · scan to connect";toggle.Text="Stop & disconnect";networks.Enabled=false;
        }catch(Exception ex){if(server!=null)await Stop();MessageBox.Show(ex.Message,"Could not start PocketPad");}
        finally{toggle.Enabled=true;}
    }
    async Task Stop(){var old=server;server=null;if(old!=null){await old.StopAsync();old.Dispose();}qr.Image?.Dispose();qr.Image=null;pairing.Clear();state.Text="Stopped · your PC is not accepting controls";toggle.Text="Start connection";networks.Enabled=true;}
}
