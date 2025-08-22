package com.origin.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ThemesFragment extends Fragment {
    private static final String TAG = "ThemesFragment";
    private static final String PREF_SELECTED_THEME = "selected_theme";
    private static final String DEFAULT_THEME = "default";
    
    private ImageView backButton;
    private LinearLayout themesContainer;
    private LinearLayout noThemesContainer;
    private TextView noThemesText;
    private MaterialButton importThemeFab;
    private List<ThemeItem> themesList;
    private String selectedTheme;
    private File themesDirectory;
    
    private ActivityResultLauncher<Intent> filePickerLauncher;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize file picker launcher
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importThemeFile(uri);
                    }
                }
            }
        );
        
        // Initialize themes directory
        File dataDir = new File(requireContext().getExternalFilesDir(null), "themes");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        themesDirectory = dataDir;
        
        // Initialize ThemeManager and get current theme
        ThemeManager themeManager = ThemeManager.getInstance(requireContext());
        selectedTheme = themeManager.getCurrentThemeName();
        if (selectedTheme == null) {
            selectedTheme = DEFAULT_THEME;
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_themes, container, false);
        
        backButton = view.findViewById(R.id.back_button);
        themesContainer = view.findViewById(R.id.themes_container);
        noThemesContainer = view.findViewById(R.id.no_themes_container);
        noThemesText = view.findViewById(R.id.no_themes_text);
        importThemeFab = view.findViewById(R.id.new_theme_button);
        
        // Set up back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        // Set up FAB
        importThemeFab.setOnClickListener(v -> openFilePicker());
        
        loadThemes();
        
        // Apply current theme
        applyCurrentTheme();
        
        return view;
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // Add extra filter for .xtheme files
        String[] mimeTypes = {"application/octet-stream", "*/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Theme File"));
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker", e);
            Toast.makeText(getContext(), "Unable to open file picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void importThemeFile(Uri uri) {
        try {
            String fileName = getFileName(uri);
            if (fileName == null || !fileName.endsWith(".xtheme")) {
                Toast.makeText(getContext(), "Please select a valid .xtheme file", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Copy file to themes directory
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            File destinationFile = new File(themesDirectory, fileName);
            
            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
            Toast.makeText(getContext(), "Theme imported: " + fileName, Toast.LENGTH_SHORT).show();
            loadThemes(); // Refresh the list
            
        } catch (Exception e) {
            Log.e(TAG, "Error importing theme file", e);
            Toast.makeText(getContext(), "Error importing theme: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    
    private void loadThemes() {
        themesList = new ArrayList<>();
        
        // Initialize ThemeManager
        ThemeManager themeManager = ThemeManager.getInstance(requireContext());
        
        // Load built-in themes from assets
        String[] availableThemes = themeManager.getAvailableThemes();
        for (String themeName : availableThemes) {
            ThemeManager.ThemeMetadata metadata = themeManager.getThemeMetadata(themeName);
            themesList.add(new ThemeItem(metadata.name, metadata.description, metadata.key, false, metadata.author));
        }
        
        // Load custom themes from directory
        if (themesDirectory.exists() && themesDirectory.isDirectory()) {
            File[] themeFiles = themesDirectory.listFiles((dir, name) -> name.endsWith(".xtheme"));
            if (themeFiles != null) {
                for (File themeFile : themeFiles) {
                    String themeName = themeFile.getName().replace(".xtheme", "");
                    String themeKey = themeFile.getName();
                    themesList.add(new ThemeItem(themeName, "Custom theme", themeKey, false, null));
                }
            }
        }
        
        displayThemes();
    }
    
    private void displayThemes() {
        themesContainer.removeAllViews();
        
        // Check if we only have the default theme
        boolean hasCustomThemes = themesList.size() > 1;
        
        if (!hasCustomThemes) {
            noThemesContainer.setVisibility(View.VISIBLE);
            // Still show the default theme
            for (ThemeItem theme : themesList) {
                createThemeCard(theme, 0);
            }
        } else {
            noThemesContainer.setVisibility(View.GONE);
            for (int i = 0; i < themesList.size(); i++) {
                ThemeItem theme = themesList.get(i);
                createThemeCard(theme, i);
                
                // Add spacing between cards
                if (i < themesList.size() - 1) {
                    View spacer = new View(getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 
                        (int) (12 * getResources().getDisplayMetrics().density)
                    );
                    spacer.setLayoutParams(params);
                    themesContainer.addView(spacer);
                }
            }
        }
    }
    
    private void createThemeCard(ThemeItem theme, int position) {
    // Create card layout
    MaterialCardView card = new MaterialCardView(requireContext());
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    card.setLayoutParams(cardParams);
    card.setRadius(12 * getResources().getDisplayMetrics().density);
    card.setCardElevation(0); // Remove elevation for flat design
    card.setClickable(true);
    card.setFocusable(true);
    
    // Apply theme colors to card
    ThemeUtils.applyThemeToCard(card, requireContext());
    card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
    
    // Main container
    LinearLayout mainLayout = new LinearLayout(requireContext());
    mainLayout.setOrientation(LinearLayout.HORIZONTAL);
    mainLayout.setPadding(
        (int) (16 * getResources().getDisplayMetrics().density),
        (int) (16 * getResources().getDisplayMetrics().density),
        (int) (16 * getResources().getDisplayMetrics().density),
        (int) (16 * getResources().getDisplayMetrics().density)
    );
    mainLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
    
    // Text container (moved before radio button for better layout)
    LinearLayout textLayout = new LinearLayout(requireContext());
    textLayout.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
        0, 
        LinearLayout.LayoutParams.WRAP_CONTENT, 
        1.0f
    );
    textLayout.setLayoutParams(textParams);
    
    // Theme name
    TextView nameText = new TextView(requireContext());
    nameText.setText(theme.name);
    nameText.setTextSize(16);
    nameText.setTypeface(null, android.graphics.Typeface.BOLD);
    ThemeUtils.applyThemeToTextView(nameText, "onSurface");
    
    // Author text (if available)
    if (theme.author != null && !theme.author.isEmpty()) {
        TextView authorText = new TextView(requireContext());
        authorText.setText("by " + theme.author);
        authorText.setTextSize(14);
        ThemeUtils.applyThemeToTextView(authorText, "onSurfaceVariant");
        LinearLayout.LayoutParams authorParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        authorParams.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
        authorText.setLayoutParams(authorParams);
        textLayout.addView(authorText);
    }
    
    // Theme description
    TextView descText = new TextView(requireContext());
    descText.setText(theme.description);
    descText.setTextSize(14);
    ThemeUtils.applyThemeToTextView(descText, "onSurfaceVariant");
    LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, 
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    descParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
    descText.setLayoutParams(descParams);
    
    textLayout.addView(nameText);
    textLayout.addView(descText);
    
    // Right side container for buttons
    LinearLayout rightContainer = new LinearLayout(requireContext());
    rightContainer.setOrientation(LinearLayout.HORIZONTAL);
    rightContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    rightParams.setMarginStart((int) (16 * getResources().getDisplayMetrics().density));
    rightContainer.setLayoutParams(rightParams);
    
    // Info button (optional - you can add this if needed)
    ImageView infoButton = new ImageView(requireContext());
    LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
        (int) (24 * getResources().getDisplayMetrics().density),
        (int) (24 * getResources().getDisplayMetrics().density)
    );
    infoParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
    infoButton.setLayoutParams(infoParams);
    infoButton.setImageResource(android.R.drawable.ic_dialog_info); // You can replace with your own icon
    infoButton.setColorFilter(ThemeManager.getInstance().getColor("onSurfaceVariant"));
    
    // Create circle ripple background for info button
    android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
    circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
    circle.setColor(android.graphics.Color.TRANSPARENT);
    
    infoButton.setBackground(ThemeUtils.createCircularRipple("onSurfaceVariant"));
    infoButton.setClickable(true);
    infoButton.setFocusable(true);
    
    // Radio button / Selection indicator
    MaterialRadioButton radioButton = new MaterialRadioButton(requireContext());
    LinearLayout.LayoutParams radioParams = new LinearLayout.LayoutParams(
        (int) (24 * getResources().getDisplayMetrics().density),
        (int) (24 * getResources().getDisplayMetrics().density)
    );
    radioButton.setLayoutParams(radioParams);
    radioButton.setChecked(theme.key.equals(selectedTheme));
    radioButton.setClickable(false);
    radioButton.setFocusable(false);
    
    // Apply theme colors to radio button
    ThemeUtils.applyThemeToRadioButton(radioButton, requireContext());
    
    rightContainer.addView(infoButton);
    rightContainer.addView(radioButton);
    
    // Delete button (if not default theme)
    if (!theme.isDefault) {
        ImageView deleteButton = new ImageView(requireContext());
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density)
        );
        deleteParams.setMarginStart((int) (8 * getResources().getDisplayMetrics().density));
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete); // You can replace with your own icon
        deleteButton.setColorFilter(ThemeManager.getInstance().getColor("error"));
        
        // Create circle ripple background for delete button
        android.graphics.drawable.GradientDrawable deleteCircle = new android.graphics.drawable.GradientDrawable();
        deleteCircle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        deleteCircle.setColor(android.graphics.Color.TRANSPARENT);
        
        deleteButton.setBackground(ThemeUtils.createCircularRipple("error"));
        deleteButton.setClickable(true);
        deleteButton.setFocusable(true);
        deleteButton.setContentDescription("Delete theme");
        deleteButton.setOnClickListener(v -> showDeleteConfirmation(theme, position));
        
        rightContainer.addView(deleteButton);
    }
    
    mainLayout.addView(textLayout);
    mainLayout.addView(rightContainer);
    
    card.addView(mainLayout);
    
    // Set card click listener with ripple effect
    card.setOnClickListener(v -> {
        if (!theme.key.equals(selectedTheme)) {
            selectedTheme = theme.key;
            
            // Apply theme using ThemeManager
            ThemeManager themeManager = ThemeManager.getInstance();
            boolean success = themeManager.loadTheme(theme.key);
            
            if (success) {
                displayThemes(); // Refresh to update radio buttons
                Toast.makeText(getContext(), "Theme applied: " + theme.name, Toast.LENGTH_SHORT).show();
                
                // Refresh the current view with new theme
                applyCurrentTheme();
            } else {
                Toast.makeText(getContext(), "Failed to apply theme", Toast.LENGTH_SHORT).show();
            }
        }
    });
    
    // Ripple effect is already applied by ThemeUtils.applyThemeToCard()
    
    themesContainer.addView(card);
}
    
    private void showDeleteConfirmation(ThemeItem theme, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Theme")
            .setMessage("Are you sure you want to delete \"" + theme.name + "\"? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteTheme(theme, position);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteTheme(ThemeItem theme, int position) {
        try {
            File themeFile = new File(themesDirectory, theme.key);
            if (themeFile.exists() && themeFile.delete()) {
                // If this was the selected theme, revert to default
                if (theme.key.equals(selectedTheme)) {
                    selectedTheme = DEFAULT_THEME;
                    ThemeManager.getInstance().loadTheme(DEFAULT_THEME);
                    applyCurrentTheme();
                }
                
                themesList.remove(position);
                displayThemes();
                Toast.makeText(getContext(), "Theme deleted: " + theme.name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to delete theme file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting theme", e);
            Toast.makeText(getContext(), "Error deleting theme: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void applyCurrentTheme() {
        View rootView = getView();
        if (rootView != null) {
            // Apply theme to root view
            ThemeUtils.applyThemeToRootView(rootView);
            
            // Apply theme to the import button
            ThemeUtils.applyThemeToButton(importThemeFab, requireContext());
            
            // Refresh the themes display to apply theme colors
            displayThemes();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Update Discord RPC when fragment resumes
        DiscordRPCHelper.getInstance().updateMenuPresence("Themes");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Update Discord RPC when leaving themes
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
    
    // Theme item class
    private static class ThemeItem {
        String name;
        String description;
        String key;
        boolean isDefault;
        String author;
        
        ThemeItem(String name, String description, String key, boolean isDefault, String author) {
            this.name = name;
            this.description = description;
            this.key = key;
            this.isDefault = isDefault;
            this.author = author;
        }
    }
}