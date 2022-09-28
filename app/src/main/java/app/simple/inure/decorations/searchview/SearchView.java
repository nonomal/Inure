package app.simple.inure.decorations.searchview;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.text.DecimalFormat;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import app.simple.inure.R;
import app.simple.inure.decorations.typeface.TypeFaceEditText;
import app.simple.inure.decorations.typeface.TypeFaceTextView;
import app.simple.inure.preferences.SearchPreferences;
import app.simple.inure.themes.manager.ThemeManager;
import app.simple.inure.util.TextViewUtils;

public class SearchView extends LinearLayout implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    private TypeFaceEditText editText;
    private TypeFaceTextView number;
    private ImageButton imageButton;
    private SearchViewEventListener searchViewEventListener;
    
    private ValueAnimator numberAnimator;
    private final DecimalFormat format = new DecimalFormat();
    
    private int oldNumber = 0;
    
    public SearchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initViews();
        setProperties();
    }
    
    private void setProperties() {
        setElevation(5);
        setBackgroundColor(ThemeManager.INSTANCE.getTheme().getViewGroupTheme().getBackground());
        setOrientation(LinearLayout.HORIZONTAL);
        setLayoutTransition(new LayoutTransition());
    }
    
    private void initViews() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.search_view, this, true);
    
        editText = view.findViewById(R.id.search_view_text_input_layout);
        number = view.findViewById(R.id.search_number);
        imageButton = view.findViewById(R.id.search_view_menu_button);
    
        editText.setText(SearchPreferences.INSTANCE.getLastSearchKeyword());
        updateSearchIcon();
        editText.setSaveEnabled(false); // ViewModel and SharedPreferences will handle the saved states
        
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                /* no-op */
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editText.isFocused()) {
                    searchViewEventListener.onSearchTextChanged(s.toString().trim(), count);
                }
            }
    
            @Override
            public void afterTextChanged(Editable s) {
                /* no-op */
            }
        });
    
        imageButton.setOnClickListener(button -> searchViewEventListener.onSearchMenuPressed(button));
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        app.simple.inure.preferences.SharedPreferences.INSTANCE.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        editText.clearAnimation();
        imageButton.clearAnimation();
        if (numberAnimator != null) {
            numberAnimator.cancel();
        }
        app.simple.inure.preferences.SharedPreferences.INSTANCE.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
    
    public void setNewNumber(int number) {
        String pattern;
        if (number < 1000) {
            pattern = "000";
        } else if (number < 10000) {
            pattern = "0000";
        } else {
            this.number.setText("∞");
            return;
        }
        
        format.applyPattern(pattern);
        animateNumber(number);
    }
    
    private void animateNumber(int newNumber) {
        if (numberAnimator != null) {
            numberAnimator.cancel();
        }
    
        numberAnimator = ValueAnimator.ofInt(oldNumber, newNumber);
        numberAnimator.setInterpolator(new FastOutLinearInInterpolator());
        numberAnimator.setDuration(getResources().getInteger(R.integer.animation_duration));
        numberAnimator.addUpdateListener(animation -> {
            number.setText(format.format((int) animation.getAnimatedValue()));
            oldNumber = (int) animation.getAnimatedValue();
        });
        numberAnimator.start();
    }
    
    public void showInput() {
        editText.showInput();
    }
    
    private void updateSearchIcon() {
        if (SearchPreferences.INSTANCE.isDeepSearchEnabled()) {
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_deep_search, 0, 0, 0);
            TextViewUtils.INSTANCE.setDrawableTint(editText, Color.parseColor("#d35400"));
        } else {
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
            TextViewUtils.INSTANCE.setDrawableTint(editText, ThemeManager.INSTANCE.getTheme().getIconTheme().getSecondaryIconColor());
        }
    }
    
    public void setSearchViewEventListener(SearchViewEventListener searchViewEventListener) {
        this.searchViewEventListener = searchViewEventListener;
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SearchPreferences.deepSearch)) {
            updateSearchIcon();
        }
    }
    
    public TypeFaceEditText getEditText() {
        return editText;
    }
}
