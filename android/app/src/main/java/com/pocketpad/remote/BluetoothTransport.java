package com.pocketpad.remote;

import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.util.*;

@android.annotation.TargetApi(28)
final class BluetoothTransport implements Transport {
    interface Listener {void status(String text);void registered();}
    private final BluetoothAdapter adapter;private final Listener listener;
    private BluetoothHidDevice hid;private BluetoothDevice host;private boolean registered,closed;
    private int buttons,scroll;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayDeque<Runnable> reports=new ArrayDeque<>();private boolean pumping;
    BluetoothTransport(Context context,Listener listener){
        this.listener=listener;adapter=((BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if(adapter==null||!adapter.isEnabled()){listener.status("Enable Bluetooth in phone settings, then try again.");return;}
        boolean started=adapter.getProfileProxy(context,new BluetoothProfile.ServiceListener(){
            public void onServiceConnected(int profile,BluetoothProfile proxy){
                if(closed){adapter.closeProfileProxy(profile,proxy);return;}hid=(BluetoothHidDevice)proxy;
                try { boolean requested=hid.registerApp(new BluetoothHidDeviceAppSdpSettings("PocketPad","Phone mouse and keyboard","PocketPad",BluetoothHidDevice.SUBCLASS1_COMBO,HidCodec.DESCRIPTOR),null,null,context.getMainExecutor(),callback);
                if(!requested)listener.status("This phone could not register Bluetooth mouse/keyboard mode."); } catch(SecurityException ex){listener.status("Nearby devices permission was removed. Connect again.");close();}
            }
            public void onServiceDisconnected(int profile){host=null;hid=null;registered=false;listener.status("Bluetooth service disconnected");}
        },BluetoothProfile.HID_DEVICE);
        if(!started)listener.status("Bluetooth mouse/keyboard mode is unavailable on this phone.");
    }
    private final BluetoothHidDevice.Callback callback=new BluetoothHidDevice.Callback(){
        public void onAppStatusChanged(BluetoothDevice plugged,boolean success){registered=success;if(closed)return;if(success){listener.status("Bluetooth ready · choose your paired PC");listener.registered();}else{host=null;listener.status("Bluetooth registration ended · reconnect");}}
        public void onConnectionStateChanged(BluetoothDevice device,int state){if(closed)return;if(state==BluetoothProfile.STATE_CONNECTED){host=device;listener.status("Connected · Bluetooth · US keyboard layout");}else if(state==BluetoothProfile.STATE_DISCONNECTED){host=null;buttons=0;reports.clear();listener.status("Bluetooth disconnected · choose PC again");}else listener.status("Bluetooth connecting…");}
        public void onGetReport(BluetoothDevice d,byte type,byte id,int buffer){try{if(hid!=null)hid.replyReport(d,type,id,id==1?new byte[]{(byte)buttons,0,0,0}:new byte[8]);}catch(SecurityException ex){close();}}
        public void onSetReport(BluetoothDevice d,byte type,byte id,byte[] data){/* Keyboard LED output is intentionally ignored. */}
    };
    Set<BluetoothDevice> bonded(){try{return adapter==null?Collections.emptySet():adapter.getBondedDevices();}catch(SecurityException ex){listener.status("Nearby devices permission was removed. Connect again.");return Collections.emptySet();}}
    void connect(BluetoothDevice device){try{if(hid==null||!registered){listener.status("Bluetooth is still preparing. Try again shortly.");return;}if(!hid.connect(device))listener.status("Could not connect. Pair this phone in Windows Bluetooth settings first.");}catch(SecurityException ex){listener.status("Nearby devices permission was removed. Connect again.");close();}}
    public boolean connected(){return !closed&&hid!=null&&host!=null;}
    private void report(int id,byte[] data){
        if(!connected())return;
        if(reports.size()>=512){listener.status("Bluetooth queue full · reconnect");close();return;}
        BluetoothDevice target=host;
        reports.add(()->{try{if(hid!=null&&target.equals(host)&&!hid.sendReport(target,id,data)){listener.status("Bluetooth could not deliver input · reconnect");close();}}catch(SecurityException ex){listener.status("Nearby devices permission was removed. Connect again.");close();}});
        if(!pumping){pumping=true;pump();}
    }
    private void pump(){Runnable r=reports.poll();if(r==null||closed){pumping=false;return;}r.run();handler.postDelayed(this::pump,8);}
    private void mouse(int x,int y,int wheel){do{int dx=Math.max(-127,Math.min(127,x)),dy=Math.max(-127,Math.min(127,y)),w=Math.max(-127,Math.min(127,wheel));report(1,new byte[]{(byte)buttons,(byte)dx,(byte)dy,(byte)w});x-=dx;y-=dy;wheel-=w;}while(x!=0||y!=0||wheel!=0);}
    private void key(int modifier,int code){report(2,HidCodec.keyboard(modifier,code));report(2,new byte[8]);}
    public void send(JSONObject e){try{
        switch(e.getString("type")){
            case"move":mouse(e.getInt("x"),e.getInt("y"),0);break;
            case"scroll":scroll+=e.getInt("y");int wheel=scroll/120;scroll-=wheel*120;if(wheel!=0)mouse(0,0,wheel);break;
            case"button":int bit=e.getString("button").equals("left")?1:2;buttons=e.getBoolean("down")?buttons|bit:buttons&~bit;mouse(0,0,0);break;
            case"key":int code=HidCodec.key(e.getString("key"));if(code==0){listener.status("This key needs Wi-Fi or USB mode.");break;}key(0,code);break;
            case"shortcut":int[] chord=HidCodec.character(e.getString("key").charAt(0));if(chord!=null)key(1,chord[1]);break;
            case"text":String text=e.getString("text");for(char c:text.toCharArray())if(HidCodec.character(c)==null){listener.status("Accents and emoji need Wi-Fi or USB. Text was not sent.");return;}if(text.length()>180){listener.status("Bluetooth: send up to 180 characters at a time.");return;}for(char c:text.toCharArray()){int[] k=HidCodec.character(c);key(k[0],k[1]);}break;
            case"release":reports.clear();buttons=0;scroll=0;mouse(0,0,0);report(2,new byte[8]);break;
        }
    }catch(Exception ex){listener.status("Bluetooth input failed: "+ex.getMessage());}}
    public void close(){if(closed)return;closed=true;reports.clear();handler.removeCallbacksAndMessages(null);if(hid!=null){try{if(host!=null){hid.sendReport(host,1,new byte[4]);hid.sendReport(host,2,new byte[8]);hid.disconnect(host);}hid.unregisterApp();}catch(SecurityException ignored){}adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE,hid);}host=null;buttons=0;}
}
