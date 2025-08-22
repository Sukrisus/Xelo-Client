# .xtheme File Format

## Overview

`.xtheme` files are ZIP archives with a specific internal structure that contain theme data for the Xelo Client.

## File Structure

```
MyCoolTheme.xtheme (ZIP file)
└── colors/
    └── colors.json
```

## colors.json Format

The `colors/colors.json` file should contain theme metadata and color definitions:

```json
{
  "name": "My Cool Theme",
  "author": "Your Name",
  "description": "A cool custom theme",
  "colors": {
    "primary": "#FF6B35",
    "primaryDark": "#E55A2B",
    "accent": "#4ECDC4",
    "background": "#2C3E50",
    "surface": "#34495E",
    "onBackground": "#ECF0F1",
    "onSurface": "#FFFFFF",
    "onSurfaceVariant": "#BDC3C7",
    "onPrimary": "#FFFFFF",
    "outline": "#7F8C8D",
    "error": "#E74C3C"
  }
}
```

## Color Definitions

- **primary**: Main brand color (used for buttons, selected items)
- **primaryDark**: Darker variant of primary
- **accent**: Accent color for highlights
- **background**: Main background color
- **surface**: Card/surface background color
- **onBackground**: Text color on background
- **onSurface**: Text color on surfaces
- **onSurfaceVariant**: Secondary text color
- **onPrimary**: Text color on primary-colored elements
- **outline**: Border/outline color
- **error**: Error state color

## Creating a .xtheme File

1. Create the folder structure:
   ```
   MyCoolTheme/
   └── colors/
       └── colors.json
   ```

2. Create your `colors.json` with the format above

3. ZIP the `MyCoolTheme` folder

4. Rename the ZIP file to `MyCoolTheme.xtheme`

## Installation

1. Open Xelo Client
2. Go to Themes section
3. Tap the "New Theme" button
4. Select your `.xtheme` file
5. The theme will be extracted and appear in your themes list

## Usage

- Once imported, `.xtheme` themes work exactly like built-in themes
- You can switch between them instantly
- Custom themes can be deleted (built-in themes cannot)
- Theme preferences are saved and restored on app restart

## Technical Notes

- `.xtheme` files are extracted to `Android/data/com.origin.launcher/files/themes/`
- Each theme gets its own folder named after the theme key
- The system looks for `colors/colors.json` inside each theme folder
- Invalid `.xtheme` files are rejected during import