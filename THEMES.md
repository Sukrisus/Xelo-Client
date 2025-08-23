# Xelo Client Theme System

## What is it?
The Xelo Client uses a dynamic theme system that loads colors from JSON files instead of compiled XML resources. This allows for runtime theme switching and easy customization.

## Built-in Themes
- **Default** - Material Design 3 dark theme with white primary colors
- **Dark Blue** - Cool blue-tinted dark theme  
- **Purple Dream** - Elegant purple-tinted dark theme

## Creating Custom Themes

### 1. Create a .xtheme file
.xtheme files are ZIP archives with this structure:
```
MyCoolTheme.xtheme
└── colors/
    └── colors.json
```

### 2. colors.json format
```json
{
  "name": "My Cool Theme",
  "author": "Your Name", 
  "description": "A cool custom theme",
  "colors": {
    "background": "#0A0A0A",
    "onBackground": "#FFFFFF",
    "surface": "#141414",
    "onSurface": "#FFFFFF",
    "surfaceVariant": "#1F1F1F",
    "onSurfaceVariant": "#CCCCCC",
    "outline": "#505050",
    "primary": "#FFFFFF",
    "onPrimary": "#000000",
    "primaryContainer": "#1F1F1F",
    "onPrimaryContainer": "#FFFFFF",
    "secondary": "#FFFFFF", 
    "onSecondary": "#000000",
    "secondaryContainer": "#2A2A2A",
    "onSecondaryContainer": "#FFFFFF",
    "tertiary": "#F5F5F5",
    "onTertiary": "#000000",
    "tertiaryContainer": "#3A3A3A",
    "onTertiaryContainer": "#FFFFFF",
    "error": "#FF6659",
    "onError": "#FFFFFF",
    "errorContainer": "#B00020",
    "onErrorContainer": "#FFFFFF",
    "success": "#00E676",
    "info": "#64B5F6",
    "warning": "#FFC107"
  }
}
```

### 3. Install the theme
1. Open Xelo Client
2. Go to Themes section
3. Tap "New Theme" button
4. Select your .xtheme file
5. Theme appears in the list instantly

## How it works
- Themes are automatically applied to all UI elements
- Switch between themes instantly 
- Custom themes can be deleted, built-in themes cannot
- Theme choice persists across app restarts
- Uses Material Design 3 color system for proper contrast and accessibility