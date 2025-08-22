package com.origin.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;

public class ThemeUtils {
    
    /**
     * Apply theme colors to a MaterialCardView
     */
    public static void applyThemeToCard(MaterialCardView card, Context context) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        card.setCardBackgroundColor(themeManager.getColor("surface"));
        card.setStrokeColor(themeManager.getColor("outline"));
        
        // Create ripple effect with theme colors
        RippleDrawable ripple = new RippleDrawable(
            ColorStateList.valueOf(themeManager.getColor("onSurface") & 0x1AFFFFFF),
            null,
            null
        );
        card.setForeground(ripple);
    }
    
    /**
     * Apply theme colors to a MaterialButton
     */
    public static void applyThemeToButton(MaterialButton button, Context context) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        button.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("primary")));
        button.setTextColor(themeManager.getColor("onPrimary"));
    }
    
    /**
     * Apply theme colors to a TextView
     */
    public static void applyThemeToTextView(TextView textView, String colorType) {
        ThemeManager themeManager = ThemeManager.getInstance();
        textView.setTextColor(themeManager.getColor(colorType));
    }
    
    /**
     * Apply theme colors to a MaterialRadioButton
     */
    public static void applyThemeToRadioButton(MaterialRadioButton radioButton, Context context) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        ColorStateList colorStateList = new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
            },
            new int[]{
                themeManager.getColor("primary"),
                themeManager.getColor("onSurfaceVariant")
            }
        );
        radioButton.setButtonTintList(colorStateList);
    }
    
    /**
     * Create a circular ripple drawable with theme colors
     */
    public static RippleDrawable createCircularRipple(String colorName) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(android.graphics.Color.TRANSPARENT);
        
        return new RippleDrawable(
            ColorStateList.valueOf(themeManager.getColor(colorName) & 0x1AFFFFFF),
            null,
            circle
        );
    }
    
    /**
     * Apply theme background to a view
     */
    public static void applyThemeBackground(View view, String colorName) {
        ThemeManager themeManager = ThemeManager.getInstance();
        view.setBackgroundColor(themeManager.getColor(colorName));
    }
    
    /**
     * Get themed color state list for various states
     */
    public static ColorStateList getThemedColorStateList(String enabledColor, String disabledColor) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        return new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_enabled},
                new int[]{-android.R.attr.state_enabled}
            },
            new int[]{
                themeManager.getColor(enabledColor),
                themeManager.getColor(disabledColor)
            }
        );
    }
    
    /**
     * Apply theme to the root view (typically the activity's main layout)
     */
    public static void applyThemeToRootView(View rootView) {
        ThemeManager themeManager = ThemeManager.getInstance();
        rootView.setBackgroundColor(themeManager.getColor("background"));
        
        // Recursively apply themes to common view types
        applyThemeToViewHierarchy(rootView);
    }
    
    /**
     * Recursively apply theme to all views in the hierarchy
     */
    private static void applyThemeToViewHierarchy(View view) {
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                applyThemeToViewHierarchy(child);
            }
        }
        
        // Apply theme to specific view types
        if (view instanceof MaterialCardView) {
            applyThemeToCard((MaterialCardView) view, view.getContext());
        } else if (view instanceof MaterialButton) {
            applyThemeToButton((MaterialButton) view, view.getContext());
        } else if (view instanceof MaterialRadioButton) {
            applyThemeToRadioButton((MaterialRadioButton) view, view.getContext());
        } else if (view instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
            applyThemeToBottomNavigation(view);
        } else if (view instanceof TextView) {
            // Apply default text color for TextViews
            applyThemeToTextView((TextView) view, "onSurface");
        } else if (view instanceof ImageView) {
            // Apply tint to ImageViews that might be icons
            ImageView imageView = (ImageView) view;
            if (imageView.getColorFilter() != null) {
                imageView.setColorFilter(ThemeManager.getInstance().getColor("onSurface"));
            }
        }
    }
    
    /**
     * Apply theme colors to BottomNavigationView
     */
    public static void applyThemeToBottomNavigation(View bottomNavView) {
        if (bottomNavView instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                (com.google.android.material.bottomnavigation.BottomNavigationView) bottomNavView;
            
            ThemeManager themeManager = ThemeManager.getInstance();
            bottomNav.setBackgroundColor(themeManager.getColor("surface"));
            bottomNav.setItemTextColor(getThemedColorStateList("onSurface", "onSurfaceVariant"));
            bottomNav.setItemIconTintList(getThemedColorStateList("onSurface", "onSurfaceVariant"));
        }
    }
}