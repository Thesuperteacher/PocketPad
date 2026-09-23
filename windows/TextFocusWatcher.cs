using System.Windows.Automation;

namespace PocketPad;

// Only an editable flag crosses the connection. Never read values or names.
sealed class TextFocusWatcher : IDisposable
{
    volatile bool enabled, editable, stopped;
    readonly Thread worker;
    long revision;
    public long Revision=>Interlocked.Read(ref revision);
    public bool Enabled { set { enabled=value; if(!value)editable=false; } }
    public bool Editable => enabled && editable;
    public TextFocusWatcher()
    {
        worker=new Thread(()=>{
            string? previous=null,previousKey=null;
            while(!stopped){
                if(enabled){
                    string state;
                    try{var e=AutomationElement.FocusedElement;bool next=IsEditable(e);string key=e==null?"":string.Join(".",e.GetRuntimeId());bool changed=previousKey!=key||editable!=next;editable=next;if(changed){previousKey=key;Interlocked.Increment(ref revision);}state=$"focus process={e?.Current.ProcessId} type={e?.Current.ControlType.ProgrammaticName} editable={editable}";}
                    catch(Exception ex){editable=false;state="focus error="+ex.GetType().Name;}
                    if(state!=previous){FocusTrace.Write(state);previous=state;}
                }
                Thread.Sleep(150);
            }
        }) {IsBackground=true,Name="PocketPad text focus"};
        worker.SetApartmentState(ApartmentState.MTA);worker.Start();
    }
    internal static bool IsEditable(AutomationElement? element)
    {
        if(element==null || !element.Current.IsEnabled)return false;
        if(element.TryGetCurrentPattern(ValuePattern.Pattern,out var value))
            return !((ValuePattern)value).Current.IsReadOnly;
        if(element.TryGetCurrentPattern(TextPattern.Pattern,out var text))
            return ((TextPattern)text).DocumentRange.GetAttributeValue(TextPattern.IsReadOnlyAttribute) is bool readOnly && !readOnly;
        return false;
    }
    internal static void Diagnose(string path)
    {
        var worker=new Thread(()=>{
            string? previous=null;
            for(int i=0;i<60;i++){
                string state;
                try {
                    var e=AutomationElement.FocusedElement;
                    if(e==null)state="No focused element";
                    else {
                        var value=e.TryGetCurrentPattern(ValuePattern.Pattern,out var vp) ? ((ValuePattern)vp).Current.IsReadOnly.ToString() : "unavailable";
                        var text=e.TryGetCurrentPattern(TextPattern.Pattern,out var tp) ? ((TextPattern)tp).DocumentRange.GetAttributeValue(TextPattern.IsReadOnlyAttribute) : null;
                        state=$"process={e.Current.ProcessId} type={e.Current.ControlType.ProgrammaticName} enabled={e.Current.IsEnabled} valueReadOnly={value} textReadOnly={(text is bool b ? b.ToString() : "unavailable")} editable={IsEditable(e)}";
                    }
                }catch(Exception ex){state="ERROR "+ex.GetType().Name+": "+ex.Message;}
                if(state!=previous){File.AppendAllText(path,DateTimeOffset.Now.ToString("O")+" "+state+Environment.NewLine);previous=state;}
                Thread.Sleep(250);
            }
        });worker.SetApartmentState(ApartmentState.MTA);worker.Start();worker.Join(17000);
    }
    public void Dispose(){stopped=true;enabled=false;worker.Join(300);}
}
