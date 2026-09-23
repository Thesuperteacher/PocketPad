using System.Runtime.InteropServices;
using System.Text.Json;

namespace PocketPad;

public interface IInputSink { void Apply(JsonElement e); void Release(); }

public sealed class WindowsInput : IInputSink
{
    [StructLayout(LayoutKind.Sequential)] struct INPUT { public uint type; public UNION data; }
    [StructLayout(LayoutKind.Explicit)] struct UNION { [FieldOffset(0)] public MOUSE mouse; [FieldOffset(0)] public KEY key; }
    [StructLayout(LayoutKind.Sequential)] struct MOUSE { public int dx, dy; public uint mouseData, flags, time; public UIntPtr extra; }
    [StructLayout(LayoutKind.Sequential)] struct KEY { public ushort vk, scan; public uint flags, time; public UIntPtr extra; }
    [DllImport("user32.dll", SetLastError = true)] static extern uint SendInput(uint count, INPUT[] inputs, int size);
    int held;
    static readonly Dictionary<string, ushort> Keys = new() { ["Enter"]=13,["Backspace"]=8,["Tab"]=9,["Escape"]=27,["Delete"]=46,["Left"]=37,["Up"]=38,["Right"]=39,["Down"]=40,["Home"]=36,["End"]=35,["PageUp"]=33,["PageDown"]=34,["VolumeUp"]=175,["VolumeDown"]=174,["Mute"]=173,["PlayPause"]=179,["F5"]=116 };
    static void Send(params INPUT[] inputs) { if (SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<INPUT>()) != inputs.Length) throw new IOException("Windows blocked input. Select a normal desktop app; administrator windows are restricted."); }
    static INPUT K(ushort vk, bool up = false) => new() { type=1, data=new UNION { key=new KEY { vk=vk, flags=up ? 2u : 0u } } };
    static void Mouse(uint flags, int x=0, int y=0, int wheel=0) => Send(new INPUT { data=new UNION { mouse=new MOUSE { dx=x, dy=y, mouseData=unchecked((uint)wheel), flags=flags } } });
    public void Apply(JsonElement e)
    {
        var kind = e.GetProperty("type").GetString();
        switch (kind)
        {
            case "move": Mouse(1, Number(e,"x",-1000,1000), Number(e,"y",-1000,1000)); break;
            case "scroll": Mouse(0x800, wheel:Number(e,"y",-1200,1200)); break;
            case "button":
                var b=e.GetProperty("button").GetString(); var down=e.GetProperty("down").GetBoolean();
                if (b is not ("left" or "right")) throw new InvalidDataException("Unknown button");
                var mask=b=="left"?1:2; if (((held&mask)!=0)==down) break;
                Mouse(b=="left" ? (down?2u:4u) : (down?8u:16u)); held=down?held|mask:held&~mask; break;
            case "text":
                string t=e.GetProperty("text").GetString()??""; if(t.Length>2048) throw new InvalidDataException("Text too long");
                foreach(char c in t) Send(new INPUT { type=1, data=new UNION { key=new KEY { scan=c, flags=4 } } }, new INPUT { type=1, data=new UNION { key=new KEY { scan=c, flags=6 } } }); break;
            case "key":
                var name=e.GetProperty("key").GetString()??"";
                if(!Keys.TryGetValue(name,out var vk)) throw new InvalidDataException("Unknown key"); Send(K(vk),K(vk,true)); break;
            case "shortcut":
                var key=e.GetProperty("key").GetString(); if(key is not ("a" or "c" or "v" or "z")) throw new InvalidDataException("Unknown shortcut");
                ushort letter=(ushort)char.ToUpperInvariant(key[0]); Send(K(17),K(letter),K(letter,true),K(17,true)); break;
            case "release": Release(); break;
            case "ping": break;
            default: throw new InvalidDataException("Unknown input type");
        }
    }
    static int Number(JsonElement e,string key,int min,int max) { int v=e.GetProperty(key).GetInt32(); if(v<min||v>max) throw new InvalidDataException("Input out of range"); return v; }
    public void Release() { int old=held; held=0; if((old&1)!=0) Mouse(4); if((old&2)!=0) Mouse(16); }
}
