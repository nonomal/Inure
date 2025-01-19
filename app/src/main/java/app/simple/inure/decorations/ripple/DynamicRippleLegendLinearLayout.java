package app.simple.inure.decorations.ripple;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import app.simple.inure.R;
import app.simple.inure.constants.Misc;
import app.simple.inure.decorations.corners.LayoutBackground;
import app.simple.inure.preferences.AccessibilityPreferences;
import app.simple.inure.preferences.AppearancePreferences;
import app.simple.inure.themes.interfaces.ThemeChangedListener;
import app.simple.inure.themes.manager.Theme;
import app.simple.inure.themes.manager.ThemeManager;
import app.simple.inure.util.ViewUtils;

public class DynamicRippleLegendLinearLayout extends LinearLayout implements ThemeChangedListener, SharedPreferences.OnSharedPreferenceChangeListener {
    
    private int rippleColor;
    
    public DynamicRippleLegendLinearLayout(Context context) {
        super(context);
        setBackgroundColor(Color.TRANSPARENT);
    }
    
    public DynamicRippleLegendLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.TRANSPARENT);
    }
    
    public DynamicRippleLegendLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.TRANSPARENT);
    }
    
    /**
     * @noinspection unused
     */
    public DynamicRippleLegendLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setBackgroundColor(Color.TRANSPARENT);
    }
    
    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        setHighlightBackgroundColor();
        super.setOnClickListener(l);
    }
    
    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        setBackground(Utils.getRoundedBackground(Misc.roundedCornerFactor));
        setClickable(false);
        setSelectedBackgroundColor();
    }
    
    @SuppressLint ("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                if (AccessibilityPreferences.INSTANCE.isHighlightMode() && isClickable()) {
                    animate()
                            .scaleY(0.8F)
                            .scaleX(0.8F)
                            .alpha(0.5F)
                            .setInterpolator(new LinearOutSlowInInterpolator())
                            .setDuration(getResources().getInteger(R.integer.animation_duration))
                            .start();
                }
                
                try {
                    if (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            if (isLongClickable()) {
                                if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                                    performLongClick();
                                    return true;
                                } else {
                                    return super.onTouchEvent(event);
                                }
                            } else {
                                return super.onTouchEvent(event);
                            }
                        } else {
                            return super.onTouchEvent(event);
                        }
                    } else {
                        return super.onTouchEvent(event);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return super.onTouchEvent(event);
                }
            }
            case MotionEvent.ACTION_MOVE, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (AccessibilityPreferences.INSTANCE.isHighlightMode() && isClickable()) {
                    animate()
                            .scaleY(1F)
                            .scaleX(1F)
                            .alpha(1F)
                            .setStartDelay(50)
                            .setInterpolator(new LinearOutSlowInInterpolator())
                            .setDuration(getResources().getInteger(R.integer.animation_duration))
                            .start();
                }
            }
        }
        return super.onTouchEvent(event);
    }
    
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        ViewUtils.INSTANCE.triggerHover(this, event);
        return super.onGenericMotionEvent(event);
    }
    
    @Override
    public void onThemeChanged(@NonNull Theme theme, boolean animate) {
        if (isClickable()) {
            setHighlightBackgroundColor();
        } else if (isSelected()) {
            setSelectedBackgroundColor();
        }
    }
    
    private void setSelectedBackgroundColor() {
        if (AccessibilityPreferences.INSTANCE.isHighlightMode()) {
            setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        } else {
            setBackgroundTintList(ColorStateList.valueOf(ThemeManager.INSTANCE.getTheme().getViewGroupTheme().getSelectedBackground()));
        }
    }
    
    private void setHighlightBackgroundColor() {
        if (AccessibilityPreferences.INSTANCE.isHighlightMode()) {
            LayoutBackground.setBackground(getContext(), this, null, Misc.roundedCornerFactor);
            setBackgroundTintList(ColorStateList.valueOf(ThemeManager.INSTANCE.getTheme().getViewGroupTheme().getHighlightBackground()));
        } else {
            setBackground(null);
            setBackground(Utils.getCustomRippleDrawable(getBackground(), rippleColor));
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(@Nullable SharedPreferences sharedPreferences, @Nullable String key) {
        if (Objects.equals(key, AppearancePreferences.ACCENT_COLOR) ||
                Objects.equals(key, AccessibilityPreferences.IS_HIGHLIGHT_STROKE) ||
                Objects.equals(key, AccessibilityPreferences.IS_HIGHLIGHT_MODE)) {
            setHighlightBackgroundColor();
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearAnimation();
        setScaleX(1);
        setScaleY(1);
    }
    
    public void setRippleColor(int rippleColor) {
        this.rippleColor = rippleColor;
    }
    
    public void highlight(int color) {
        LayoutBackground.setBackground(getContext(), this, null, Misc.roundedCornerFactor);
        setBackgroundTintList(ColorStateList.valueOf(color));
    }
    
    public void unHighlight() {
        setHighlightBackgroundColor();
    }
}
