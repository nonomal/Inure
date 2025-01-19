package app.simple.inure.decorations.padding;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import app.simple.inure.decorations.theme.ThemeConstraintLayout;
import app.simple.inure.preferences.DevelopmentPreferences;
import app.simple.inure.util.StatusBarHeight;

public class PaddingAwareConstraintLayout extends ThemeConstraintLayout implements SharedPreferences.OnSharedPreferenceChangeListener {
    public PaddingAwareConstraintLayout(Context context) {
        super(context);
        init();
    }
    
    public PaddingAwareConstraintLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        if (isInEditMode()) {
            return;
        }
        updatePadding();
        app.simple.inure.preferences.SharedPreferences.INSTANCE.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    
    private void updatePadding() {
        if (DevelopmentPreferences.INSTANCE.get(DevelopmentPreferences.DISABLE_TRANSPARENT_STATUS)) {
            if (getPaddingTop() >= StatusBarHeight.getStatusBarHeight(getResources())) {
                setPadding(getPaddingLeft(),
                        Math.abs(StatusBarHeight.getStatusBarHeight(getResources()) - getPaddingTop()),
                        getPaddingRight(),
                        getPaddingBottom());
            }
        } else {
            setPadding(getPaddingLeft(),
                    StatusBarHeight.getStatusBarHeight(getResources()) + getPaddingTop(),
                    getPaddingRight(),
                    getPaddingBottom());
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            if (key.equals(DevelopmentPreferences.DISABLE_TRANSPARENT_STATUS)) {
                updatePadding();
            }
        } catch (Exception ignored) {
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        app.simple.inure.preferences.SharedPreferences.INSTANCE.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
