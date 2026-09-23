package com.pocketpad.remote;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;

final class TrackpadView extends View {
    interface Listener {void move(int x,int y);void scroll(int y);void click(boolean right);void cancel();default void gestureEnded(){}default boolean startTapDrag(){return false;}default void endTapDrag(){}}
    final Listener listener;final Paint paint=new Paint(3);final Path outline=new Path();
    float lastX,lastY,travel,rx,ry,rs;long began;boolean multi;int maxPointers;
    final float density;float speed=1.7f;
    private final int doubleTapSlop;
    private long lastTapUp=-1;private float tapX,tapY;
    private boolean tapDragging,cancelled;
    private boolean touching;private float targetX,targetY,warpX,warpY,strength;private long frameTime;
    TrackpadView(Context c,Listener listener){this(c,listener,new TouchFeedback(c));}
    TrackpadView(Context c,Listener listener,TouchFeedback feedback){super(c);this.listener=listener;density=getResources().getDisplayMetrics().density;doubleTapSlop=ViewConfiguration.get(c).getScaledDoubleTapSlop();setContentDescription("Trackpad. One finger moves; tap clicks; tap again and hold to drag until lifted; two fingers scroll or tap to right-click.");setFocusable(true);}
    boolean isTouchActive(){return touching;}
    boolean isAwaitingSecondTap(){long gap=SystemClock.uptimeMillis()-lastTapUp;return lastTapUp>=0&&gap>=0&&gap<=ViewConfiguration.getDoubleTapTimeout();}
    private void finishTapDrag(){if(tapDragging){tapDragging=false;listener.endTapDrag();}}
    void cancelGesture(){finishTapDrag();lastTapUp=-1;cancelled=true;touching=false;strength=0;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);invalidate();}

    protected void onSizeChanged(int w,int h,int oldw,int oldh){outline.reset();outline.addRoundRect(0,0,w,h,24*density,24*density,Path.Direction.CW);}
    protected void onDraw(Canvas c){
        super.onDraw(c);long now=SystemClock.uptimeMillis();float dt=frameTime==0?16:Math.min(50,now-frameTime);frameTime=now;
        float follow=1-(float)Math.exp(-dt/45f),fade=1-(float)Math.exp(-dt/95f);
        warpX+=(targetX-warpX)*follow;warpY+=(targetY-warpY)*follow;strength+=((touching?1f:0f)-strength)*fade;
        if(!touching&&strength<0.005f)strength=0;
        c.save();c.clipPath(outline);paint.setColor(Color.rgb(24,35,49));c.drawPaint(paint);
        float radius=105*density;
        if(strength>0){paint.setColor(Color.argb((int)(10*strength),87,229,182));c.drawCircle(warpX,warpY,radius*.85f,paint);paint.setColor(Color.argb((int)(10*strength),87,229,182));c.drawCircle(warpX,warpY,radius*.48f,paint);}
        for(float x=24*density;x<getWidth();x+=24*density)for(float y=24*density;y<getHeight();y+=24*density){
            float dx=x-warpX,dy=y-warpY,d=(float)Math.sqrt(dx*dx+dy*dy),falloff=(float)Math.exp(-(d*d)/(radius*radius)) * strength;
            float push=8*density*falloff,denom=Math.max(d,1f);
            paint.setColor(Color.rgb((int)(31+46*falloff),(int)(44+123*falloff),(int)(60+85*falloff)));
            c.drawCircle(x+dx/denom*push,y+dy/denom*push,density*(1+.35f*falloff),paint);
        }
        c.restore();
        if(touching||strength>0)postInvalidateOnAnimation();else frameTime=0;
    }
    float x(MotionEvent e){float s=0;for(int i=0;i<e.getPointerCount();i++)s+=e.getX(i);return s/e.getPointerCount();}
    float y(MotionEvent e){float s=0;for(int i=0;i<e.getPointerCount();i++)s+=e.getY(i);return s/e.getPointerCount();}
    public boolean onTouchEvent(MotionEvent e){
        float x=x(e),y=y(e);targetX=x;targetY=y;
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                finishTapDrag();cancelled=false;touching=true;warpX=x;warpY=y;
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                lastX=x;lastY=y;travel=0;began=e.getEventTime();multi=false;maxPointers=1;
                long gap=began-lastTapUp;float tx=x-tapX,ty=y-tapY;
                boolean second=lastTapUp>=0&&gap>=0&&gap<=ViewConfiguration.getDoubleTapTimeout()&&tx*tx+ty*ty<=doubleTapSlop*doubleTapSlop;
                lastTapUp=-1;
                if(second)tapDragging=listener.startTapDrag();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if(tapDragging){finishTapDrag();cancelled=true;}
                lastTapUp=-1;multi=true;maxPointers=Math.max(maxPointers,e.getPointerCount());lastX=x;lastY=y;break;
            case MotionEvent.ACTION_MOVE:
                if(cancelled)break;
                float dx=(x-lastX)/density,dy=(y-lastY)/density;travel+=Math.abs(dx)+Math.abs(dy);lastX=x;lastY=y;
                if(e.getPointerCount()==2){rs-=dy*6;int scroll=(int)rs;rs-=scroll;if(scroll!=0)listener.scroll(scroll);}
                else if(!multi){rx+=dx*speed;ry+=dy*speed;int mx=(int)rx,my=(int)ry;rx-=mx;ry-=my;if(mx!=0||my!=0)listener.move(mx,my);}
                break;
            case MotionEvent.ACTION_POINTER_UP:break;
            case MotionEvent.ACTION_UP:
                touching=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                if(tapDragging){finishTapDrag();}
                else if(!cancelled&&travel<10&&e.getEventTime()-began<350&&maxPointers<=2){
                    if(!multi){lastTapUp=e.getEventTime();tapX=x;tapY=y;}
                    listener.click(multi);
                }
                performClick();listener.gestureEnded();break;
            case MotionEvent.ACTION_CANCEL:
                cancelGesture();listener.cancel();listener.gestureEnded();break;
        }
        postInvalidateOnAnimation();return true;
    }
    protected void onDetachedFromWindow(){cancelGesture();listener.cancel();super.onDetachedFromWindow();}
    public boolean performClick(){super.performClick();return true;}
}
