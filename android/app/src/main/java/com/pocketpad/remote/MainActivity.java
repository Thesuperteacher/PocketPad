package com.pocketpad.remote;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.core.view.*;
import com.google.zxing.integration.android.*;
import org.json.JSONObject;
import java.util.*;

public class MainActivity extends Activity {
    static final int BG=0xff0d1117,SURFACE=0xff161b22,PAD=0xff182331,LINE=0xff263544,INK=0xffedf2f7,MUTED=0xff8796a8;
    final int mint=0xff57e5b6;
    TouchFeedback feedback;TrackpadView pad;boolean pendingKeyboard=false,autoKeyboard=true;
    LinearLayout root,keyboard,trackpadPage,trackpadCard,shortcutsPage;TextView status,brand;EditText text;Button drag;
    FrameLayout pages;LinearLayout keyboardPage;ScrollView shortcutsScroll;
    final ArrayList<LinearLayout> tabs=new ArrayList<>();
    Transport transport;boolean dragging=false,tapDragging=false,usb=false;String pairing="",lastStatus="Not connected";int generation=0,mode=0,imeRequest=0;
    boolean needIme=false,imeVisible=false;Dialog tutorial;

    protected void onCreate(Bundle state){
        super.onCreate(state);feedback=new TouchFeedback(this);autoKeyboard=prefs().getBoolean("auto_keyboard",true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        root=column();root.setBackgroundColor(BG);root.setPadding(dp(20),dp(12),dp(20),dp(10));setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());
            imeVisible=insets.isVisible(WindowInsetsCompat.Type.ime());
            if(imeVisible)needIme=false;
            root.setPadding(dp(20)+bars.left,dp(12)+bars.top,dp(20)+bars.right,dp(10)+Math.max(bars.bottom,ime.bottom));return insets;
        });
        buildHeader();
        pages=new FrameLayout(this);root.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
        buildTrackpad();buildKeyboard();buildShortcuts();buildNavigation();
        selectMode(0);setStatus("Not connected");
        root.post(()->{if(!prefs().getBoolean("gestures_seen",false))showTutorial(0);});
    }
    android.content.SharedPreferences prefs(){return getSharedPreferences("touch-feel",MODE_PRIVATE);}
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    TextView label(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(INK);t.setFontFeatureSettings("kern");return t;}
    GradientDrawable surface(int color,int radius,int border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
    android.graphics.drawable.Drawable ripple(int color,int radius){return new RippleDrawable(ColorStateList.valueOf(0x2257e5b6),surface(color,radius,0),surface(Color.WHITE,radius,0));}
    void gap(LinearLayout target,int size){target.addView(new View(this),new LinearLayout.LayoutParams(1,dp(size)));}
    Button action(String title,Runnable run){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(INK);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(8),0,dp(8),0);b.setBackground(ripple(SURFACE,14));b.setStateListAnimator(null);b.setOnClickListener(v->run.run());return b;}
    Button button(LinearLayout row,String title,Runnable run){Button b=action(title,run);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);p.setMargins(dp(3),0,dp(3),0);row.addView(b,p);return b;}
    void buildHeader(){
        LinearLayout top=row();brand=label("PocketPad",25);brand.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));brand.setLetterSpacing(-.025f);top.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton settings=new ImageButton(this);settings.setImageDrawable(new UiIcon("settings",MUTED,dp(23)));settings.setContentDescription("Settings");settings.setBackground(ripple(Color.TRANSPARENT,24));settings.setOnClickListener(v->settings());top.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(top);
        status=label("Not connected",12);status.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));status.setPadding(dp(12),0,dp(12),0);status.setGravity(Gravity.CENTER_VERTICAL);status.setMaxLines(1);status.setEllipsize(android.text.TextUtils.TruncateAt.END);status.setContentDescription("Connection status");status.setOnClickListener(v->connectionStatus());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(40));p.setMargins(0,dp(2),0,dp(12));root.addView(status,p);
    }
    void buildTrackpad(){
        trackpadPage=column();trackpadPage.setPadding(0,dp(8),0,dp(30));pages.addView(trackpadPage,new FrameLayout.LayoutParams(-1,-1));
        trackpadCard=column();trackpadCard.setBackground(surface(PAD,24,LINE));trackpadCard.setClipToOutline(true);trackpadCard.setPadding(dp(1),dp(1),dp(1),dp(1));trackpadPage.addView(trackpadCard,new LinearLayout.LayoutParams(-1,-1));
        pad=new TrackpadView(this,new TrackpadView.Listener(){
            public void move(int x,int y){send("move","x",Math.max(-1000,Math.min(1000,x)),"y",Math.max(-1000,Math.min(1000,y)));}
            public void scroll(int y){send("scroll","y",Math.max(-1200,Math.min(1200,y)));}
            public void click(boolean right){clickButton(right?"right":"left");}
            public void cancel(){release();}
            public boolean startTapDrag(){return beginTapDrag();}
            public void endTapDrag(){finishTapDrag();}
            public void gestureEnded(){maybeOpenKeyboard();root.postDelayed(()->maybeOpenKeyboard(),ViewConfiguration.getDoubleTapTimeout()+20);}
        },feedback);pad.speed=prefs().getFloat("speed",1.7f);trackpadCard.addView(pad,new LinearLayout.LayoutParams(-1,0,1));
        View line=new View(this);line.setBackgroundColor(LINE);trackpadCard.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        LinearLayout strip=row();
        Button left=button(strip,"Left",()->clickButton("left"));left.setContentDescription("Left click");
        divider(strip);
        drag=button(strip,"Drag ○",this::toggleDrag);drag.setContentDescription("Drag lock off");
        divider(strip);
        Button right=button(strip,"Right",()->clickButton("right"));right.setContentDescription("Right click");
        for(int i=0;i<strip.getChildCount();i++)if(strip.getChildAt(i) instanceof Button){View b=strip.getChildAt(i);b.setBackground(ripple(Color.TRANSPARENT,0));LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)b.getLayoutParams();p.height=dp(56);p.setMargins(0,0,0,0);}
        trackpadCard.addView(strip,new LinearLayout.LayoutParams(-1,dp(56)));
    }
    void divider(LinearLayout row){View d=new View(this);d.setBackgroundColor(LINE);row.addView(d,new LinearLayout.LayoutParams(dp(1),dp(20)));}
    void buildKeyboard(){
        keyboardPage=column();pages.addView(keyboardPage,new FrameLayout.LayoutParams(-1,-1));ScrollView typingScroll=new ScrollView(this);typingScroll.setFillViewport(true);typingScroll.setVerticalScrollBarEnabled(false);keyboardPage.addView(typingScroll,new LinearLayout.LayoutParams(-1,0,1));
        keyboard=column();keyboard.setPadding(0,dp(10),0,dp(12));typingScroll.addView(keyboard,new ScrollView.LayoutParams(-1,-2));
        TextView hint=label("Write here. Send to your computer.",13);hint.setTextColor(MUTED);keyboard.addView(hint);gap(keyboard,18);
        text=new EditText(this);text.setTextColor(INK);text.setHintTextColor(MUTED);text.setHint("Type something…");text.setTextSize(17);text.setGravity(Gravity.TOP|Gravity.START);text.setPadding(dp(16),dp(14),dp(16),dp(14));text.setBackground(surface(SURFACE,18,LINE));text.setMinLines(2);text.setMaxLines(4);text.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);text.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2048)});text.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);keyboard.addView(text,new LinearLayout.LayoutParams(-1,dp(96)));gap(keyboard,14);
        LinearLayout keys=row();for(String[] k:new String[][]{{"Esc","Escape"},{"Tab","Tab"},{"←","Left"},{"↑","Up"},{"↓","Down"},{"→","Right"}}){Button b=button(keys,k[0],()->send("key","key",k[1]));b.setContentDescription(k[1]);b.setTextSize(14);}keyboard.addView(keys);gap(keyboard,8);
        LinearLayout editing=row();button(editing,"⌫  Backspace",()->send("key","key","Backspace"));button(editing,"Enter ↵",()->send("key","key","Enter"));keyboard.addView(editing);gap(keyboard,16);
        Button send=action("Send text  →",this::sendText);send.setTextColor(BG);send.setBackground(ripple(mint,16));LinearLayout.LayoutParams sendParams=new LinearLayout.LayoutParams(-1,dp(52));sendParams.setMargins(0,dp(8),0,dp(8));keyboardPage.addView(send,sendParams);
    }
    void sendText(){if(!isConnected())return;String value=text.getText().toString();if(value.isEmpty())return;if(transport instanceof BluetoothTransport){if(value.length()>180){notice("Bluetooth text","Use up to 180 characters per send.");return;}for(char c:value.toCharArray())if(HidCodec.character(c)==null){notice("Use USB or Wi-Fi","Bluetooth supports basic characters. Your draft has been kept.");return;}}send("text","text",value);text.setText("");}
    void buildShortcuts(){
        shortcutsScroll=new ScrollView(this);shortcutsScroll.setVerticalScrollBarEnabled(false);pages.addView(shortcutsScroll,new FrameLayout.LayoutParams(-1,-1));shortcutsPage=column();shortcutsPage.setPadding(0,dp(10),0,dp(20));shortcutsScroll.addView(shortcutsPage);
        TextView hint=label("Editing, slides and sound.",13);hint.setTextColor(MUTED);shortcutsPage.addView(hint);gap(shortcutsPage,24);
        section("EDIT");shortcutRow("Select all","a","Copy","c",true);shortcutRow("Paste","v","Undo","z",true);gap(shortcutsPage,20);
        section("PRESENT");shortcutRow("Previous","PageUp","Next","PageDown",false);shortcutRow("Start slides","F5","Escape","Escape",false);gap(shortcutsPage,20);
        section("SOUND");shortcutRow("Volume −","VolumeDown","Volume +","VolumeUp",false);
        TextView note=label("Volume controls use USB or Wi-Fi.",12);note.setTextColor(MUTED);gap(shortcutsPage,12);shortcutsPage.addView(note);
    }
    void section(String value){TextView t=label(value,11);t.setTextColor(MUTED);t.setLetterSpacing(.12f);shortcutsPage.addView(t);gap(shortcutsPage,10);}
    void shortcutRow(String a,String ak,String b,String bk,boolean shortcut){LinearLayout r=row();button(r,a,()->send(shortcut?"shortcut":"key","key",ak));button(r,b,()->send(shortcut?"shortcut":"key","key",bk));shortcutsPage.addView(r);gap(shortcutsPage,8);}
    void buildNavigation(){
        LinearLayout nav=row();nav.setPadding(0,dp(8),0,dp(4));String[] names={"Trackpad","Keyboard","Shortcuts"};String[] icons={"trackpad","keyboard","shortcuts"};
        for(int i=0;i<3;i++){final int index=i;LinearLayout tab=column();tab.setGravity(Gravity.CENTER);tab.setContentDescription(names[i]+" mode");tab.setOnClickListener(v->{pendingKeyboard=false;selectMode(index);});ImageView icon=new ImageView(this);icon.setImageDrawable(new UiIcon(icons[i],MUTED,dp(22)));tab.addView(icon,new LinearLayout.LayoutParams(dp(22),dp(22)));TextView label=label(names[i],11);label.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(6);tab.addView(label,lp);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(64),1);p.setMargins(dp(2),0,dp(2),0);nav.addView(tab,p);tabs.add(tab);}
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(80)));
    }
    void selectMode(int next){
        if((dragging||tapDragging)&&next!=0)release();if(next!=0&&pad!=null)pad.cancelGesture();mode=next;brand.setText(next==0?"PocketPad":next==1?"Keyboard":"Shortcuts");status.setVisibility(next==0?View.VISIBLE:View.GONE);
        trackpadPage.setVisibility(next==0?View.VISIBLE:View.GONE);keyboardPage.setVisibility(next==1?View.VISIBLE:View.GONE);keyboard.setVisibility(next==1?View.VISIBLE:View.GONE);shortcutsScroll.setVisibility(next==2?View.VISIBLE:View.GONE);
        String[] icons={"trackpad","keyboard","shortcuts"};for(int i=0;i<tabs.size();i++){LinearLayout tab=tabs.get(i);int color=i==next?mint:MUTED;tab.setBackground(ripple(i==next?SURFACE:Color.TRANSPARENT,18));((ImageView)tab.getChildAt(0)).setImageDrawable(new UiIcon(icons[i],color,dp(22)));((TextView)tab.getChildAt(1)).setTextColor(color);tab.setSelected(i==next);}
        if(next==1)requestKeyboard();else{needIme=false;imeRequest++;text.clearFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(text.getWindowToken(),0);WindowCompat.getInsetsController(getWindow(),root).hide(WindowInsetsCompat.Type.ime());}
    }
    void toggleKeyboard(){pendingKeyboard=false;selectMode(mode==1?0:1);}
    void setKeyboardVisible(boolean show){selectMode(show?1:0);}
    void requestKeyboard(){
        needIme=true;int request=++imeRequest;text.requestFocus();
        // Wait for the dedicated page to lay out, and retry once if the IME was not ready.
        for(int delay:new int[]{0,180,450})text.postDelayed(()->{
            if(request!=imeRequest||mode!=1||!needIme||!hasWindowFocus())return;
            text.requestFocus();WindowCompat.getInsetsController(getWindow(),text).show(WindowInsetsCompat.Type.ime());
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(text,InputMethodManager.SHOW_IMPLICIT);
        },delay);
    }
    void remoteFocus(boolean editable){runOnUiThread(()->{pendingKeyboard=editable&&autoKeyboard;maybeOpenKeyboard();});}
    void maybeOpenKeyboard(){if(pendingKeyboard&&hasWindowFocus()&&!dragging&&!tapDragging&&pad!=null&&!pad.isTouchActive()&&!pad.isAwaitingSecondTap()){pendingKeyboard=false;selectMode(1);}}
    public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused&&(tapDragging||(pad!=null&&pad.isTouchActive())))release();if(focused&&keyboard!=null){maybeOpenKeyboard();if(mode==1&&needIme)requestKeyboard();}}
    public void onBackPressed(){if(mode!=0){pendingKeyboard=false;selectMode(0);}else super.onBackPressed();}
    boolean beginTapDrag(){if(dragging||!isConnected())return false;tapDragging=true;feedback.click(root,false);send("button","button","left","down",true);updateDrag();return true;}
    void finishTapDrag(){if(!tapDragging)return;tapDragging=false;if(transport!=null&&transport.connected())send("button","button","left","down",false);updateDrag();}
    void toggleDrag(){if(tapDragging)release();if(!isConnected())return;dragging=!dragging;feedback.drag(root,dragging);send("button","button","left","down",dragging);updateDrag();if(!dragging)maybeOpenKeyboard();}
    void updateDrag(){boolean held=dragging||tapDragging;if(drag!=null){drag.setText(held?"Drag ●":"Drag ○");drag.setTextColor(held?mint:INK);drag.setContentDescription(tapDragging?"Tap drag active":dragging?"Drag lock on":"Drag lock off");drag.setSelected(held);}if(trackpadCard!=null)trackpadCard.setBackground(surface(PAD,24,held?mint:LINE));}
    void clickButton(String button){if(!isConnected())return;if(dragging&&button.equals("left")){feedback.click(root,false);release();maybeOpenKeyboard();return;}feedback.click(root,button.equals("right"));send("button","button",button,"down",true);send("button","button",button,"down",false);}
    void release(){if(pad!=null)pad.cancelGesture();tapDragging=false;if(transport!=null&&transport.connected())try{transport.send(new JSONObject().put("type","release"));}catch(Exception ignored){}dragging=false;updateDrag();}
    void disconnect(){pendingKeyboard=false;generation++;release();if(transport!=null)transport.close();transport=null;}
    boolean isConnected(){if(transport==null||!transport.connected()){setStatus("Connect your PC first");return false;}return true;}
    void send(String type,Object... fields){if(!isConnected())return;try{JSONObject e=new JSONObject().put("type",type);for(int i=0;i<fields.length;i+=2)e.put((String)fields[i],fields[i+1]);transport.send(e);}catch(Exception ex){setStatus("Could not send input");}}
    void setStatus(String value){runOnUiThread(()->{
        lastStatus=value;boolean connected=value.startsWith("Connected");String display=connected?(value.contains("USB")?"USB Connected":value.contains("Bluetooth")?"Bluetooth Connected":"Wi-Fi Connected"):value.startsWith("Connecting")?"Connecting…":value.startsWith("Connection failed")?"Connection failed":"Connect your PC";
        status.setText("●  "+display);status.setTextColor(connected?mint:MUTED);status.setBackground(ripple(connected?0xff152a24:SURFACE,20));
    });}
    void connectionStatus(){if(transport!=null&&transport.connected())new AlertDialog.Builder(this).setTitle("Connected to your PC").setItems(new String[]{"Change connection","Disconnect"},(d,i)->{if(i==0)connectionMenu();else{disconnect();setStatus("Disconnected");}}).setNegativeButton("Close",null).show();else connectionMenu();}
    void notice(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK",null).show();}
    void settings(){
        LinearLayout content=column();content.setPadding(dp(22),dp(12),dp(22),dp(8));
        TextView speedLabel=label("Pointer speed",15);content.addView(speedLabel);
        SeekBar speed=new SeekBar(this);speed.setMax(35);speed.setProgress((int)(pad.speed*10)-5);speed.setProgressTintList(ColorStateList.valueOf(mint));speed.setThumbTintList(ColorStateList.valueOf(mint));speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){pad.speed=(p+5)/10f;}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){prefs().edit().putFloat("speed",pad.speed).apply();}});content.addView(speed,new LinearLayout.LayoutParams(-1,dp(48)));gap(content,12);
        Switch haptics=new Switch(this);haptics.setText("Click haptics");haptics.setTextColor(INK);haptics.setChecked(feedback.enabled());haptics.setPadding(0,dp(8),0,dp(8));haptics.setOnCheckedChangeListener((b,value)->{feedback.setEnabled(value);if(value)feedback.click(haptics,false);});content.addView(haptics,new LinearLayout.LayoutParams(-1,dp(52)));
        Switch automatic=new Switch(this);automatic.setText("Automatic keyboard");automatic.setTextColor(INK);automatic.setChecked(autoKeyboard);automatic.setOnCheckedChangeListener((b,value)->{autoKeyboard=value;if(!value)pendingKeyboard=false;prefs().edit().putBoolean("auto_keyboard",value).apply();});content.addView(automatic,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView info=label("Opens for PC text fields over USB or Wi-Fi.\nBluetooth uses the Keyboard tab.",12);info.setTextColor(MUTED);content.addView(info);gap(content,20);
        Button help=action("Gesture guide",()->{});content.addView(help,new LinearLayout.LayoutParams(-1,dp(48)));gap(content,12);
        TextView keep=label("Keep PocketPad open while controlling your PC.\nLeaving the app disconnects it.",12);keep.setTextColor(MUTED);content.addView(keep);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Settings").setView(content).setPositiveButton("Done",null).create();help.setOnClickListener(v->{dialog.dismiss();showTutorial(0);});dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mint);
    }
    void showTutorial(int step){
        if(isFinishing())return;if(tutorial!=null)tutorial.dismiss();
        String[] title={"Move naturally","Scroll with two fingers","Tap to right-click"};String[] detail={"Slide one finger to move the cursor.\nTap once to click. Tap again and hold to drag; lift to release.","Slide two fingers together to scroll\nup or down on your computer.","Tap the pad with two fingers.\nUse Drag to hold and move an item."};
        LinearLayout card=column();card.setPadding(dp(28),dp(28),dp(28),dp(24));card.setBackground(surface(SURFACE,24,LINE));
        TextView count=label("GESTURES   "+(step+1)+" / 3",11);count.setTextColor(MUTED);count.setLetterSpacing(.12f);card.addView(count);gap(card,26);
        ImageView icon=new ImageView(this);icon.setImageDrawable(new UiIcon(step==0?"trackpad":step==1?"scroll":"tap",INK,dp(44)));card.addView(icon,new LinearLayout.LayoutParams(dp(44),dp(44)));gap(card,22);
        TextView heading=label(title[step],23);heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));card.addView(heading);gap(card,12);TextView body=label(detail[step],14);body.setTextColor(MUTED);body.setLineSpacing(dp(4),1);card.addView(body);gap(card,28);
        Button next=action(step==2?"Got it":"Next",()->{if(step<2)showTutorial(step+1);else{prefs().edit().putBoolean("gestures_seen",true).apply();tutorial.dismiss();tutorial=null;}});next.setBackground(ripple(mint,14));next.setTextColor(BG);card.addView(next,new LinearLayout.LayoutParams(-1,dp(48)));
        tutorial=new Dialog(this);tutorial.requestWindowFeature(Window.FEATURE_NO_TITLE);tutorial.setContentView(card);tutorial.setCancelable(false);tutorial.getWindow().setBackgroundDrawableResource(android.R.color.transparent);tutorial.show();tutorial.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(40),dp(400)),-2);
    }
    void connectionMenu(){new AlertDialog.Builder(this).setTitle("Connect to your PC").setItems(new String[]{"Scan PC code · Wi-Fi / USB tethering","Scan PC code · USB debugging cable","Paste pairing link","Reconnect last code","Bluetooth mouse & keyboard"},(d,i)->{if(i==4){startBluetooth();return;}if(i==3){if(pairing.isEmpty())setStatus("Scan the PC code first");else connectNetwork();return;}if(i==2){EditText entry=new EditText(this);entry.setHint("pocketpad://…");entry.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);new AlertDialog.Builder(this).setTitle("Paste the PC pairing link").setView(entry).setPositiveButton("Connect",(dialog,w)->{pairing=entry.getText().toString();usb=false;connectNetwork();}).setNegativeButton("Cancel",null).show();return;}usb=i==1;new IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).setPrompt("Scan the code in PocketPad on your PC").setBeepEnabled(false).setOrientationLocked(false).initiateScan();}).show();}
    protected void onActivityResult(int request,int result,Intent data){IntentResult scan=IntentIntegrator.parseActivityResult(request,result,data);if(scan!=null){if(scan.getContents()!=null){pairing=scan.getContents();connectNetwork();}return;}super.onActivityResult(request,result,data);}
    void connectNetwork(){disconnect();int current=generation;setStatus("Connecting securely…");transport=new NetworkTransport(pairing,usb,new NetworkTransport.Listener(){public void status(String message){runOnUiThread(()->{if(generation==current)setStatus(message);});}public void focus(boolean editable){runOnUiThread(()->{if(generation==current)remoteFocus(editable);});}});}
    void startBluetooth(){if(Build.VERSION.SDK_INT<28){setStatus("Bluetooth mouse mode needs Android 9+. Use Wi-Fi or USB on this phone.");return;}if(Build.VERSION.SDK_INT>=31&&(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)!=PackageManager.PERMISSION_GRANTED)){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE},7);return;}disconnect();int current=generation;setStatus("Preparing Bluetooth…");transport=new BluetoothTransport(this,new BluetoothTransport.Listener(){public void status(String value){if(generation==current)setStatus(value);}public void registered(){if(generation==current)chooseBluetooth();}});}
    public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==7){if(grants.length==2&&grants[0]==PackageManager.PERMISSION_GRANTED&&grants[1]==PackageManager.PERMISSION_GRANTED)startBluetooth();else setStatus("Nearby devices permission is needed for Bluetooth.");}}
    void chooseBluetooth(){try{if(!(transport instanceof BluetoothTransport))return;BluetoothTransport bt=(BluetoothTransport)transport;List<BluetoothDevice> devices=new ArrayList<>(bt.bonded());String[] names=new String[devices.size()+2];for(int i=0;i<devices.size();i++){String name=devices.get(i).getName();names[i]=(name==null?"Paired device":name)+" · "+devices.get(i).getAddress();}names[devices.size()]="Pair a new PC (make phone visible)";names[devices.size()+1]="Refresh paired devices";new AlertDialog.Builder(this).setTitle("Choose your Windows PC").setItems(names,(d,i)->{if(i<devices.size())bt.connect(devices.get(i));else if(i==devices.size()){Intent visible=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);visible.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,120);startActivity(visible);setStatus("Windows: Bluetooth > Add device. Then tap Connect > Bluetooth again.");}else chooseBluetooth();}).setNegativeButton("Close",null).show();}catch(SecurityException ex){disconnect();setStatus("Nearby devices permission was removed. Connect again.");}}
    protected void onStop(){super.onStop();needIme=false;imeRequest++;disconnect();setStatus("Paused · reconnect when you return");}
    protected void onDestroy(){disconnect();if(tutorial!=null)tutorial.dismiss();super.onDestroy();}
}
