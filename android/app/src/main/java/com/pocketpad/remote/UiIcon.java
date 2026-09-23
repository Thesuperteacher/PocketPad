package com.pocketpad.remote;
import android.graphics.*;
import android.graphics.drawable.Drawable;
final class UiIcon extends Drawable {
    final String name;final Paint p=new Paint(3);final int size;
    UiIcon(String name,int color,int size){this.name=name;this.size=size;p.setColor(color);p.setStrokeWidth(1.65f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);}
    public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
    public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
        switch(name){
            case "settings":c.drawCircle(12,12,6,p);c.drawCircle(12,12,2.2f,p);for(int i=0;i<8;i++){c.save();c.rotate(i*45,12,12);c.drawLine(12,3,12,5,p);c.restore();}break;
            case "keyboard":c.drawRoundRect(2,5,22,19,3,3,p);for(int y=9;y<=12;y+=3)for(int x=6;x<=18;x+=4)c.drawPoint(x,y,p);c.drawLine(8,16,16,16,p);break;
            case "shortcuts":Path bolt=new Path();bolt.moveTo(13,2);bolt.lineTo(5,13);bolt.lineTo(11,13);bolt.lineTo(10,22);bolt.lineTo(19,10);bolt.lineTo(13,10);bolt.close();c.drawPath(bolt,p);break;
            case "scroll":c.drawLine(8,4,8,20,p);c.drawLine(16,4,16,20,p);c.drawLine(5,7,8,4,p);c.drawLine(11,7,8,4,p);c.drawLine(13,17,16,20,p);c.drawLine(19,17,16,20,p);break;
            case "tap":c.drawCircle(8,9,3,p);c.drawCircle(17,9,3,p);c.drawLine(8,15,8,20,p);c.drawLine(17,15,17,20,p);break;
            default:c.drawRoundRect(3,3,21,21,4,4,p);c.drawLine(3,16,21,16,p);c.drawLine(12,16,12,21,p);break;
        }c.restore();
    }
    public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
