package com.origin.launcher;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseThemedActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply theme before setting content view
        applyTheme();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Reapply theme when activity resumes (in case theme was changed)
        applyTheme();
    }
    
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        
        // Apply theme to root view after content is set
        applyThemeToViews();
    }
    
    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        
        // Apply theme to root view after content is set
        applyThemeToViews();
    }
    
    private void applyTheme() {
        try {
            // Ensure ThemeManager is initialized
            ThemeManager.getInstance();
        } catch (IllegalStateException e) {
            // ThemeManager not initialized, initialize it
            ThemeManager.getInstance(this);
        }
    }
    
    private void applyThemeToViews() {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ThemeUtils.applyThemeToRootView(rootView);
        }
        
        // Allow subclasses to apply additional theming
        onApplyTheme();
    }
    
    /**
     * Override this method in subclasses to apply theme to specific views
     */
    protected void onApplyTheme() {
        // Default implementation does nothing
    }
    
    /**
     * Call this method when theme changes to refresh the current activity
     */
    protected void refreshTheme() {
        applyThemeToViews();
    }
}