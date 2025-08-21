package com.origin.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ThemesFragment extends Fragment {
    private static final String TAG = "ThemesFragment";
    private static final String PREF_SELECTED_THEME = "selected_theme";
    private static final String DEFAULT_THEME = "default";
    
    private LinearLayout themesContainer;
    private MaterialButton importThemeButton;
    private TextView noThemesText;
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
        
        // Load selected theme preference
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", 0);
        selectedTheme = prefs.getString(PREF_SELECTED_THEME, DEFAULT_THEME);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_themes, container, false);
        
        themesContainer = view.findViewById(R.id.themes_container);
        importThemeButton = view.findViewById(R.id.import_theme_button);
        noThemesText = view.findViewById(R.id.no_themes_text);
        
        importThemeButton.setOnClickListener(v -> openFilePicker());
        
        loadThemes();
        
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
            
            Toast.makeText(getContext(), "Theme imported successfully: " + fileName, Toast.LENGTH_SHORT).show();
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
        
        // Add default theme
        themesList.add(new ThemeItem("Default", "Built-in default theme", DEFAULT_THEME, true));
        
        // Load themes from directory
        if (themesDirectory.exists() && themesDirectory.isDirectory()) {
            File[] themeFiles = themesDirectory.listFiles((dir, name) -> name.endsWith(".xtheme"));
            if (themeFiles != null) {
                for (File themeFile : themeFiles) {
                    String themeName = themeFile.getName().replace(".xtheme", "");
                    String themeKey = themeFile.getName();
                    themesList.add(new ThemeItem(themeName, "Custom theme", themeKey, false));
                }
            }
        }
        
        displayThemes();
    }
    
    private void displayThemes() {
        themesContainer.removeAllViews();
        
        if (themesList.isEmpty() || (themesList.size() == 1 && themesList.get(0).isDefault)) {
            noThemesText.setVisibility(View.VISIBLE);
        } else {
            noThemesText.setVisibility(View.GONE);
        }
        
        for (int i = 0; i < themesList.size(); i++) {
            ThemeItem theme = themesList.get(i);
            createThemeCard(theme, i);
        }
    }
    
    private void createThemeCard(ThemeItem theme, int position) {
        View themeCard = LayoutInflater.from(getContext()).inflate(R.layout.item_theme, themesContainer, false);
        
        MaterialCardView cardView = themeCard.findViewById(R.id.theme_card);
        TextView themeName = themeCard.findViewById(R.id.theme_name);
        TextView themeDescription = themeCard.findViewById(R.id.theme_description);
        MaterialRadioButton radioButton = themeCard.findViewById(R.id.theme_radio_button);
        MaterialButton deleteButton = themeCard.findViewById(R.id.delete_theme_button);
        
        themeName.setText(theme.name);
        themeDescription.setText(theme.description);
        radioButton.setChecked(theme.key.equals(selectedTheme));
        
        // Hide delete button for default theme
        if (theme.isDefault) {
            deleteButton.setVisibility(View.GONE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> showDeleteConfirmation(theme, position));
        }
        
        // Set up card click listener
        View.OnClickListener selectTheme = v -> {
            if (!theme.key.equals(selectedTheme)) {
                selectedTheme = theme.key;
                saveSelectedTheme();
                displayThemes(); // Refresh to update radio buttons
                Toast.makeText(getContext(), "Theme applied: " + theme.name, Toast.LENGTH_SHORT).show();
            }
        };
        
        cardView.setOnClickListener(selectTheme);
        radioButton.setOnClickListener(selectTheme);
        
        themesContainer.addView(themeCard);
        
        // Add margin between cards
        if (position < themesList.size() - 1) {
            View spacer = new View(getContext());
            spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                (int) (8 * getResources().getDisplayMetrics().density)
            ));
            themesContainer.addView(spacer);
        }
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
                    saveSelectedTheme();
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
    
    private void saveSelectedTheme() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", 0);
        prefs.edit().putString(PREF_SELECTED_THEME, selectedTheme).apply();
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
        
        ThemeItem(String name, String description, String key, boolean isDefault) {
            this.name = name;
            this.description = description;
            this.key = key;
            this.isDefault = isDefault;
        }
    }
}