using System.Windows.Automation;
namespace PocketPad;
sealed class FocusProbe : Form
{
    public FocusProbe(){
        var edit=new TextBox {Text="Test field"};var readOnly=new TextBox {Text="Read only",ReadOnly=true,Top=40};var button=new Button {Text="Test button",Top=80};
        Controls.AddRange([edit,readOnly,button]);
        Shown+=async(_,_)=>{
            var handles=new[]{edit.Handle,readOnly.Handle,button.Handle};
            try{
                var result=await Task.Run(()=>handles.Select(h=>TextFocusWatcher.IsEditable(AutomationElement.FromHandle(h))).ToArray());
                if(!result.SequenceEqual(new[]{true,false,false}))throw new Exception("Unexpected control classification: "+string.Join(",",result));
                File.WriteAllText(Path.Combine(AppContext.BaseDirectory,"focus-test.txt"),"PASS: editable TextBox detected; read-only TextBox and Button excluded.");
            }catch(Exception ex){File.WriteAllText(Path.Combine(AppContext.BaseDirectory,"focus-test.txt"),"FAIL: "+ex);Environment.ExitCode=1;}
            Close();
        };
    }
}
