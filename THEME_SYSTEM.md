# Theme System Documentation

## Overview

The Xelo Client now supports dynamic theming using JSON configuration files instead of compiled XML resources. This allows for runtime theme switching and easier customization.

## Architecture

### Core Components

1. **ThemeManager**: Singleton class that handles loading and managing themes
2. **ThemeUtils**: Utility class with helper methods for applying themes to views
3. **Theme JSON Files**: Asset files containing color definitions

### Theme JSON Structure

```json
{
  "name": "Theme Name",
  "author": "Author Name",
  "description": "Theme description",
  "colors": {
    "primary": "#00FFCC",
    "primaryDark": "#008877",
    "accent": "#FF0077",
    "background": "#121212",
    "surface": "#1E1E1E",
    "onBackground": "#FFFFFF",
    "onSurface": "#FFFFFF",
    "onSurfaceVariant": "#BBBBBB",
    "onPrimary": "#000000",
    "outline": "#444444",
    "error": "#FF5555"
  }
}
```

## Usage

### Initializing the Theme System

```java
// In your Activity's onCreate()
ThemeManager.getInstance(this);
```

### Applying Themes

```java
// Apply theme to root view
ThemeUtils.applyThemeToRootView(findViewById(android.R.id.content));

// Apply to specific views
ThemeUtils.applyThemeToButton(button, context);
ThemeUtils.applyThemeToCard(cardView, context);
ThemeUtils.applyThemeToTextView(textView, "onSurface");
```

### Loading a Different Theme

```java
ThemeManager themeManager = ThemeManager.getInstance();
boolean success = themeManager.loadTheme("dark_blue");
if (success) {
    // Refresh your views
    applyCurrentTheme();
}
```

### Getting Theme Colors

```java
ThemeManager themeManager = ThemeManager.getInstance();
int primaryColor = themeManager.getColor("primary");
int backgroundColor = themeManager.getColor("background");
```

## Available Themes

- **default**: Default dark theme with cyan accents
- **dark_blue**: Cool dark blue theme
- **purple**: Elegant purple theme

## Creating Custom Themes

1. Create a new JSON file in `app/src/main/assets/themes/`
2. Follow the JSON structure above
3. Use hex color codes for all color values
4. The theme will automatically appear in the themes list

## Color Naming Convention

- **primary**: Main brand color
- **primaryDark**: Darker variant of primary
- **accent**: Accent color for highlights
- **background**: Main background color
- **surface**: Card/surface background
- **onBackground**: Text on background
- **onSurface**: Text on surface
- **onSurfaceVariant**: Secondary text
- **onPrimary**: Text on primary color
- **outline**: Border/outline color
- **error**: Error state color

## Best Practices

1. Always initialize ThemeManager in your Application or main Activity
2. Apply themes in `onResume()` to handle theme changes
3. Use ThemeUtils helper methods instead of hardcoded colors
4. Test themes in both light and dark environments
5. Ensure sufficient contrast for accessibility

## Migration from XML Colors

Replace hardcoded color references:

```java
// Old way
getResources().getColor(R.color.primary, null)

// New way
ThemeManager.getInstance().getColor("primary")
```

## Performance Notes

- Themes are cached after first load
- JSON parsing happens only once per theme
- Color lookups are O(1) HashMap operations
- Minimal overhead for theme switching