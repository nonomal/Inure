package app.simple.inure.decorations.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;

import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import app.simple.inure.preferences.AccessibilityPreferences;
import app.simple.inure.preferences.AppearancePreferences;
import app.simple.inure.themes.manager.ThemeManager;

public class AppIconImageView extends AppCompatImageView implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    public AppIconImageView(@NonNull Context context) {
        super(context);
        init();
    }
    
    public AppIconImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public AppIconImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setCropToPadding(false);
    
        int size = 0;
        if (isInEditMode()) {
            size = AppearancePreferences.INSTANCE.getMaxIconSize();
        } else {
            size = AppearancePreferences.INSTANCE.getIconSize();
        }
    
        setPadding(size, size, size, size);
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            app.simple.inure.preferences.SharedPreferences.INSTANCE.registerSharedPreferencesListener(this);
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            app.simple.inure.preferences.SharedPreferences.INSTANCE.unregisterListener(this);
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppearancePreferences.ICON_SIZE)) {
            setSize(AppearancePreferences.INSTANCE.getIconSize());
        }
    }
    
    public void setSize(int size) {
        setPadding(size, size, size, size);
    }
    
    public void enableBorders() {
        setBackgroundColor(Color.TRANSPARENT);
        setBackgroundTintList(ColorStateList.valueOf(ThemeManager.INSTANCE.getTheme().getViewGroupTheme().getBackground()));
        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel()
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, AppearancePreferences.INSTANCE.getCornerRadius())
                .build();
        
        MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable(shapeAppearanceModel);
        
        if (AccessibilityPreferences.INSTANCE.isHighlightStroke()) {
            materialShapeDrawable.setStroke(3, AppearancePreferences.INSTANCE.getAccentColor());
        }
        
        this.setBackground(materialShapeDrawable);
    }
}
