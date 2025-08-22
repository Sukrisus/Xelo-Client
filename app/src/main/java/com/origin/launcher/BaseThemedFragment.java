package com.origin.launcher;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

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
            
            // Apply themes to common Material components that might not be caught by hierarchy walk
            applyThemeToCommonViews(rootView);
            
            // Allow subclasses to apply additional theming
            onApplyTheme();
        }
    }
    
    /**
     * Apply themes to common Material Design components
     */
    private void applyThemeToCommonViews(View rootView) {
        // Find and theme common components by ID patterns
        applyThemeToViewById(rootView, "card", MaterialCardView.class);
        applyThemeToViewById(rootView, "button", MaterialButton.class);
    }
    
    @SuppressWarnings("unchecked")
    private <T extends View> void applyThemeToViewById(View rootView, String idPattern, Class<T> viewClass) {
        if (rootView instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) rootView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                
                // Check if this view matches our criteria
                if (viewClass.isInstance(child)) {
                    // Apply appropriate theme
                    if (child instanceof MaterialCardView) {
                        ThemeUtils.applyThemeToCard((MaterialCardView) child, requireContext());
                    } else if (child instanceof MaterialButton) {
                        ThemeUtils.applyThemeToButton((MaterialButton) child, requireContext());
                    }
                }
                
                // Recursively check children
                applyThemeToViewById(child, idPattern, viewClass);
            }
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