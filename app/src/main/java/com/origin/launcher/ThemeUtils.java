package com.origin.launcher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.graphics.Color;
import com.google.android.material.materialswitch.MaterialSwitch;

public class ThemeUtils {
    
    /**
     * Apply theme colors to a MaterialCardView
     */
    public static void applyThemeToCard(MaterialCardView card, Context context) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            
            if (themeManager != null && themeManager.isThemeLoaded()) {
                card.setCardBackgroundColor(themeManager.getColor("surface"));
                card.setStrokeColor(themeManager.getColor("outline"));
                card.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density)); // 1dp stroke
                card.setCardElevation(0f); // Remove elevation for flat design
                card.setRadius(12 * context.getResources().getDisplayMetrics().density); // 12dp radius
                
                // Create ripple effect with theme colors
                RippleDrawable ripple = new RippleDrawable(
                    ColorStateList.valueOf(createOptimizedRippleColor("onSurface", "card")),
                    null,
                    null
                );
                card.setForeground(ripple);
            } else {
                // Fallback to default colors if theme not ready
                card.setCardBackgroundColor(Color.parseColor("#141414"));
                card.setStrokeColor(Color.parseColor("#505050"));
                card.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
                card.setCardElevation(0f);
                card.setRadius(12 * context.getResources().getDisplayMetrics().density);
            }
        } catch (Exception e) {
            // Fallback to default colors on error
            card.setCardBackgroundColor(Color.parseColor("#141414"));
            card.setStrokeColor(Color.parseColor("#505050"));
            card.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
            card.setCardElevation(0f);
            card.setRadius(12 * context.getResources().getDisplayMetrics().density);
        }
    }
    
    /**
     * Apply theme colors to a MaterialButton
     */
    public static void applyThemeToButton(MaterialButton button, Context context) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        // Determine button type and apply appropriate styling
        String buttonType = determineButtonType(button);
        
        // Create lighter ripple colors for better visibility
        int lightRippleColor = createOptimizedRippleColor("primary", "button");
        int lightSurfaceRippleColor = createOptimizedRippleColor("surfaceVariant", "button");
        int lightOutlineRippleColor = createOptimizedRippleColor("outline", "button");
        
        switch (buttonType) {
            case "outlined":
                // Outlined button: transparent background, colored border and text
                button.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                button.setTextColor(themeManager.getColor("onSurface")); // Use onSurface for better visibility
                button.setStrokeColor(ColorStateList.valueOf(themeManager.getColor("outline")));
                button.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
                button.setRippleColor(ColorStateList.valueOf(lightOutlineRippleColor));
                break;
            case "text":
                // Text button: transparent background, colored text only
                button.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                button.setTextColor(themeManager.getColor("onSurface")); // Use onSurface for better visibility
                button.setRippleColor(ColorStateList.valueOf(lightRippleColor));
                break;
            case "filled":
            default:
                // Filled button: colored background, contrasting text
                ColorStateList enabledStates = getThemedColorStateList("primary", "surfaceVariant");
                button.setBackgroundTintList(enabledStates);
                button.setTextColor(themeManager.getColor("onPrimary"));
                button.setRippleColor(ColorStateList.valueOf(lightSurfaceRippleColor));
                break;
        }
    }
    
    /**
     * Determine button type based on current styling
     */
    private static String determineButtonType(MaterialButton button) {
        // Check view tag for button type hint
        Object tag = button.getTag();
        if (tag != null) {
            String tagStr = tag.toString().toLowerCase();
            if (tagStr.contains("outlined")) return "outlined";
            if (tagStr.contains("text")) return "text";
        }
        
        // Check if button has outlined style characteristics
        if (button.getStrokeWidth() > 0) {
            return "outlined";
        }
        
        // Check if background is transparent
        if (button.getBackgroundTintList() != null && 
            button.getBackgroundTintList().equals(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))) {
            return "text";
        }
        
        // Check button ID to determine type
        String resourceName = "";
        try {
            resourceName = button.getContext().getResources().getResourceEntryName(button.getId()).toLowerCase();
        } catch (Exception e) {
            // Ignore, use default
        }
        
        if (resourceName.contains("import") || resourceName.contains("export")) {
            // Import/Export buttons are typically outlined
            return "outlined";
        }
        
        return "filled";
    }
    
    /**
     * Apply theme colors to a TextView
     */
    public static void applyThemeToTextView(TextView textView, String colorType) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            if (themeManager != null && themeManager.isThemeLoaded()) {
                textView.setTextColor(themeManager.getColor(colorType));
            } else {
                // Fallback to default colors if theme not ready
                switch (colorType) {
                    case "onSurface":
                        textView.setTextColor(Color.parseColor("#FFFFFF"));
                        break;
                    case "onSurfaceVariant":
                        textView.setTextColor(Color.parseColor("#CCCCCC"));
                        break;
                    default:
                        textView.setTextColor(Color.parseColor("#FFFFFF"));
                        break;
                }
            }
        } catch (Exception e) {
            // Fallback to default colors on error
            textView.setTextColor(Color.parseColor("#FFFFFF"));
        }
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
            ColorStateList.valueOf(createOptimizedRippleColor(colorName, "button")),
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
        
        // Apply theme to specific view types more selectively
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            // Always update colors, but preserve stroke width if already set
            ThemeManager themeManager = ThemeManager.getInstance();
            card.setCardBackgroundColor(themeManager.getColor("surface"));
            card.setStrokeColor(themeManager.getColor("outline"));
            // Only set stroke width if it's currently 0 (not manually set)
            if (card.getStrokeWidth() == 0) {
                card.setStrokeWidth((int) (1 * view.getContext().getResources().getDisplayMetrics().density));
            }
        } else if (view instanceof MaterialButton) {
            // Always apply theming to override hardcoded colors from XML
            MaterialButton button = (MaterialButton) view;
            applyThemeToButton(button, view.getContext());
        } else if (view instanceof MaterialRadioButton) {
            applyThemeToRadioButton((MaterialRadioButton) view, view.getContext());
        } else if (view instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
            applyThemeToBottomNavigation(view);
        } else if (view instanceof TextInputLayout) {
            applyThemeToTextInputLayout((TextInputLayout) view);
        } else if (view instanceof EditText && !(view instanceof TextInputEditText)) {
            // Only theme EditTexts that don't have custom styling
            EditText editText = (EditText) view;
            if (editText.getBackground() == null || editText.getCurrentTextColor() == android.graphics.Color.BLACK) {
                applyThemeToEditText(editText);
            }
        }
        // Removed automatic TextView and ImageView theming to preserve custom styling
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
    
    /**
     * Apply theme colors to TextInputLayout
     */
    public static void applyThemeToTextInputLayout(TextInputLayout textInputLayout) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        textInputLayout.setBoxBackgroundColor(themeManager.getColor("surfaceVariant"));
        textInputLayout.setHintTextColor(getThemedColorStateList("onSurfaceVariant", "onSurfaceVariant"));
        textInputLayout.setBoxStrokeColor(themeManager.getColor("outline"));
        
        // Apply theme to the EditText inside
        EditText editText = textInputLayout.getEditText();
        if (editText != null) {
            editText.setTextColor(themeManager.getColor("onSurface"));
            editText.setHintTextColor(themeManager.getColor("onSurfaceVariant"));
        }
    }
    
    /**
     * Apply theme colors to EditText
     */
    public static void applyThemeToEditText(EditText editText) {
        ThemeManager themeManager = ThemeManager.getInstance();
        editText.setTextColor(themeManager.getColor("onSurface"));
        editText.setHintTextColor(themeManager.getColor("onSurfaceVariant"));
        editText.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("outline")));
    }
    
    /**
     * Apply theme colors to share/action buttons
     */
    public static void applyThemeToActionButton(MaterialButton button, String colorType) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        switch (colorType) {
            case "primary":
                button.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("primary")));
                button.setTextColor(themeManager.getColor("onPrimary"));
                break;
            case "secondary":
                button.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("secondary")));
                button.setTextColor(themeManager.getColor("onSecondary"));
                break;
            case "error":
                button.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("error")));
                button.setTextColor(themeManager.getColor("onError"));
                break;
            case "success":
                button.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("success")));
                button.setTextColor(themeManager.getColor("onSurface"));
                break;
            default:
                applyThemeToButton(button, button.getContext());
                break;
        }
    }
    
    /**
     * Apply theme colors to Material AlertDialog
     */
    public static void applyThemeToDialog(androidx.appcompat.app.AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        
        try {
            // Apply background color to dialog
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            
            // Get the root view and apply theme
            View dialogView = dialog.findViewById(android.R.id.content);
            if (dialogView != null) {
                dialogView.setBackgroundColor(ThemeManager.getInstance().getColor("surface"));
            }
            
            // Apply theme to buttons
            android.widget.Button positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            android.widget.Button negativeButton = dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
            android.widget.Button neutralButton = dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL);
            
            if (positiveButton != null) {
                positiveButton.setTextColor(ThemeManager.getInstance().getColor("primary"));
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(ThemeManager.getInstance().getColor("onSurfaceVariant"));
            }
            if (neutralButton != null) {
                neutralButton.setTextColor(ThemeManager.getInstance().getColor("onSurfaceVariant"));
            }
            
        } catch (Exception e) {
            // Ignore theming errors
        }
    }

    /**
     * Apply theme colors to MaterialSwitch
     */
    public static void applyThemeToSwitch(MaterialSwitch materialSwitch, Context context) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            if (themeManager != null && themeManager.isThemeLoaded()) {
                // Use toggle colors if available, otherwise fall back to theme colors
                int trackColor, trackCheckedColor, thumbColor, thumbCheckedColor;
                
                if (themeManager.hasToggleColors()) {
                    // Use theme-specific toggle colors
                    trackColor = themeManager.getToggleColor("track");
                    trackCheckedColor = themeManager.getToggleColor("trackChecked");
                    thumbColor = themeManager.getToggleColor("thumb");
                    thumbCheckedColor = themeManager.getToggleColor("thumbChecked");
                } else {
                    // Fall back to theme colors
                    trackColor = themeManager.getColor("surfaceVariant");
                    trackCheckedColor = themeManager.getColor("primary");
                    thumbColor = themeManager.getColor("onSurface");
                    thumbCheckedColor = themeManager.getColor("onSurface");
                }
                
                // Create color state lists for different states
                ColorStateList trackColorStateList = new ColorStateList(
                    new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                    },
                    new int[]{trackCheckedColor, trackColor}
                );
                
                ColorStateList thumbColorStateList = new ColorStateList(
                    new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                    },
                    new int[]{thumbCheckedColor, thumbColor}
                );
                
                // Apply colors to the switch
                materialSwitch.setTrackTintList(trackColorStateList);
                materialSwitch.setThumbTintList(thumbColorStateList);
                
                // Add ripple effect if toggle colors are available
                if (themeManager.hasToggleColors()) {
                    int rippleColor = themeManager.getToggleColor("ripple");
                    materialSwitch.setRippleColor(ColorStateList.valueOf(createLightRippleColor(rippleColor)));
                }
            } else {
                // Fallback to default colors if theme not ready
                materialSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#2A2A2A")));
                materialSwitch.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
            }
        } catch (Exception e) {
            // Fallback to default colors on error
            materialSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#2A2A2A")));
            materialSwitch.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        }
    }

    /**
     * Create a lighter ripple color for better visibility
     */
    private static int createLightRippleColor(int baseColor) {
        // Convert to HSV for better color manipulation
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(baseColor, hsv);
        
        // For very dark colors, create a light gray ripple
        if (hsv[2] < 0.3f) {
            // Create a light gray with slight tint of the original color
            hsv[0] = hsv[0]; // Keep hue
            hsv[1] = 0.1f;   // Very low saturation
            hsv[2] = 0.7f;   // High lightness
        } else {
            // For lighter colors, increase lightness and reduce saturation
            hsv[1] = Math.max(0.15f, hsv[1] * 0.5f); // Reduce saturation more
            hsv[2] = Math.min(0.85f, hsv[2] * 1.4f); // Increase lightness more
        }
        
        // Convert back to color and add transparency
        int lightColor = android.graphics.Color.HSVToColor(hsv);
        
        // Make it semi-transparent for a subtle ripple effect
        return (lightColor & 0x00FFFFFF) | 0x50000000; // 31% opacity for better visibility
    }
    
    /**
     * Create a ripple color optimized for specific UI element types
     */
    public static int createOptimizedRippleColor(String colorName, String elementType) {
        ThemeManager themeManager = ThemeManager.getInstance();
        int baseColor = themeManager.getColor(colorName);
        
        switch (elementType) {
            case "button":
                // Buttons get lighter, more visible ripples
                return createLightRippleColor(baseColor);
            case "card":
                // Cards get subtle, elegant ripples
                return createSubtleRippleColor(baseColor);
            case "switch":
                // Switches get medium visibility ripples
                return createMediumRippleColor(baseColor);
            default:
                return createLightRippleColor(baseColor);
        }
    }
    
    /**
     * Create a subtle ripple color for cards and other subtle elements
     */
    private static int createSubtleRippleColor(int baseColor) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(baseColor, hsv);
        
        // Create very subtle ripple
        hsv[1] = Math.max(0.05f, hsv[1] * 0.3f); // Very low saturation
        hsv[2] = Math.min(0.8f, hsv[2] * 1.2f);  // Moderate lightness increase
        
        int subtleColor = android.graphics.Color.HSVToColor(hsv);
        return (subtleColor & 0x00FFFFFF) | 0x30000000; // 19% opacity for subtle effect
    }
    
    /**
     * Create a medium visibility ripple color for switches and other medium elements
     */
    private static int createMediumRippleColor(int baseColor) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(baseColor, hsv);
        
        // Create medium visibility ripple
        hsv[1] = Math.max(0.1f, hsv[1] * 0.4f);  // Low saturation
        hsv[2] = Math.min(0.8f, hsv[2] * 1.3f);  // Good lightness increase
        
        int mediumColor = android.graphics.Color.HSVToColor(hsv);
        return (mediumColor & 0x00FFFFFF) | 0x40000000; // 25% opacity for medium effect
    }

    /**
     * Refresh all ripple effects in a view hierarchy
     */
    public static void refreshRippleEffects(View view) {
        try {
            if (view instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) view;
                // Reapply theme to refresh ripple effects
                applyThemeToCard(card, view.getContext());
            } else if (view instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) view;
                // Reapply theme to refresh ripple effects
                applyThemeToButton(button, view.getContext());
            }
            
            // Recursively refresh child views
            if (view instanceof android.view.ViewGroup) {
                android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    refreshRippleEffects(child);
                }
            }
        } catch (Exception e) {
            // Handle error gracefully
        }
    }
}
