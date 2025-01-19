package app.simple.inure.decorations.ripple;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import app.simple.inure.R;
import app.simple.inure.constants.Misc;
import app.simple.inure.decorations.corners.LayoutBackground;
import app.simple.inure.decorations.theme.ThemeButton;
import app.simple.inure.loaders.ImageLoader;
import app.simple.inure.popups.app.PopupTooltip;
import app.simple.inure.preferences.AccessibilityPreferences;
import app.simple.inure.preferences.AppearancePreferences;
import app.simple.inure.themes.manager.Theme;
import app.simple.inure.themes.manager.ThemeManager;
import app.simple.inure.util.ColorUtils;
import app.simple.inure.util.ViewUtils;

public class DynamicRippleImageButton extends ThemeButton {
    
    private int rippleColor = -1;
    
    public DynamicRippleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        rippleColor = AppearancePreferences.INSTANCE.getAccentColor();
        setBackgroundColor(Color.TRANSPARENT);
    
        setOnLongClickListener(view -> {
            try {
                if (getContentDescription() != null || !getContentDescription().toString().isEmpty()) {
                    new PopupTooltip(view);
                }
            } catch (NullPointerException ignored) {
            }
            return false;
        });
    }
    
    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        setHighlightBackgroundColor();
    }
    
    @SuppressLint ("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                if (AccessibilityPreferences.INSTANCE.isHighlightMode()) {
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
            case MotionEvent.ACTION_MOVE,
                    MotionEvent.ACTION_CANCEL,
                    MotionEvent.ACTION_UP -> {
                if (AccessibilityPreferences.INSTANCE.isHighlightMode()) {
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
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearAnimation();
        setScaleX(1);
        setScaleY(1);
    }
    
    private void setHighlightBackgroundColor() {
        if (AccessibilityPreferences.INSTANCE.isHighlightMode()) {
            LayoutBackground.setBackground(getContext(), this, null, Misc.roundedCornerFactor);
            setBackgroundTintList(ColorStateList.valueOf(ThemeManager.INSTANCE.getTheme().getViewGroupTheme().getHighlightBackground()));
        } else {
            setBackground(null);
            setBackground(Utils.getCustomRippleDrawable(getBackground(), rippleColor, Misc.roundedCornerFactor));
        }
    }
    
    public void setIcon(int resId, boolean animate) {
        if (animate && !AccessibilityPreferences.INSTANCE.isAnimationReduced()) {
            ImageLoader.INSTANCE.loadImage(resId, this, 0);
        } else {
            setImageResource(resId);
        }
    }
    
    /**
     * Use this method to track selection in {@link androidx.recyclerview.widget.RecyclerView}.
     * This will change the background according to the accent color and will also keep
     * save the ripple effect.
     *
     * @param selected true for selected item
     */
    public void setDefaultBackground(boolean selected) {
        if (selected) {
            setBackgroundTintList(null);
            setBackgroundTintList(ColorStateList.valueOf(ColorUtils.INSTANCE.changeAlpha(
                    ColorUtils.INSTANCE.resolveAttrColor(getContext(), R.attr.colorAppAccent),
                    Misc.highlightColorAlpha)));
            
            LayoutBackground.setBackground(getContext(), this, null);
        } else {
            setBackground(null);
            setBackground(Utils.getRippleDrawable(getBackground()));
        }
    }
    
    public void setRippleColor(int rippleColor) {
        this.rippleColor = rippleColor;
        setHighlightBackgroundColor();
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Objects.equals(key, AppearancePreferences.ACCENT_COLOR)) {
            setHighlightBackgroundColor();
        }
    }
}
