package com.origin.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static final String TAG = "ThemeManager";
    private static final String PREF_NAME = "theme_preferences";
    private static final String PREF_CURRENT_THEME = "current_theme";
    private static final String DEFAULT_THEME = "default";
    
    private static ThemeManager instance;
    private Context context;
    private Map<String, Integer> currentColors;
    private String currentThemeName;
    private List<ThemeChangeListener> themeChangeListeners;
    
    /**
     * Interface for theme change notifications
     */
    public interface ThemeChangeListener {
        void onThemeChanged(String themeName);
    }
    
    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.currentColors = new HashMap<>();
        this.themeChangeListeners = new ArrayList<>();
        
        Log.d(TAG, "Initializing ThemeManager");
        
        // Load default theme immediately
        if (!loadCurrentTheme()) {
            Log.w(TAG, "Failed to load current theme, using hardcoded fallbacks");
            loadHardcodedFallbackColors();
        }
        
        Log.d(TAG, "ThemeManager initialized with theme: " + currentThemeName);
    }
    
    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ThemeManager not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }
    
    /**
     * Load theme from JSON file in assets/themes/ or from extracted .xtheme
     */
    public boolean loadTheme(String themeName) {
        // First try to load from assets (built-in themes)
        if (loadThemeFromAssets(themeName)) {
            return true;
        }
        
        // Then try to load from extracted .xtheme files
        return loadThemeFromXTheme(themeName);
    }
    
    private boolean loadThemeFromAssets(String themeName) {
        try {
            String jsonPath = "themes/" + themeName + ".json";
            InputStream inputStream = context.getAssets().open(jsonPath);
            
            return loadThemeFromInputStream(inputStream, themeName);
            
        } catch (IOException e) {
            Log.d(TAG, "Theme not found in assets: " + themeName);
            return false;
        }
    }
    
    private boolean loadThemeFromXTheme(String themeName) {
        try {
            // Look for extracted .xtheme theme
            File themesDir = new File(context.getExternalFilesDir(null), "themes");
            File themeDir = new File(themesDir, themeName);
            File colorsJsonFile = new File(themeDir, "colors/colors.json");
            
            if (!colorsJsonFile.exists()) {
                Log.d(TAG, "Theme not found in .xtheme: " + themeName);
                return false;
            }
            
            InputStream inputStream = new java.io.FileInputStream(colorsJsonFile);
            return loadThemeFromInputStream(inputStream, themeName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading .xtheme: " + themeName, e);
            return false;
        }
    }
    
    private boolean loadThemeFromInputStream(InputStream inputStream, String themeName) {
        try {
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            
            String jsonString = new String(buffer, "UTF-8");
            JSONObject themeJson = new JSONObject(jsonString);
            JSONObject colors = themeJson.getJSONObject("colors");
            
            // Parse colors from JSON
            Map<String, Integer> newColors = new HashMap<>();
            String[] colorKeys = {"background", "onBackground", "surface", "onSurface", 
                                "surfaceVariant", "onSurfaceVariant", "outline", "primary", "onPrimary",
                                "primaryContainer", "onPrimaryContainer", "secondary", "onSecondary",
                                "secondaryContainer", "onSecondaryContainer", "tertiary", "onTertiary",
                                "tertiaryContainer", "onTertiaryContainer", "error", "onError",
                                "errorContainer", "onErrorContainer", "success", "info", "warning"};
            
            for (String key : colorKeys) {
                if (colors.has(key)) {
                    String colorHex = colors.getString(key);
                    int color = Color.parseColor(colorHex);
                    newColors.put(key, color);
                }
            }
            
            // Parse toggle colors if they exist
            if (colors.has("toggle")) {
                try {
                    JSONObject toggleColors = colors.getJSONObject("toggle");
                    String[] toggleKeys = {"track", "trackChecked", "thumb", "thumbChecked", "ripple"};
                    
                    for (String toggleKey : toggleKeys) {
                        if (toggleColors.has(toggleKey)) {
                            String colorHex = toggleColors.getString(toggleKey);
                            int color = Color.parseColor(colorHex);
                            newColors.put("toggle_" + toggleKey, color);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing toggle colors, using defaults", e);
                }
            }
            
            // Update current colors
            currentColors.clear();
            currentColors.putAll(newColors);
            currentThemeName = themeName;
            
            // Save to preferences
            saveCurrentTheme(themeName);
            
            // Notify listeners of theme change
            notifyThemeChanged(themeName);
            
            Log.d(TAG, "Theme loaded successfully: " + themeName);
            return true;
            
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error parsing theme: " + themeName, e);
            return false;
        }
    }
    
    /**
     * Get color by name
     */
    public int getColor(String colorName) {
        if (colorName == null || colorName.isEmpty()) {
            Log.w(TAG, "Color name is null or empty, returning default");
            return Color.parseColor("#FFFFFF");
        }
        
        Integer color = currentColors.get(colorName);
        if (color != null) {
            return color;
        }
        
        // Fallback to Material Expressive colors if not found
        switch (colorName) {
            case "background": return Color.parseColor("#0B0B0F");
            case "onBackground": return Color.parseColor("#FEFEFE");
            case "surface": return Color.parseColor("#1A1A22");
            case "onSurface": return Color.parseColor("#FEFEFE");
            case "surfaceVariant": return Color.parseColor("#252530");
            case "onSurfaceVariant": return Color.parseColor("#D4D4DC");
            case "outline": return Color.parseColor("#6B6B78");
            case "outlineVariant": return Color.parseColor("#3D3D48");
            case "primary": return Color.parseColor("#7C4DFF");
            case "onPrimary": return Color.parseColor("#FFFFFF");
            case "primaryContainer": return Color.parseColor("#5E35B1");
            case "onPrimaryContainer": return Color.parseColor("#FFFFFF");
            case "secondary": return Color.parseColor("#00E5FF");
            case "onSecondary": return Color.parseColor("#000000");
            case "secondaryContainer": return Color.parseColor("#00ACC1");
            case "onSecondaryContainer": return Color.parseColor("#FFFFFF");
            case "tertiary": return Color.parseColor("#FF6F00");
            case "onTertiary": return Color.parseColor("#FFFFFF");
            case "tertiaryContainer": return Color.parseColor("#E65100");
            case "onTertiaryContainer": return Color.parseColor("#FFFFFF");
            case "error": return Color.parseColor("#FF5252");
            case "onError": return Color.parseColor("#FFFFFF");
            case "errorContainer": return Color.parseColor("#D32F2F");
            case "onErrorContainer": return Color.parseColor("#FFFFFF");
            case "success": return Color.parseColor("#4CAF50");
            case "onSuccess": return Color.parseColor("#FFFFFF");
            case "successContainer": return Color.parseColor("#2E7D32");
            case "onSuccessContainer": return Color.parseColor("#FFFFFF");
            case "warning": return Color.parseColor("#FF9800");
            case "onWarning": return Color.parseColor("#000000");
            case "warningContainer": return Color.parseColor("#F57C00");
            case "onWarningContainer": return Color.parseColor("#FFFFFF");
            case "info": return Color.parseColor("#2196F3");
            case "onInfo": return Color.parseColor("#FFFFFF");
            case "infoContainer": return Color.parseColor("#1976D2");
            case "onInfoContainer": return Color.parseColor("#FFFFFF");
            case "accent1": return Color.parseColor("#FF4081");
            case "accent2": return Color.parseColor("#00BCD4");
            case "accent3": return Color.parseColor("#8BC34A");
            default: 
                Log.w(TAG, "Unknown color name: " + colorName + ", returning default");
                return Color.parseColor("#FFFFFF");
        }
    }
    
    /**
     * Get theme metadata from JSON
     */
    public ThemeMetadata getThemeMetadata(String themeName) {
        // First try to get metadata from assets (built-in themes)
        try {
            String jsonPath = "themes/" + themeName + ".json";
            InputStream inputStream = context.getAssets().open(jsonPath);
            
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            
            String jsonString = new String(buffer, "UTF-8");
            JSONObject themeJson = new JSONObject(jsonString);
            
            String name = themeJson.optString("name", themeName);
            String author = themeJson.optString("author", null);
            String description = themeJson.optString("description", "Custom theme");
            
            return new ThemeMetadata(name, author, description, themeName);
            
        } catch (IOException | JSONException e) {
            Log.d(TAG, "Theme metadata not found in assets: " + themeName);
        }
        
        // Then try to get metadata from .xtheme files
        try {
            File themesDir = new File(context.getExternalFilesDir(null), "themes");
            File themeDir = new File(themesDir, themeName);
            File manifestFile = new File(themeDir, "manifest.json");
            File colorsJsonFile = new File(themeDir, "colors/colors.json");
            
            // First try manifest.json
            if (manifestFile.exists()) {
                InputStream inputStream = new java.io.FileInputStream(manifestFile);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();
                
                String jsonString = new String(buffer, "UTF-8");
                JSONObject manifestJson = new JSONObject(jsonString);
                
                String name = manifestJson.optString("name", themeName);
                String author = manifestJson.optString("author", null);
                String description = manifestJson.optString("description", "Custom theme");
                
                return new ThemeMetadata(name, author, description, themeName);
            }
            // Fallback to colors.json for compatibility
            else if (colorsJsonFile.exists()) {
                InputStream inputStream = new java.io.FileInputStream(colorsJsonFile);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();
                
                String jsonString = new String(buffer, "UTF-8");
                JSONObject themeJson = new JSONObject(jsonString);
                
                String name = themeJson.optString("name", themeName);
                String author = themeJson.optString("author", null);
                String description = themeJson.optString("description", "Custom theme");
                
                return new ThemeMetadata(name, author, description, themeName);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading .xtheme metadata: " + themeName, e);
        }
        
        // Fallback
        return new ThemeMetadata(themeName, null, "Custom theme", themeName);
    }
    
    /**
     * Get list of available themes from assets
     */
    public String[] getAvailableThemes() {
        try {
            String[] themeFiles = context.getAssets().list("themes");
            if (themeFiles == null) return new String[0];
            
            String[] themeNames = new String[themeFiles.length];
            for (int i = 0; i < themeFiles.length; i++) {
                // Remove .json extension
                themeNames[i] = themeFiles[i].replace(".json", "");
            }
            return themeNames;
            
        } catch (IOException e) {
            Log.e(TAG, "Error listing themes", e);
            return new String[0];
        }
    }
    
    /**
     * Apply theme to the current activity (call this in onCreate/onResume)
     */
    public void applyTheme(Context activityContext) {
        // This method can be extended to apply theme to specific views
        // For now, it ensures the theme is loaded
        if (currentColors.isEmpty()) {
            loadCurrentTheme();
        }
    }
    
    private boolean loadCurrentTheme() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String themeName = prefs.getString(PREF_CURRENT_THEME, DEFAULT_THEME);
        
        Log.d(TAG, "Loading current theme: " + themeName);
        
        if (!loadTheme(themeName)) {
            // Fallback to default theme
            Log.w(TAG, "Failed to load theme " + themeName + ", falling back to default");
            if (!loadTheme(DEFAULT_THEME)) {
                Log.e(TAG, "Failed to load default theme, using hardcoded fallbacks");
                return false; // Indicate failure
            }
        }
        return true; // Indicate success
    }
    
    /**
     * Load hardcoded fallback colors when theme loading fails
     */
    private void loadHardcodedFallbackColors() {
        currentColors.clear();
        // Material Expressive fallback colors
        currentColors.put("background", Color.parseColor("#0B0B0F"));
        currentColors.put("onBackground", Color.parseColor("#FEFEFE"));
        currentColors.put("surface", Color.parseColor("#1A1A22"));
        currentColors.put("onSurface", Color.parseColor("#FEFEFE"));
        currentColors.put("surfaceVariant", Color.parseColor("#252530"));
        currentColors.put("onSurfaceVariant", Color.parseColor("#D4D4DC"));
        currentColors.put("outline", Color.parseColor("#6B6B78"));
        currentColors.put("outlineVariant", Color.parseColor("#3D3D48"));
        currentColors.put("primary", Color.parseColor("#7C4DFF"));
        currentColors.put("onPrimary", Color.parseColor("#FFFFFF"));
        currentColors.put("primaryContainer", Color.parseColor("#5E35B1"));
        currentColors.put("onPrimaryContainer", Color.parseColor("#FFFFFF"));
        currentColors.put("secondary", Color.parseColor("#00E5FF"));
        currentColors.put("onSecondary", Color.parseColor("#000000"));
        currentColors.put("secondaryContainer", Color.parseColor("#00ACC1"));
        currentColors.put("onSecondaryContainer", Color.parseColor("#FFFFFF"));
        currentColors.put("tertiary", Color.parseColor("#FF6F00"));
        currentColors.put("onTertiary", Color.parseColor("#FFFFFF"));
        currentColors.put("tertiaryContainer", Color.parseColor("#E65100"));
        currentColors.put("onTertiaryContainer", Color.parseColor("#FFFFFF"));
        currentColors.put("error", Color.parseColor("#FF5252"));
        currentColors.put("onError", Color.parseColor("#FFFFFF"));
        currentColors.put("errorContainer", Color.parseColor("#D32F2F"));
        currentColors.put("onErrorContainer", Color.parseColor("#FFFFFF"));
        currentColors.put("success", Color.parseColor("#4CAF50"));
        currentColors.put("onSuccess", Color.parseColor("#FFFFFF"));
        currentColors.put("successContainer", Color.parseColor("#2E7D32"));
        currentColors.put("onSuccessContainer", Color.parseColor("#FFFFFF"));
        currentColors.put("warning", Color.parseColor("#FF9800"));
        currentColors.put("onWarning", Color.parseColor("#000000"));
        currentColors.put("warningContainer", Color.parseColor("#F57C00"));
        currentColors.put("onWarningContainer", Color.parseColor("#FFFFFF"));
        currentColors.put("info", Color.parseColor("#2196F3"));
        currentColors.put("onInfo", Color.parseColor("#FFFFFF"));
        currentColors.put("infoContainer", Color.parseColor("#1976D2"));
        currentColors.put("onInfoContainer", Color.parseColor("#FFFFFF"));
        currentColors.put("accent1", Color.parseColor("#FF4081"));
        currentColors.put("accent2", Color.parseColor("#00BCD4"));
        currentColors.put("accent3", Color.parseColor("#8BC34A"));
        
        currentThemeName = "fallback";
        Log.d(TAG, "Material Expressive hardcoded fallback colors loaded");
    }
    
    private void saveCurrentTheme(String themeName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_CURRENT_THEME, themeName).apply();
    }
    
    public String getCurrentThemeName() {
        if (currentThemeName == null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            currentThemeName = prefs.getString(PREF_CURRENT_THEME, DEFAULT_THEME);
        }
        return currentThemeName;
    }
    
    /**
     * Force refresh the current theme
     */
    public void refreshCurrentTheme() {
        Log.d(TAG, "Refreshing current theme: " + currentThemeName);
        if (currentThemeName != null && !currentThemeName.equals("fallback")) {
            loadTheme(currentThemeName);
        } else {
            loadTheme(DEFAULT_THEME);
        }
    }
    
    /**
     * Check if theme is properly loaded
     */
    public boolean isThemeLoaded() {
        return !currentColors.isEmpty() && currentThemeName != null;
    }
    
    /**
     * Get current theme colors map
     */
    public Map<String, Integer> getCurrentColors() {
        return new HashMap<>(currentColors);
    }
    
    /**
     * Add a theme change listener
     */
    public void addThemeChangeListener(ThemeChangeListener listener) {
        if (listener != null && !themeChangeListeners.contains(listener)) {
            themeChangeListeners.add(listener);
        }
    }
    
    /**
     * Remove a theme change listener
     */
    public void removeThemeChangeListener(ThemeChangeListener listener) {
        if (listener != null) {
            themeChangeListeners.remove(listener);
        }
    }
    
    /**
     * Notify all listeners of theme change
     */
    private void notifyThemeChanged(String themeName) {
        for (ThemeChangeListener listener : themeChangeListeners) {
            try {
                listener.onThemeChanged(themeName);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying theme change listener", e);
            }
        }
    }
    
    /**
     * Theme metadata class
     */
    public static class ThemeMetadata {
        public final String name;
        public final String author;
        public final String description;
        public final String key;
        
        public ThemeMetadata(String name, String author, String description, String key) {
            this.name = name;
            this.author = author;
            this.description = description;
            this.key = key;
        }
    }

    /**
     * Get toggle color by type
     */
    public int getToggleColor(String colorType) {
        try {
            // Check if toggle colors are available in the current theme
            if (hasToggleColors()) {
                String toggleKey = "toggle_" + colorType;
                if (currentColors.containsKey(toggleKey)) {
                    return currentColors.get(toggleKey);
                }
            }
            // Fallback to default toggle colors
            return getDefaultToggleColor(colorType);
        } catch (Exception e) {
            return getDefaultToggleColor(colorType);
        }
    }
    
    /**
     * Get default toggle color if theme doesn't specify it
     */
    private int getDefaultToggleColor(String colorType) {
        switch (colorType) {
            case "track":
                return Color.parseColor("#3D3D48");
            case "trackChecked":
                return Color.parseColor("#7C4DFF");
            case "thumb":
                return Color.parseColor("#FEFEFE");
            case "thumbChecked":
                return Color.parseColor("#FFFFFF");
            case "ripple":
                return Color.parseColor("#7C4DFF");
            default:
                return Color.parseColor("#3D3D48");
        }
    }
    
    /**
     * Check if toggle colors are available in current theme
     */
    public boolean hasToggleColors() {
        try {
            // Check if any toggle colors exist in the current theme
            return currentColors.containsKey("toggle_track") || 
                   currentColors.containsKey("toggle_trackChecked") ||
                   currentColors.containsKey("toggle_thumb") ||
                   currentColors.containsKey("toggle_thumbChecked") ||
                   currentColors.containsKey("toggle_ripple");
        } catch (Exception e) {
            return false;
        }
    }
}