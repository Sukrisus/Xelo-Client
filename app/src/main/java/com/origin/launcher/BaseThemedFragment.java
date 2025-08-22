package com.origin.launcher;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class BaseThemedFragment extends Fragment {
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Apply theme when view is created
        applyTheme();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Reapply theme when fragment resumes
        applyTheme();
    }
    
    private void applyTheme() {
        View rootView = getView();
        if (rootView != null) {
            ThemeUtils.applyThemeToRootView(rootView);
            
            // Allow subclasses to apply additional theming
            onApplyTheme();
        }
    }
    
    /**
     * Override this method in subclasses to apply theme to specific views
     */
    protected void onApplyTheme() {
        // Default implementation does nothing
    }
    
    /**
     * Call this method when theme changes to refresh the current fragment
     */
    protected void refreshTheme() {
        applyTheme();
    }
}