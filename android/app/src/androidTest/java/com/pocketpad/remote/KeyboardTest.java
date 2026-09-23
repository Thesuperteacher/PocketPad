package com.pocketpad.remote;
import org.junit.Test;
import static org.junit.Assert.*;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import android.content.Intent;
import android.view.View;
import android.view.MotionEvent;

public class KeyboardTest {
    @Test public void opensForEditableFocusAndWaitsForGesture() throws Exception {
        var inst=getInstrumentation();
        var prefs=inst.getTargetContext().getSharedPreferences("touch-feel",0);boolean seen=prefs.getBoolean("gestures_seen",false);prefs.edit().putBoolean("gestures_seen",true).commit();
        var intent=new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity=(MainActivity)inst.startActivitySync(intent);
        awaitFocus(activity);
        try {
            inst.runOnMainSync(()->{
                activity.autoKeyboard=true;
                activity.setKeyboardVisible(false);
                activity.remoteFocus(false);
                assertEquals(View.GONE,activity.keyboard.getVisibility());
                long time=android.os.SystemClock.uptimeMillis();
                MotionEvent down=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,50,50,0);
                activity.pad.onTouchEvent(down);down.recycle();
                activity.remoteFocus(true);
                assertEquals(View.GONE,activity.keyboard.getVisibility());
                MotionEvent cancel=MotionEvent.obtain(time,time+100,MotionEvent.ACTION_CANCEL,50,50,0);
                activity.pad.onTouchEvent(cancel);cancel.recycle();
                assertEquals(View.VISIBLE,activity.keyboard.getVisibility());
                activity.setKeyboardVisible(false);activity.autoKeyboard=false;
                activity.remoteFocus(true);
                assertEquals(View.GONE,activity.keyboard.getVisibility());
            });
        }finally{inst.runOnMainSync(activity::finish);prefs.edit().putBoolean("gestures_seen",seen).commit();}
    }

    @Test public void switchingModesPreservesDraftAndReleasesDrag() throws Exception {
        var inst=getInstrumentation();var prefs=inst.getTargetContext().getSharedPreferences("touch-feel",0);boolean seen=prefs.getBoolean("gestures_seen",false);prefs.edit().putBoolean("gestures_seen",true).commit();
        MainActivity activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        awaitFocus(activity);
        try{inst.runOnMainSync(()->{
            java.util.List<String> sent=new java.util.ArrayList<>();
            activity.transport=new Transport(){public boolean connected(){return true;}public void send(org.json.JSONObject e){sent.add(e.optString("type"));}public void close(){}};
            activity.text.setText("Draft stays here");activity.dragging=true;activity.selectMode(1);
            assertFalse(activity.dragging);assertTrue(sent.contains("release"));
            activity.selectMode(2);assertEquals("Draft stays here",activity.text.getText().toString());assertEquals(View.GONE,activity.keyboardPage.getVisibility());
            activity.selectMode(0);assertEquals(View.VISIBLE,activity.trackpadPage.getVisibility());assertEquals(View.GONE,activity.shortcutsScroll.getVisibility());
        });}finally{inst.runOnMainSync(activity::finish);prefs.edit().putBoolean("gestures_seen",seen).commit();}
    }
    @Test public void nativeKeyboardReopensAfterDismissal() throws Exception {
        var inst=getInstrumentation();var prefs=inst.getTargetContext().getSharedPreferences("touch-feel",0);boolean seen=prefs.getBoolean("gestures_seen",false);prefs.edit().putBoolean("gestures_seen",true).commit();
        MainActivity activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        awaitFocus(activity);
        try{
            inst.runOnMainSync(()->{activity.autoKeyboard=true;activity.remoteFocus(true);});
            assertIme(activity,true);
            inst.runOnMainSync(()->{androidx.core.view.WindowCompat.getInsetsController(activity.getWindow(),activity.text).hide(androidx.core.view.WindowInsetsCompat.Type.ime());});
            assertIme(activity,false);
            inst.runOnMainSync(()->activity.remoteFocus(true));
            assertIme(activity,true);
        }finally{inst.runOnMainSync(activity::finish);prefs.edit().putBoolean("gestures_seen",seen).commit();}
    }
    void assertIme(MainActivity activity,boolean expected) throws Exception {
        java.util.concurrent.atomic.AtomicBoolean value=new java.util.concurrent.atomic.AtomicBoolean(!expected);
        for(int i=0;i<35;i++){
            getInstrumentation().runOnMainSync(()->value.set(activity.imeVisible));
            if(value.get()==expected)return;
            Thread.sleep(100);
        }
        assertEquals("Native keyboard visibility",expected,value.get());
    }

    @Test public void tapDragDefersKeyboardAndReleasesOnModeChange() throws Exception {
        var inst=getInstrumentation();var prefs=inst.getTargetContext().getSharedPreferences("touch-feel",0);boolean seen=prefs.getBoolean("gestures_seen",false);prefs.edit().putBoolean("gestures_seen",true).commit();
        MainActivity activity=(MainActivity)inst.startActivitySync(new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        awaitFocus(activity);
        try{inst.runOnMainSync(()->{
            java.util.List<String> sent=new java.util.ArrayList<>();
            activity.transport=new Transport(){public boolean connected(){return true;}public void send(org.json.JSONObject e){sent.add(e.optString("type")+":"+e.optBoolean("down"));}public void close(){}};
            activity.autoKeyboard=true;activity.selectMode(0);
            long now=android.os.SystemClock.uptimeMillis();
            java.util.function.BiConsumer<Integer,Long> touch=(action,time)->{MotionEvent e=MotionEvent.obtain(now-150,time,action,50,50,0);activity.pad.onTouchEvent(e);e.recycle();};
            touch.accept(MotionEvent.ACTION_DOWN,now-100);touch.accept(MotionEvent.ACTION_UP,now-50);
            activity.remoteFocus(true);assertEquals(0,activity.mode);
            touch.accept(MotionEvent.ACTION_DOWN,now);assertTrue(activity.tapDragging);assertEquals(0,activity.mode);
            touch.accept(MotionEvent.ACTION_UP,now+50);assertFalse(activity.tapDragging);assertEquals(1,activity.mode);
            assertEquals(java.util.Arrays.asList("button:true","button:false","button:true","button:false"),sent);
            activity.selectMode(0);assertTrue(activity.beginTapDrag());activity.selectMode(2);assertFalse(activity.tapDragging);assertTrue(sent.contains("release:false"));
            activity.dragging=true;assertFalse(activity.beginTapDrag());assertTrue(activity.dragging);activity.release();
        });}finally{inst.runOnMainSync(activity::finish);prefs.edit().putBoolean("gestures_seen",seen).commit();}
    }

    void awaitFocus(MainActivity activity) throws Exception {
        java.util.concurrent.atomic.AtomicBoolean focused=new java.util.concurrent.atomic.AtomicBoolean();
        for(int i=0;i<50;i++){
            getInstrumentation().runOnMainSync(()->focused.set(activity.hasWindowFocus()));
            if(focused.get())return;Thread.sleep(50);
        }
        fail("Test activity did not gain window focus");
    }
}
