package com.pocketpad.remote;

import org.junit.Test;
import static org.junit.Assert.*;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import java.util.*;

@SuppressWarnings("deprecation")
public class GestureTest {
    void runTestOnUiThread(Runnable action) throws Throwable {java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();getInstrumentation().runOnMainSync(()->{try{action.run();}catch(Throwable ex){error.set(ex);}});if(error.get()!=null)throw error.get();}
    static class Recorded implements TrackpadView.Listener {
        int moves,scroll,cancel;List<Boolean> clicks=new ArrayList<>();List<String> events=new ArrayList<>();
        public void move(int x,int y){moves+=Math.abs(x)+Math.abs(y);events.add("move");}public void scroll(int y){scroll+=y;}public void click(boolean right){clicks.add(right);events.add(right?"right":"click");}public void cancel(){cancel++;}
        public boolean startTapDrag(){events.add("down");return true;}public void endTapDrag(){events.add("up");}
    }
    void touch(TrackpadView pad,int action,long time,float... xy){
        int count=xy.length/2;MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];
        for(int i=0;i<count;i++){props[i]=new MotionEvent.PointerProperties();props[i].id=i;props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=xy[i*2];coords[i].y=xy[i*2+1];coords[i].pressure=1;coords[i].size=1;}
        MotionEvent event=MotionEvent.obtain(1000,time,action,count,props,coords,0,0,1,1,0,0,0,0);pad.onTouchEvent(event);event.recycle();
    }
    TrackpadView pad(Recorded out){var context=getInstrumentation().getTargetContext();var parent=new FrameLayout(context);var pad=new TrackpadView(context,out);parent.addView(pad);return pad;}
    @Test public void testTapClicksOnce()throws Throwable{runTestOnUiThread(()->{Recorded r=new Recorded();TrackpadView p=pad(r);touch(p,0,1000,50,50);touch(p,1,1100,50,50);assertEquals(Arrays.asList(false),r.clicks);assertEquals(0,r.moves);});}
    @Test public void testMovementDoesNotClick()throws Throwable{runTestOnUiThread(()->{Recorded r=new Recorded();TrackpadView p=pad(r);touch(p,0,1000,50,50);touch(p,2,1100,180,160);touch(p,1,1200,180,160);assertTrue(r.moves>0);assertTrue(r.clicks.isEmpty());});}
    @Test public void testTwoFingerTapRightClicks()throws Throwable{runTestOnUiThread(()->{Recorded r=new Recorded();TrackpadView p=pad(r);touch(p,0,1000,50,50);touch(p,5|(1<<8),1020,50,50,80,50);touch(p,6|(1<<8),1080,50,50,80,50);touch(p,1,1100,50,50);assertEquals(Arrays.asList(true),r.clicks);});}
    @Test public void testTwoFingerScrollDoesNotMoveOrClick()throws Throwable{runTestOnUiThread(()->{Recorded r=new Recorded();TrackpadView p=pad(r);touch(p,0,1000,50,50);touch(p,5|(1<<8),1020,50,50,80,50);touch(p,2,1100,50,150,80,150);touch(p,6|(1<<8),1120,50,150,80,150);touch(p,1,1140,50,150);assertTrue(r.scroll<0);assertEquals(0,r.moves);assertTrue(r.clicks.isEmpty());});}
    @Test public void testCancelReleases()throws Throwable{runTestOnUiThread(()->{Recorded r=new Recorded();TrackpadView p=pad(r);touch(p,0,1000,50,50);touch(p,3,1020,50,50);assertEquals(1,r.cancel);assertTrue(r.clicks.isEmpty());});}

    void firstTap(TrackpadView p){touch(p,0,1000,50,50);touch(p,1,1050,50,50);}
    @Test public void tapHoldDragReleasesOnLiftWithoutExtraClick()throws Throwable{runTestOnUiThread(()->{
        Recorded r=new Recorded();TrackpadView p=pad(r);firstTap(p);touch(p,0,1120,52,51);touch(p,2,1250,180,160);
        assertEquals(Arrays.asList("click","down","move"),r.events);
        touch(p,1,1800,180,160);assertEquals(Arrays.asList("click","down","move","up"),r.events);assertEquals(1,r.clicks.size());
    });}
    @Test public void twoQuickTapsProduceFirstClickAndSecondDownUp()throws Throwable{runTestOnUiThread(()->{
        Recorded r=new Recorded();TrackpadView p=pad(r);firstTap(p);touch(p,0,1120,50,50);touch(p,1,1160,50,50);
        assertEquals(Arrays.asList("click","down","up"),r.events);
    });}
    @Test public void lateSecondTouchOnlyMoves()throws Throwable{runTestOnUiThread(()->{
        Recorded r=new Recorded();TrackpadView p=pad(r);firstTap(p);long time=1051+android.view.ViewConfiguration.getDoubleTapTimeout();touch(p,0,time,50,50);touch(p,2,time+50,180,160);touch(p,1,time+100,180,160);assertEquals(Arrays.asList("click","move"),r.events);
    });}
    @Test public void distantSecondTouchDoesNotDrag()throws Throwable{runTestOnUiThread(()->{
        Recorded r=new Recorded();TrackpadView p=pad(r);firstTap(p);touch(p,0,1120,5000,5000);touch(p,2,1180,5100,5100);touch(p,1,1200,5100,5100);assertEquals(Arrays.asList("click","move"),r.events);
    });}
    @Test public void cancelledTapDragReleasesExactlyOnce()throws Throwable{runTestOnUiThread(()->{
        Recorded r=new Recorded();TrackpadView p=pad(r);firstTap(p);touch(p,0,1120,50,50);touch(p,3,1180,50,50);touch(p,1,1220,50,50);assertEquals(Arrays.asList("click","down","up"),r.events);assertEquals(1,r.cancel);
    });}
    @Test public void extraFingerCancelsHeldButton()throws Throwable{runTestOnUiThread(()->{
        Recorded r=new Recorded();TrackpadView p=pad(r);firstTap(p);touch(p,0,1120,50,50);touch(p,5|(1<<8),1140,50,50,80,50);touch(p,2,1180,50,150,80,150);touch(p,6|(1<<8),1190,50,150,80,150);touch(p,1,1200,50,150);assertEquals(Arrays.asList("click","down","up"),r.events);assertEquals(0,r.scroll);
    });}
    @Test public void detachingPadReleasesTapDrag()throws Throwable{runTestOnUiThread(()->{
        Recorded r=new Recorded();TrackpadView p=pad(r);firstTap(p);touch(p,0,1120,50,50);p.onDetachedFromWindow();assertEquals(Arrays.asList("click","down","up"),r.events);assertFalse(p.isTouchActive());
    });}
    @Test public void rightTapDoesNotArmSingleFingerDrag()throws Throwable{runTestOnUiThread(()->{
        Recorded r=new Recorded();TrackpadView p=pad(r);touch(p,0,1000,50,50);touch(p,5|(1<<8),1020,50,50,80,50);touch(p,6|(1<<8),1080,50,50,80,50);touch(p,1,1100,50,50);touch(p,0,1150,50,50);touch(p,2,1200,180,160);touch(p,1,1300,180,160);assertEquals(Arrays.asList("right","move"),r.events);
    });}
}
