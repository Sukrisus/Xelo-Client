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

public class ThemeManager {
    private static final String TAG = "ThemeManager";
    private static final String PREF_NAME = "theme_preferences";
    private static final String PREF_CURRENT_THEME = "current_theme";
    private static final String DEFAULT_THEME = "default";
    
    private static ThemeManager instance;
    private Context context;
    private Map<String, Integer> currentColors;
    private String currentThemeName;
    
    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.currentColors = new HashMap<>();
        
        Log.d(TAG, "Initializing ThemeManager");
        loadCurrentTheme();
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
            
            // Update current colors
            currentColors.clear();
            currentColors.putAll(newColors);
            currentThemeName = themeName;
            
            // Save to preferences
            saveCurrentTheme(themeName);
            
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
        Integer color = currentColors.get(colorName);
        if (color != null) {
            return color;
        }
        
        // Fallback to default colors if not found
        switch (colorName) {
            case "background": return Color.parseColor("#121212");
            case "onBackground": return Color.parseColor("#FFFFFF");
            case "surface": return Color.parseColor("#1E1E1E");
            case "onSurface": return Color.parseColor("#FFFFFF");
            case "surfaceVariant": return Color.parseColor("#2D2D2D");
            case "onSurfaceVariant": return Color.parseColor("#CCCCCC");
            case "outline": return Color.parseColor("#737373");
            case "primary": return Color.parseColor("#BB86FC");
            case "onPrimary": return Color.parseColor("#000000");
            case "primaryContainer": return Color.parseColor("#3700B3");
            case "onPrimaryContainer": return Color.parseColor("#FFFFFF");
            case "secondary": return Color.parseColor("#03DAC6");
            case "onSecondary": return Color.parseColor("#000000");
            case "secondaryContainer": return Color.parseColor("#018786");
            case "onSecondaryContainer": return Color.parseColor("#FFFFFF");
            case "tertiary": return Color.parseColor("#FFFFFF");
            case "onTertiary": return Color.parseColor("#000000");
            case "tertiaryContainer": return Color.parseColor("#444444");
            case "onTertiaryContainer": return Color.parseColor("#FFFFFF");
            case "error": return Color.parseColor("#CF6679");
            case "onError": return Color.parseColor("#FFFFFF");
            case "errorContainer": return Color.parseColor("#B00020");
            case "onErrorContainer": return Color.parseColor("#FFFFFF");
            case "success": return Color.parseColor("#00C853");
            case "info": return Color.parseColor("#2196F3");
            case "warning": return Color.parseColor("#FF9800");
            default: return Color.parseColor("#FFFFFF");
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
            File colorsJsonFile = new File(themeDir, "colors/colors.json");
            
            if (colorsJsonFile.exists()) {
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
    
    private void loadCurrentTheme() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String themeName = prefs.getString(PREF_CURRENT_THEME, DEFAULT_THEME);
        
        Log.d(TAG, "Loading current theme: " + themeName);
        
        if (!loadTheme(themeName)) {
            // Fallback to default theme
            Log.w(TAG, "Failed to load theme " + themeName + ", falling back to default");
            if (!loadTheme(DEFAULT_THEME)) {
                Log.e(TAG, "Failed to load default theme, using hardcoded fallbacks");
            }
        }
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
}