package com.pocketpad.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.os.Build;

/** Native touch feedback: short tactile cues, with no vibration permission or setting override. */
final class TouchFeedback {
    private final SharedPreferences settings;
    private boolean enabled;
    TouchFeedback(Context context){settings=context.getSharedPreferences("touch-feel",Context.MODE_PRIVATE);enabled=settings.getBoolean("enabled",true);}
    boolean enabled(){return enabled;}
    void setEnabled(boolean value){enabled=value;settings.edit().putBoolean("enabled",value).apply();}
    private void emit(View view,int effect){if(enabled)view.performHapticFeedback(effect);}
    void click(View view,boolean right){emit(view,right?HapticFeedbackConstants.LONG_PRESS:HapticFeedbackConstants.KEYBOARD_TAP);}
    void drag(View view,boolean held){emit(view,held?HapticFeedbackConstants.LONG_PRESS:(Build.VERSION.SDK_INT>=27?HapticFeedbackConstants.VIRTUAL_KEY_RELEASE:HapticFeedbackConstants.VIRTUAL_KEY));}
}
