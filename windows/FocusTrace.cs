namespace PocketPad;
static class FocusTrace
{
    static readonly string? path=Environment.GetEnvironmentVariable("POCKETPAD_FOCUS_TRACE");
    static readonly object gate=new();
    public static void Write(string status){if(string.IsNullOrEmpty(path)||!File.Exists(path+".enabled"))return;try{lock(gate)File.AppendAllText(path,DateTimeOffset.Now.ToString("O")+" "+status+Environment.NewLine);}catch(IOException){}}
}
