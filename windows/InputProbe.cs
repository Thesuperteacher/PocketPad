using System.Runtime.InteropServices;
using System.Text.Json;

namespace PocketPad;
// Explicit, interactive verification mode. Input is sent only while this probe owns focus.
public sealed class InputProbe : Form
{
    [DllImport("user32.dll")] static extern IntPtr GetForegroundWindow();
    readonly TextBox box=new(){Dock=DockStyle.Top,Multiline=true,Height=100};
    readonly Button target=new(){Text="Test click target",Dock=DockStyle.Top,Height=70};
    readonly WindowsInput sink=new();readonly List<string> log=new();readonly System.Windows.Forms.Timer timer=new(){Interval=200};
    int stage,clicks;Point original;
    public InputProbe()
    {
        Text="PocketPad · input verification";Size=new(450,280);StartPosition=FormStartPosition.CenterScreen;Controls.Add(target);Controls.Add(box);target.Click+=(_,_)=>clicks++;
        Shown+=(_,_)=>{original=Cursor.Position;Activate();box.Focus();timer.Start();};
        timer.Tick+=(_,_)=>{
            try {
                if(GetForegroundWindow()!=Handle)throw new Exception("Probe does not own foreground focus; no input sent.");
                switch(stage++){
                    case 0:box.Focus();Apply("{\"type\":\"text\",\"text\":\"PocketPad ñ Ω\"}");break;
                    case 1:if(box.Text!="PocketPad ñ Ω")throw new Exception("Unicode input mismatch: "+box.Text);log.Add("PASS: Windows SendInput Unicode typing into the probe textbox");var p=target.PointToScreen(new Point(target.Width/2,target.Height/2));Cursor.Position=p;Apply("{\"type\":\"button\",\"button\":\"left\",\"down\":true}");Apply("{\"type\":\"button\",\"button\":\"left\",\"down\":false}");break;
                    case 2:if(clicks!=1)throw new Exception("Click did not reach probe button");log.Add("PASS: Windows mouse down/up triggers one button click");var before=Cursor.Position;Apply("{\"type\":\"move\",\"x\":15,\"y\":5}");if(Cursor.Position==before)throw new Exception("Pointer did not move");log.Add("PASS: Windows relative pointer movement");Finish();break;
                }
            }catch(Exception ex){log.Add("FAIL: "+ex.Message);Finish();}
        };
    }
    void Apply(string json){using var doc=JsonDocument.Parse(json);sink.Apply(doc.RootElement);}
    void Finish(){timer.Stop();sink.Release();Cursor.Position=original;File.WriteAllLines(Path.Combine(AppContext.BaseDirectory,"input-test.txt"),log);Close();}
}
