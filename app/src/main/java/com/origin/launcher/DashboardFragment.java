package com.origin.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Color;

public class DashboardFragment extends BaseThemedFragment {
    private File currentRootDir = null; // Store the found root directory
    private static final int IMPORT_REQUEST_CODE = 1002;
    private static final int EXPORT_REQUEST_CODE = 1003;
    private static final int IMPORT_CONFIG_REQUEST_CODE = 1004;
    private static final int EXPORT_CONFIG_REQUEST_CODE = 1005;
    
    // Options.txt editor variables
    private File optionsFile;
    private String originalOptionsContent = "";
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private EditText optionsTextEditor;
    private LinearLayout optionsEditorLayout;
    private TextInputLayout searchInputLayout;
    private TextInputEditText searchEditText;
    private MaterialButton editOptionsButton;
    
    // Search functionality variables
    private String currentSearchTerm = "";
    private List<Integer> searchMatches = new ArrayList<>();
    private int currentMatchIndex = -1;
    
    // Modules variables - UPDATED FOR SCROLLVIEW
    private File configFile;
    private ScrollView modulesScrollView;
    private LinearLayout modulesContainer;
    private List<ModuleItem> moduleItems;
    
    // Module data class
    private static class ModuleItem {
        private String name;
        private String description;
        private String configKey;
        private boolean enabled;
        
        public ModuleItem(String name, String description, String configKey) {
            this.name = name;
            this.description = description;
            this.configKey = configKey;
            this.enabled = false; // Default to disabled
        }
        
        // Getters and setters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getConfigKey() { return configKey; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    // Module toggle listener interface
    private interface ModuleToggleListener {
        void onToggle(ModuleItem module, boolean isEnabled);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        RecyclerView folderRecyclerView = view.findViewById(R.id.folderRecyclerView);
        MaterialButton backupButton = view.findViewById(R.id.backupButton);
        MaterialButton importButton = view.findViewById(R.id.importButton);
        
        if (folderRecyclerView != null) {
            folderRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            // File management root - try multiple possible paths
            String[] possiblePaths = {
                "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/",
                "/storage/emulated/0/games/com.mojang/",
                "/storage/emulated/0/Android/data/com.mojang.minecraftpe/files/games/com.mojang/",
                getContext().getExternalFilesDir(null) + "/games/com.mojang/"
            };
            
            File rootDir = null;
            String rootPath = null;
            
            for (String path : possiblePaths) {
                File testDir = new File(path);
                if (testDir.exists() && testDir.isDirectory()) {
                    File[] testFiles = testDir.listFiles();
                    if (testFiles != null && testFiles.length > 0) {
                        rootDir = testDir;
                        rootPath = path;
                        currentRootDir = testDir; // Store for later use
                        break;
                    }
                }
            }
            
            List<String> folderNames = new ArrayList<>();
            if (rootDir != null && rootDir.exists() && rootDir.isDirectory()) {
                File[] files = rootDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            folderNames.add(file.getName());
                        }
                    }
                }
            } else {
                folderNames.add("No Minecraft data found");
            }
            FolderAdapter adapter = new FolderAdapter(folderNames, getContext());
            folderRecyclerView.setAdapter(adapter);
        }

        if (backupButton != null) {
            // Apply theme to backup button
            ThemeUtils.applyThemeToButton(backupButton, requireContext());
            backupButton.setOnClickListener(v -> {
                if (hasStoragePermission()) {
                    if (currentRootDir != null) {
                        openSaveLocationChooser();
                    } else {
                        Toast.makeText(requireContext(), "No Minecraft data found to backup", Toast.LENGTH_LONG).show();
                    }
                } else {
                    requestStoragePermissions();
                }
            });
        }

        if (importButton != null) {
            // Apply theme to import button
            ThemeUtils.applyThemeToButton(importButton, requireContext());
            importButton.setOnClickListener(v -> {
                if (hasStoragePermission()) {
                    openFileChooser();
                } else {
                    requestStoragePermissions();
                }
            });
        }

        // Initialize modules
        initializeModules(view);
        
        // Initialize options.txt editor
        initializeOptionsEditor(view);

        return view;
    }
    
    /**
     * Refresh folder themes when theme changes
     */
    private void refreshFolderThemes() {
        try {
            RecyclerView folderRecyclerView = getView().findViewById(R.id.folderRecyclerView);
            if (folderRecyclerView != null && folderRecyclerView.getAdapter() != null) {
                // Force refresh of all folder items with animation
                folderRecyclerView.getAdapter().notifyDataSetChanged();
                
                // Apply animated theme changes to existing folder items
                for (int i = 0; i < folderRecyclerView.getChildCount(); i++) {
                    View child = folderRecyclerView.getChildAt(i);
                    if (child instanceof MaterialCardView) {
                        MaterialCardView card = (MaterialCardView) child;
                        
                        // Get current colors
                        int currentBackground = card.getCardBackgroundColor().getDefaultColor();
                        int currentStroke = card.getStrokeColor();
                        
                        // Get target colors
                        int targetBackground = ThemeManager.getInstance().getColor("surfaceVariant");
                        int targetStroke = ThemeManager.getInstance().getColor("outline");
                        
                        // Animate color transitions
                        ThemeUtils.animateBackgroundColorTransition(card, currentBackground, targetBackground, 300);
                        ThemeUtils.animateColorTransition(currentStroke, targetStroke, 300, 
                            va -> {
                                int animated = (int) va.getAnimatedValue();
                                card.setStrokeColor(animated);
                            });
                        
                        // Ensure corner radius is preserved
                        card.setRadius(12 * getResources().getDisplayMetrics().density);
                    }
                }
            }
        } catch (Exception e) {
            // Handle error gracefully
        }
    }
    
    /**
     * Refresh toggle themes when theme changes
     */
    private void refreshToggleThemes() {
        try {
            if (modulesContainer != null) {
                for (int i = 0; i < modulesContainer.getChildCount(); i++) {
                    View child = modulesContainer.getChildAt(i);
                    if (child instanceof MaterialCardView) {
                        // Find MaterialSwitch in the card and refresh its theme with animation
                        refreshSwitchThemeInCardWithAnimation((MaterialCardView) child);
                    }
                }
            }
        } catch (Exception e) {
            // Handle error gracefully
        }
    }
    
    /**
     * Refresh switch theme within a card with animation
     */
    private void refreshSwitchThemeInCardWithAnimation(MaterialCardView card) {
        try {
            // Find MaterialSwitch in the card
            for (int i = 0; i < ((ViewGroup) card.getChildAt(0)).getChildCount(); i++) {
                View child = ((ViewGroup) card.getChildAt(0)).getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout layout = (LinearLayout) child;
                    for (int j = 0; j < layout.getChildCount(); j++) {
                        View layoutChild = layout.getChildAt(j);
                        if (layoutChild instanceof LinearLayout) {
                            LinearLayout rightContainer = (LinearLayout) layoutChild;
                            for (int k = 0; k < rightContainer.getChildCount(); k++) {
                                View rightChild = rightContainer.getChildAt(k);
                                if (rightChild instanceof MaterialSwitch) {
                                    MaterialSwitch materialSwitch = (MaterialSwitch) rightChild;
                                    
                                    // Get current colors
                                    ColorStateList currentTrackColors = materialSwitch.getTrackTintList();
                                    ColorStateList currentThumbColors = materialSwitch.getThumbTintList();
                                    
                                    // Apply new theme
                                    ThemeUtils.applyThemeToSwitch(materialSwitch, requireContext());
                                    
                                    // Get new colors
                                    ColorStateList newTrackColors = materialSwitch.getTrackTintList();
                                    ColorStateList newThumbColors = materialSwitch.getThumbTintList();
                                    
                                                            // Animate color transitions if colors changed
                        if (currentTrackColors != null && newTrackColors != null) {
                            int currentTrack = currentTrackColors.getDefaultColor();
                            int newTrack = newTrackColors.getDefaultColor();
                            if (currentTrack != newTrack) {
                                ThemeUtils.animateColorTransition(currentTrack, newTrack, 300, 
                                    va -> materialSwitch.setTrackTintList(ColorStateList.valueOf((int) va.getAnimatedValue())));
                            }
                        }
                        
                        if (currentThumbColors != null && newThumbColors != null) {
                            int currentThumb = currentThumbColors.getDefaultColor();
                            int newThumb = newThumbColors.getDefaultColor();
                            if (currentThumb != newThumb) {
                                ThemeUtils.animateColorTransition(currentThumb, newThumb, 300, 
                                    va -> materialSwitch.setThumbTintList(ColorStateList.valueOf((int) va.getAnimatedValue())));
                            }
                        }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Handle error gracefully
        }
    }
    
    @Override
    protected void onApplyTheme() {
        // Apply theme to the root view background with animation
        View rootView = getView();
        if (rootView != null) {
            try {
                // Get current background color safely
                int currentBackground = Color.parseColor("#0A0A0A"); // Default fallback
                if (rootView.getBackground() != null) {
                    try {
                        currentBackground = ((android.graphics.drawable.ColorDrawable) rootView.getBackground()).getColor();
                    } catch (Exception e) {
                        // Use default if we can't get current color
                    }
                }
                
                int targetBackground = ThemeManager.getInstance().getColor("background");
                
                // Animate background color transition
                ThemeUtils.animateBackgroundColorTransition(rootView, currentBackground, targetBackground, 300);
            } catch (Exception e) {
                // Fallback to immediate theme application
                rootView.setBackgroundColor(ThemeManager.getInstance().getColor("background"));
            }
        }
        
        // Apply theme to ScrollView and modules container background
        refreshScrollViewBackground();
        
        // Refresh button themes with animation
        refreshButtonThemesWithAnimation();
        
        // Refresh all ripple effects in the entire view hierarchy
        if (rootView != null) {
            ThemeUtils.refreshRippleEffects(rootView);
        }
        
        // Refresh module card backgrounds to ensure they remain visible
        refreshModuleCardBackgroundsWithAnimation();
        
        // Refresh folder themes
        refreshFolderThemes();
        
        // Refresh toggle themes
        refreshToggleThemes();
    }
    
    /**
     * Refresh button themes with animation when theme changes
     */
    private void refreshButtonThemesWithAnimation() {
        try {
            View view = getView();
            if (view != null) {
                // Refresh backup and import buttons with animation
                MaterialButton backupButton = view.findViewById(R.id.backupButton);
                MaterialButton importButton = view.findViewById(R.id.importButton);
                
                if (backupButton != null) {
                    ThemeUtils.applyThemeToButtonWithAnimation(backupButton, requireContext(), 300);
                }
                if (importButton != null) {
                    ThemeUtils.applyThemeToButtonWithAnimation(importButton, requireContext(), 300);
                }
                
                // Refresh config buttons with animation
                MaterialButton exportConfigButton = view.findViewById(R.id.exportConfigButton);
                MaterialButton importConfigButton = view.findViewById(R.id.importConfigButton);
                
                if (exportConfigButton != null) {
                    ThemeUtils.applyThemeToButtonWithAnimation(exportConfigButton, requireContext(), 300);
                }
                if (importConfigButton != null) {
                    ThemeUtils.applyThemeToButtonWithAnimation(importConfigButton, requireContext(), 300);
                }
                
                // Refresh module card ripple effects
                refreshModuleCardRipples();
            }
        } catch (Exception e) {
            // Handle error gracefully
        }
    }
    
    /**
     * Refresh ripple effects on module cards
     */
    private void refreshModuleCardRipples() {
        try {
            if (modulesContainer != null) {
                // Use the new ThemeUtils method for consistent ripple refresh
                ThemeUtils.refreshRippleEffects(modulesContainer);
                
                // Also refresh the card backgrounds to ensure they remain visible
                refreshModuleCardBackgroundsWithAnimation();
                
                // Refresh toggle themes in the cards
                refreshToggleThemes();
            }
        } catch (Exception e) {
            // Handle error gracefully
        }
    }
    
    /**
     * Refresh module card backgrounds with animation to ensure they remain visible
     */
    private void refreshModuleCardBackgroundsWithAnimation() {
        try {
            if (modulesContainer != null) {
                for (int i = 0; i < modulesContainer.getChildCount(); i++) {
                    View child = modulesContainer.getChildAt(i);
                    if (child instanceof MaterialCardView) {
                        MaterialCardView card = (MaterialCardView) child;
                        
                        // Get current background color
                        int currentBackground = card.getCardBackgroundColor().getDefaultColor();
                        int targetBackground = ThemeManager.getInstance().getColor("surfaceVariant");
                        
                        // Animate background color transition
                        ThemeUtils.animateBackgroundColorTransition(card, currentBackground, targetBackground, 300);
                        
                        // Keep subtle elevation, no border, and 12dp radius
                        card.setCardElevation(2 * getResources().getDisplayMetrics().density);
                        card.setStrokeWidth(0);
                        card.setRadius(12 * getResources().getDisplayMetrics().density);
                        card.setPreventCornerOverlap(false);
                    }
                }
            }
        } catch (Exception e) {
            // Handle error gracefully
        }
    }

    private void initializeModules(View view) {
        // Initialize config file path
        configFile = new File(getContext().getExternalFilesDir(null), "origin_mods/config.json");
        
        // Get ScrollView and container - UPDATED
        modulesScrollView = view.findViewById(R.id.modulesScrollView);
        modulesContainer = view.findViewById(R.id.modulesContainer);
        
        // Apply theme background to ScrollView and container
        refreshScrollViewBackground();
        
        if (modulesContainer != null) {
            // Initialize module items
            moduleItems = new ArrayList<>();
            moduleItems.add(new ModuleItem("no hurt cam", "allows you to toggle the in-game hurt cam", "Nohurtcam"));
            moduleItems.add(new ModuleItem("Fullbright", "(Doesnt work with No fog) ofcouse lets u see in the dark moron", "night_vision"));
            moduleItems.add(new ModuleItem("No Fog", "(Doesnt work with fullbright) allows you to toggle the in-game fog", "Nofog"));
            moduleItems.add(new ModuleItem("Particles Disabler", "allows you to toggle the in-game particles", "particles_disabler"));
            moduleItems.add(new ModuleItem("Java Fancy Clouds", "Changes the clouds to Java Fancy Clouds", "java_clouds"));
            moduleItems.add(new ModuleItem("Java Cubemap", "improves the in-game cubemap bringing it abit lower", "java_cubemap"));
            moduleItems.add(new ModuleItem("Classic Vanilla skins", "Disables the newly added skins by mojang", "classic_skins"));
            moduleItems.add(new ModuleItem("No flipbook animation", "optimizes your fps by disabling block animation", "no_flipbook_animations"));
            moduleItems.add(new ModuleItem("No Shadows", "optimizes your fps by disabling shadows", "no_shadows"));
            moduleItems.add(new ModuleItem("Xelo Title", "Changes the Start screen title image", "xelo_title"));
            moduleItems.add(new ModuleItem("White Block Outline", "changes the block selection outline to white", "white_block_outline"));
            
            // Load current config state and populate modules
            loadModuleStates();
            populateModules();
        }
        
        // Set up the existing XML config buttons
        setupConfigButtons(view);
    }
    
    // NEW METHOD: Populate modules in ScrollView
    private void populateModules() {
        if (modulesContainer == null) return;
        
        // Clear existing modules
        modulesContainer.removeAllViews();
        
        // Add each module as a card view
        for (ModuleItem module : moduleItems) {
            View moduleView = createModuleView(module);
            modulesContainer.addView(moduleView);
        }
    }
    
    private View createModuleView(ModuleItem module) {
        // Create card layout (matching theme card design)
        MaterialCardView moduleCard = new MaterialCardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(
            (int) (8 * getResources().getDisplayMetrics().density),  // Reduced from 16 to 8
            (int) (8 * getResources().getDisplayMetrics().density),
            (int) (8 * getResources().getDisplayMetrics().density),  // Reduced from 16 to 8
            (int) (8 * getResources().getDisplayMetrics().density)
        );
        moduleCard.setLayoutParams(cardParams);
        
        // FIXED: Set corner radius ONLY ONCE and at the correct value
        float cornerRadius = 12 * getResources().getDisplayMetrics().density;
        moduleCard.setRadius(cornerRadius);
        
        // FIXED: Set card properties in the correct order
        moduleCard.setCardBackgroundColor(ThemeManager.getInstance().getColor("surfaceVariant"));
        moduleCard.setCardElevation(2 * getResources().getDisplayMetrics().density); // Small elevation like file cards
        moduleCard.setStrokeWidth(0); // No border
        
        // FIXED: Remove conflicting settings
        moduleCard.setClickable(true);
        moduleCard.setFocusable(true);
        moduleCard.setPreventCornerOverlap(false); // This can interfere with corner radius
        
        // Main container (horizontal layout like themes)
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),  // Reduced from 20 to 16
            (int) (12 * getResources().getDisplayMetrics().density),  // Reduced from 16 to 12
            (int) (16 * getResources().getDisplayMetrics().density),  // Reduced from 20 to 16
            (int) (12 * getResources().getDisplayMetrics().density)   // Reduced from 16 to 12
        );
        mainLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        // Left side: Icon
        ImageView iconView = new ImageView(getContext());
        iconView.setImageResource(R.drawable.wrench);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            (int) (24 * getResources().getDisplayMetrics().density),  // Back to 24dp for better visibility
            (int) (24 * getResources().getDisplayMetrics().density)   // Back to 24dp for better visibility
        );
        iconParams.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));  // Back to 16dp
        iconView.setLayoutParams(iconParams);
        iconView.setColorFilter(ThemeManager.getInstance().getColor("onSurface"));
        
        // Text container (matching themes layout)
        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            1.0f
        );
        textParams.setMarginStart((int) (8 * getResources().getDisplayMetrics().density));  // Back to 8dp
        textLayout.setLayoutParams(textParams);
        
        // Module name (matching theme name styling)
        TextView moduleNameText = new TextView(getContext());
        moduleNameText.setText(module.getName());
        moduleNameText.setTextSize(16);  // Back to 16sp
        moduleNameText.setTypeface(null, Typeface.BOLD);
        ThemeUtils.applyThemeToTextView(moduleNameText, "onSurface");
        
        // Module description (matching theme description styling)
        TextView moduleDescriptionText = new TextView(getContext());
        moduleDescriptionText.setText(module.getDescription());
        moduleDescriptionText.setTextSize(14);  // Back to 14sp
        ThemeUtils.applyThemeToTextView(moduleDescriptionText, "onSurfaceVariant");
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = (int) (6 * getResources().getDisplayMetrics().density);  // Reduced from 8 to 6
        moduleDescriptionText.setLayoutParams(descParams);
        moduleDescriptionText.setMaxLines(2);
        moduleDescriptionText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        textLayout.addView(moduleNameText);
        textLayout.addView(moduleDescriptionText);
        
        // Right side container for switch (matching theme card right container)
        LinearLayout rightContainer = new LinearLayout(getContext());
        rightContainer.setOrientation(LinearLayout.HORIZONTAL);
        rightContainer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rightParams.setMarginStart((int) (16 * getResources().getDisplayMetrics().density));  // Back to 16dp
        rightContainer.setLayoutParams(rightParams);
        
        // Module switch (normal size - not scaled down)
        MaterialSwitch moduleSwitch = new MaterialSwitch(getContext());
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        // No scaling - use normal size
        moduleSwitch.setLayoutParams(switchParams);
        moduleSwitch.setChecked(module.isEnabled());
        
        // Apply theme to the switch
        ThemeUtils.applyThemeToSwitch(moduleSwitch, requireContext());
        
        moduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            module.setEnabled(isChecked);
            onModuleToggle(module, isChecked);
        });
        
        rightContainer.addView(moduleSwitch);
        
        mainLayout.addView(iconView);
        mainLayout.addView(textLayout);
        mainLayout.addView(rightContainer);
        
        moduleCard.addView(mainLayout);
        
        // FINAL corner radius enforcement
        moduleCard.setRadius(12 * getResources().getDisplayMetrics().density);
        
        return moduleCard;
    }
    
    private void setupConfigButtons(View view) {
        // Find the existing buttons from XML
        MaterialButton exportConfigButton = view.findViewById(R.id.exportConfigButton);
        MaterialButton importConfigButton = view.findViewById(R.id.importConfigButton);
        
        // Apply themes to both buttons
        if (exportConfigButton != null) {
            ThemeUtils.applyThemeToButton(exportConfigButton, requireContext());
            exportConfigButton.setOnClickListener(v -> {
                if (hasStoragePermission()) {
                    exportConfig();
                } else {
                    requestStoragePermissions();
                }
            });
        }
        
        if (importConfigButton != null) {
            ThemeUtils.applyThemeToButton(importConfigButton, requireContext());
            importConfigButton.setOnClickListener(v -> {
                if (hasStoragePermission()) {
                    openConfigFileChooser();
                } else {
                    requestStoragePermissions();
                }
            });
        }
    }
    
    private void exportConfig() {
    try {
        // Check if config file exists
        if (!configFile.exists()) {
            Toast.makeText(requireContext(), "Config file not found. Creating default config first.", Toast.LENGTH_LONG).show();
            createDefaultConfig();
            
            if (!configFile.exists()) {
                Toast.makeText(requireContext(), "Failed to create config file.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        // Always copy to cache directory for sharing
        File cacheDir = requireContext().getCacheDir();
        File tempConfigFile = new File(cacheDir, "config.json");
        
        // Delete existing temp file if it exists
        if (tempConfigFile.exists()) {
            tempConfigFile.delete();
        }
        
        // Copy the actual config file to cache directory
        try (FileInputStream fis = new FileInputStream(configFile);
             FileOutputStream fos = new FileOutputStream(tempConfigFile)) {
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
        }
        
        // Verify the temp file was created and has content
        if (!tempConfigFile.exists() || tempConfigFile.length() == 0) {
            Toast.makeText(requireContext(), "Failed to prepare config file for sharing.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Create file URI for the temp file (using cache path)
        Uri fileUri = FileProvider.getUriForFile(
            requireContext(), 
            "com.origin.launcher.fileprovider", 
            tempConfigFile
        );
        
        // Create share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Xelo Client Config");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Xelo Client configuration file");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        // Verify that there are apps that can handle this intent
        if (shareIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(Intent.createChooser(shareIntent, "Export Config File"));
            Toast.makeText(requireContext(), "Config file ready to share!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "No apps available to share the config file.", Toast.LENGTH_SHORT).show();
        }
        
    } catch (IOException e) {
        Toast.makeText(requireContext(), "Failed to export config: " + e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
    } catch (Exception e) {
        Toast.makeText(requireContext(), "Unexpected error during config export: " + e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
    }
}
    
    private void openConfigFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Config File"), IMPORT_CONFIG_REQUEST_CODE);
    }
    
    private void importConfig(Uri configUri) {
        try {
            // Read the selected config file
            InputStream inputStream = requireContext().getContentResolver().openInputStream(configUri);
            if (inputStream == null) {
                Toast.makeText(requireContext(), "Could not read the selected config file", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Read content
            StringBuilder content = new StringBuilder();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                content.append(new String(buffer, 0, length));
            }
            inputStream.close();
            
            // Validate JSON
            try {
                new JSONObject(content.toString());
            } catch (JSONException e) {
                Toast.makeText(requireContext(), "Invalid config file format", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Ensure config directory exists
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // Write to config file
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(content.toString());
            }
            
            // Reload module states and refresh UI
            loadModuleStates();
            populateModules(); // Refresh the UI
            
            Toast.makeText(requireContext(), "Config imported successfully!", Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to import config: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    private void onModuleToggle(ModuleItem module, boolean isEnabled) {
        updateConfigFile(module.getConfigKey(), isEnabled);
        Toast.makeText(requireContext(), 
            module.getName() + " " + (isEnabled ? "enabled" : "disabled"), 
            Toast.LENGTH_SHORT).show();
    }
    
    private void loadModuleStates() {
        try {
            if (!configFile.exists()) {
                // Create directory if it doesn't exist
                File parentDir = configFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                // Create default config
                createDefaultConfig();
                return;
            }
            
            // Read existing config
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            
            JSONObject config = new JSONObject(content.toString());
            
            // Update module states
            for (ModuleItem module : moduleItems) {
                if (config.has(module.getConfigKey())) {
                    module.setEnabled(config.getBoolean(module.getConfigKey()));
                }
            }
            
        } catch (IOException | JSONException e) {
            Toast.makeText(requireContext(), "Failed to load module config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void createDefaultConfig() {
        try {
            // Create parent directory if it doesn't exist
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    Toast.makeText(requireContext(), "Failed to create config directory", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            JSONObject defaultConfig = new JSONObject();
            defaultConfig.put("Nohurtcam", false);
            defaultConfig.put("Nofog", false);
            defaultConfig.put("particles_disabler", false);
            defaultConfig.put("java_clouds", false);
            defaultConfig.put("java_cubemap", false);
            defaultConfig.put("classic_skins", false);
            defaultConfig.put("white_block_outline", false);
            defaultConfig.put("no_flipbook_animations", false);
            defaultConfig.put("no_shadows", false);
            defaultConfig.put("night_vision", false);
            defaultConfig.put("xelo_title", true);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultConfig.toString(2)); // Pretty print with indent
            }
            
        } catch (IOException | JSONException e) {
            Toast.makeText(requireContext(), "Failed to create default config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void updateConfigFile(String key, boolean value) {
        try {
            JSONObject config;
            
            if (configFile.exists()) {
                // Read existing config
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }
                }
                config = new JSONObject(content.toString());
            } else {
                // Create new config and ensure directory exists
                config = new JSONObject();
                File parentDir = configFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    if (!created) {
                        Toast.makeText(requireContext(), "Failed to create config directory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            
            // Update the specific key
            config.put(key, value);
            
            // Write back to file
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(config.toString(2)); // Pretty print with indent
            }
            
        } catch (IOException | JSONException e) {
            Toast.makeText(requireContext(), "Failed to update config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // REST OF THE CODE REMAINS THE SAME...
    // (All the other methods like openFileChooser, openSaveLocationChooser, etc. remain unchanged)

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Backup Zip File"), IMPORT_REQUEST_CODE);
    }

    private void openSaveLocationChooser() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "mojang_backup.zip");
        startActivityForResult(Intent.createChooser(intent, "Choose where to save backup"), EXPORT_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            Uri zipUri = data.getData();
            if (zipUri != null) {
                importBackup(zipUri);
            }
        } else if (requestCode == EXPORT_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            Uri saveUri = data.getData();
            if (saveUri != null && currentRootDir != null) {
                createBackupAtLocation(saveUri, currentRootDir);
            }
        } else if (requestCode == IMPORT_CONFIG_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            Uri configUri = data.getData();
            if (configUri != null) {
                importConfig(configUri);
            }
        }
    }

    private void importBackup(Uri zipUri) {
        try {
            // Use the specific target directory
            File targetDir = new File("/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/");
            
            // Create directory if it doesn't exist
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    Toast.makeText(requireContext(), "Could not create target directory: " + targetDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    return;
                }
            }
            
            // Check if we can write to the directory
            if (!targetDir.canWrite()) {
                Toast.makeText(requireContext(), "Cannot write to target directory: " + targetDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            
            Toast.makeText(requireContext(), "Importing backup to: " + targetDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            
            InputStream inputStream = requireContext().getContentResolver().openInputStream(zipUri);
            if (inputStream != null) {
                extractZip(inputStream, targetDir);
                
                // Update currentRootDir to the new location
                currentRootDir = targetDir;
                
                Toast.makeText(requireContext(), "Backup imported successfully!", Toast.LENGTH_LONG).show();
                
                // Refresh the folder list
                refreshFolderList();
            } else {
                Toast.makeText(requireContext(), "Could not read the selected file", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void extractZip(InputStream zipInputStream, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry zipEntry;
            byte[] buffer = new byte[4096]; // Increased buffer size
            
            while ((zipEntry = zis.getNextEntry()) != null) {
                String fileName = zipEntry.getName();
                
                // Security check: prevent directory traversal
                if (fileName.contains("..")) {
                    continue;
                }
                
                File newFile = new File(targetDir, fileName);
                
                if (zipEntry.isDirectory()) {
                    // Create directory
                    if (!newFile.exists()) {
                        boolean created = newFile.mkdirs();
                        if (!created) {
                            System.err.println("Failed to create directory: " + newFile.getAbsolutePath());
                        }
                    }
                } else {
                    // Create parent directories if they don't exist
                    File parentDir = newFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        boolean created = parentDir.mkdirs();
                        if (!created) {
                            System.err.println("Failed to create parent directory: " + parentDir.getAbsolutePath());
                            continue;
                        }
                    }
                    
                    // Extract file
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.flush();
                    } catch (IOException e) {
                        System.err.println("Failed to extract file: " + newFile.getAbsolutePath() + " - " + e.getMessage());
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void refreshFolderList() {
        RecyclerView folderRecyclerView = getView().findViewById(R.id.folderRecyclerView);
        if (folderRecyclerView != null) {
            // Re-scan for folders
            String[] possiblePaths = {
                "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/",
                "/storage/emulated/0/games/com.mojang/",
                "/storage/emulated/0/Android/data/com.mojang.minecraftpe/files/games/com.mojang/",
                getContext().getExternalFilesDir(null) + "/games/com.mojang/"
            };
            
            File rootDir = null;
            for (String path : possiblePaths) {
                File testDir = new File(path);
                if (testDir.exists() && testDir.isDirectory()) {
                    File[] testFiles = testDir.listFiles();
                    if (testFiles != null && testFiles.length > 0) {
                        rootDir = testDir;
                        currentRootDir = testDir;
                        break;
                    }
                }
            }
            
            List<String> folderNames = new ArrayList<>();
            if (rootDir != null && rootDir.exists() && rootDir.isDirectory()) {
                File[] files = rootDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            folderNames.add(file.getName());
                        }
                    }
                }
            } else {
                folderNames.add("No Minecraft data found");
            }
            FolderAdapter adapter = new FolderAdapter(folderNames, getContext());
            folderRecyclerView.setAdapter(adapter);
        }
    }

    private void initializeOptionsEditor(View view) {
        // Initialize options.txt file path
        optionsFile = new File("/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/minecraftpe/options.txt");
        
        // Get UI elements
        editOptionsButton = view.findViewById(R.id.editOptionsButton);
        TextView optionsNotFoundText = view.findViewById(R.id.optionsNotFoundText);
        optionsEditorLayout = view.findViewById(R.id.optionsEditorLayout);
        optionsTextEditor = view.findViewById(R.id.optionsTextEditor);
        searchInputLayout = view.findViewById(R.id.searchInputLayout);
        searchEditText = view.findViewById(R.id.searchEditText);
        
        MaterialButton saveOptionsButton = view.findViewById(R.id.saveOptionsButton);
        MaterialButton undoOptionsButton = view.findViewById(R.id.undoOptionsButton);
        MaterialButton redoOptionsButton = view.findViewById(R.id.redoOptionsButton);
        MaterialButton searchOptionsButton = view.findViewById(R.id.searchOptionsButton);
        MaterialButton closeEditorButton = view.findViewById(R.id.closeEditorButton);
        
        // Check if options.txt exists
        if (optionsFile.exists()) {
            editOptionsButton.setVisibility(View.VISIBLE);
            optionsNotFoundText.setVisibility(View.GONE);
            
            editOptionsButton.setOnClickListener(v -> {
                if (hasStoragePermission()) {
                    openOptionsEditor();
                } else {
                    requestStoragePermissions();
                }
            });
        } else {
            editOptionsButton.setVisibility(View.GONE);
            optionsNotFoundText.setVisibility(View.VISIBLE);
        }
        
        // Set up editor buttons
        if (saveOptionsButton != null) {
            saveOptionsButton.setOnClickListener(v -> saveOptionsFile());
        }
        
        if (undoOptionsButton != null) {
            undoOptionsButton.setOnClickListener(v -> undoChanges());
        }
        
        if (redoOptionsButton != null) {
            redoOptionsButton.setOnClickListener(v -> redoChanges());
        }
        
        if (searchOptionsButton != null) {
            searchOptionsButton.setOnClickListener(v -> {
                if (searchInputLayout.getVisibility() == View.GONE) {
                    toggleSearch();
                } else {
                    // If search is already open, cycle through matches
                    String searchTerm = searchEditText.getText().toString().trim();
                    if (!searchTerm.isEmpty()) {
                        findNextMatch(searchTerm);
                    }
                }
            });
        }
        
        if (closeEditorButton != null) {
            closeEditorButton.setOnClickListener(v -> closeOptionsEditor());
        }
        
        // Set up search functionality
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String searchTerm = s.toString().trim();
                    if (!searchTerm.isEmpty()) {
                        searchInText(searchTerm);
                    } else {
                        // Clear search results when search term is empty
                        clearSearchResults();
                    }
                }
                
                @Override
                public void afterTextChanged(Editable s) {}
            });
            
            // Handle Enter key in search
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String searchTerm = searchEditText.getText().toString().trim();
                    if (!searchTerm.isEmpty()) {
                        findNextMatch(searchTerm);
                    }
                    return true;
                }
                return false;
            });
        }
        
        // Set up text change listener for undo/redo
        if (optionsTextEditor != null) {
            optionsTextEditor.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    // Add to undo stack when text changes
                    String currentText = s.toString();
                    if (!currentText.equals(originalOptionsContent) && !undoStack.isEmpty() && !currentText.equals(undoStack.peek())) {
                        undoStack.push(currentText);
                        redoStack.clear(); // Clear redo stack when new changes are made
                    }
                }
            });
            
            // Handle touch events to ensure proper focus
            optionsTextEditor.setOnTouchListener((v, event) -> {
                v.requestFocus();
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
        }
    }

    private void openOptionsEditor() {
        try {
            // Read the options.txt file
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(optionsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            originalOptionsContent = content.toString();
            optionsTextEditor.setText(originalOptionsContent);
            
            // Initialize undo stack
            undoStack.clear();
            redoStack.clear();
            undoStack.push(originalOptionsContent);
            
            // Show editor and disable edit button
            optionsEditorLayout.setVisibility(View.VISIBLE);
            editOptionsButton.setEnabled(false);
            editOptionsButton.setText("Editor Open");
            
            Toast.makeText(requireContext(), "Options.txt loaded successfully", Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to load options.txt: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void saveOptionsFile() {
        try {
            String content = optionsTextEditor.getText().toString();
            try (FileWriter writer = new FileWriter(optionsFile)) {
                writer.write(content);
            }
            
            originalOptionsContent = content;
            Toast.makeText(requireContext(), "Options.txt saved successfully", Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to save options.txt: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void undoChanges() {
        if (undoStack.size() > 1) {
            int currentCursorPosition = optionsTextEditor.getSelectionStart();
            String currentText = optionsTextEditor.getText().toString();
            redoStack.push(currentText);
            undoStack.pop(); // Remove current state
            String previousText = undoStack.peek();
            optionsTextEditor.setText(previousText);
            
            // Maintain cursor position or set to safe position
            int safePosition = Math.min(currentCursorPosition, previousText.length());
            optionsTextEditor.setSelection(safePosition);
        } else {
            Toast.makeText(requireContext(), "Nothing to undo", Toast.LENGTH_SHORT).show();
        }
    }

    private void redoChanges() {
        if (!redoStack.isEmpty()) {
            int currentCursorPosition = optionsTextEditor.getSelectionStart();
            String redoText = redoStack.pop();
            undoStack.push(redoText);
            optionsTextEditor.setText(redoText);
            
            // Maintain cursor position or set to safe position
            int safePosition = Math.min(currentCursorPosition, redoText.length());
            optionsTextEditor.setSelection(safePosition);
        } else {
            Toast.makeText(requireContext(), "Nothing to redo", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSearch() {
        if (searchInputLayout.getVisibility() == View.GONE) {
            searchInputLayout.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
        } else {
            searchInputLayout.setVisibility(View.GONE);
            // Clear search results and highlighting
            clearSearchResults();
        }
    }

    private void searchInText(String searchTerm) {
        if (searchTerm.isEmpty()) {
            clearSearchResults();
            return;
        }
        
        // Update current search term and find all matches
        currentSearchTerm = searchTerm;
        findAllMatches(searchTerm);
        
        String text = optionsTextEditor.getText().toString();
        SpannableString spannable = new SpannableString(text);
        
        // Highlight all matches
        for (int matchIndex : searchMatches) {
            spannable.setSpan(
                new BackgroundColorSpan(0xFFFFFF00), // Yellow highlight color
                matchIndex,
                matchIndex + searchTerm.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        optionsTextEditor.setText(spannable);
        
        // Reset match index when search term changes
        currentMatchIndex = -1;
    }

    private void findAllMatches(String searchTerm) {
        searchMatches.clear();
        String text = optionsTextEditor.getText().toString();
        String lowerText = text.toLowerCase();
        String lowerSearchTerm = searchTerm.toLowerCase();
        
        int index = lowerText.indexOf(lowerSearchTerm);
        while (index >= 0) {
            searchMatches.add(index);
            index = lowerText.indexOf(lowerSearchTerm, index + 1);
        }
    }

    private void clearSearchResults() {
        searchMatches.clear();
        currentMatchIndex = -1;
        currentSearchTerm = "";
        // Clear highlighting by resetting text
        String plainText = optionsTextEditor.getText().toString();
        optionsTextEditor.setText(plainText);
    }

    private void closeOptionsEditor() {
        optionsEditorLayout.setVisibility(View.GONE);
        searchInputLayout.setVisibility(View.GONE);
        
        // Re-enable edit button
        editOptionsButton.setEnabled(true);
        editOptionsButton.setText("Edit options.txt");
        
        // Clear undo/redo stacks
        undoStack.clear();
        redoStack.clear();
        
        Toast.makeText(requireContext(), "Editor closed", Toast.LENGTH_SHORT).show();
    }

    private void findNextMatch(String searchTerm) {
        // If search term changed, find all matches first
        if (!searchTerm.equals(currentSearchTerm)) {
            currentSearchTerm = searchTerm;
            findAllMatches(searchTerm);
            currentMatchIndex = -1;
        }
        
        if (searchMatches.isEmpty()) {
            Toast.makeText(requireContext(), "No matches found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Move to next match
        currentMatchIndex++;
        if (currentMatchIndex >= searchMatches.size()) {
            currentMatchIndex = 0; // Wrap around to first match
        }
        
        int matchPosition = searchMatches.get(currentMatchIndex);
        
        // Select the found text
        optionsTextEditor.setSelection(matchPosition, matchPosition + searchTerm.length());
        optionsTextEditor.requestFocus();
        
        // Scroll to make the selection visible
        scrollToPosition(matchPosition);
        
        // Show current match info
        String message = "Match " + (currentMatchIndex + 1) + " of " + searchMatches.size();
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void scrollToPosition(int position) {
        // Get the layout of the EditText
        android.text.Layout layout = optionsTextEditor.getLayout();
        if (layout != null) {
            // Get the line number for the position
            int line = layout.getLineForOffset(position);
            
            // Get the Y coordinate of the line
            int lineTop = layout.getLineTop(line);
            int lineBottom = layout.getLineBottom(line);
            int lineHeight = lineBottom - lineTop;
            
            // Calculate scroll position to center the line in view
            int editorHeight = optionsTextEditor.getHeight();
            int scrollY = Math.max(0, lineTop - (editorHeight / 2) + (lineHeight / 2));
            
            // Scroll to the calculated position
            optionsTextEditor.scrollTo(0, scrollY);
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Check media permissions
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Check MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+ to Android 10 - Check legacy storage permissions
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                   ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Below Android 6, permissions are granted at install time
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Request media permissions
            String[] permissions = {
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
            requestPermissions(permissions, 1001);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Request MANAGE_EXTERNAL_STORAGE
            Toast.makeText(requireContext(), "Please grant 'All files access' permission to backup files", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+ to Android 10 - Request READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE
            String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            requestPermissions(permissions, 1001);
        }
    }

    private void createBackupAtLocation(Uri saveUri, File rootDir) {
        try {
            // Check if source directory exists and has content
            if (!rootDir.exists()) {
                Toast.makeText(requireContext(), "Minecraft data directory not found: " + rootDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            
            File[] files = rootDir.listFiles();
            if (files == null || files.length == 0) {
                Toast.makeText(requireContext(), "No files found to backup in: " + rootDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            
            Toast.makeText(requireContext(), "Creating backup...", Toast.LENGTH_SHORT).show();
            
            // Create backup directly to the chosen location
            try (ZipOutputStream zos = new ZipOutputStream(requireContext().getContentResolver().openOutputStream(saveUri))) {
                zipDirectoryToStream(rootDir, rootDir.getAbsolutePath(), zos);
                Toast.makeText(requireContext(), "Backup saved successfully!", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void backupAndShare(File rootDir) {
        try {
            // Check if source directory exists and has content
            if (!rootDir.exists()) {
                Toast.makeText(requireContext(), "Minecraft data directory not found: " + rootDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            
            File[] files = rootDir.listFiles();
            if (files == null || files.length == 0) {
                Toast.makeText(requireContext(), "No files found to backup in: " + rootDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            
            File cacheDir = requireContext().getCacheDir();
            File zipFile = new File(cacheDir, "mojang_backup.zip");
            
            // Delete existing zip file if it exists
            if (zipFile.exists()) {
                zipFile.delete();
            }
            
            Toast.makeText(requireContext(), "Creating backup...", Toast.LENGTH_SHORT).show();
            zipDirectory(rootDir, zipFile);
            
            if (!zipFile.exists() || zipFile.length() == 0) {
                Toast.makeText(requireContext(), "Failed to create backup file", Toast.LENGTH_LONG).show();
                return;
            }
            
            Uri fileUri = FileProvider.getUriForFile(requireContext(), "com.origin.launcher.fileprovider", zipFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/zip");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Backup Zip"));
            Toast.makeText(requireContext(), "Backup created successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace(); // This will help with debugging
        }
    }

    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFileRecursive(sourceDir, sourceDir.getAbsolutePath(), zos);
        }
    }

    private void zipDirectoryToStream(File sourceDir, String basePath, ZipOutputStream zos) throws IOException {
        zipFileRecursive(sourceDir, basePath, zos);
    }

    private void zipFileRecursive(File fileToZip, String basePath, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) return;
        if (fileToZip.isDirectory()) {
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFileRecursive(childFile, basePath, zos);
                }
            }
            return;
        }
        
        // Skip files that can't be read
        if (!fileToZip.canRead()) {
            return;
        }
        
        String zipEntryName = fileToZip.getAbsolutePath().replace(basePath, "").replaceFirst("^/", "");
        if (zipEntryName.isEmpty()) {
            zipEntryName = fileToZip.getName();
        }
        
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        } catch (IOException e) {
            // Skip files that can't be read, but continue with others
            System.err.println("Skipping file due to error: " + fileToZip.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            
            if (granted) {
                if (currentRootDir != null) {
                    backupAndShare(currentRootDir);
                } else {
                    Toast.makeText(requireContext(), "No Minecraft data found to backup", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(requireContext(), "Storage permission is required to backup files", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Refresh ScrollView and container background colors
     */
    private void refreshScrollViewBackground() {
        try {
            if (modulesScrollView != null) {
                // Make ScrollView background transparent
                modulesScrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
            if (modulesContainer != null) {
                // Make container background transparent
                modulesContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        } catch (Exception e) {
            // Fallback to transparent background
            if (modulesScrollView != null) {
                modulesScrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
            if (modulesContainer != null) {
                modulesContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }
    }

    // Improved adapter for folder names with custom styling
    private static class FolderAdapter extends RecyclerView.Adapter<FolderViewHolder> {
        private final List<String> folders;
        private final Context context;
        
        FolderAdapter(List<String> folders, Context context) { 
            this.folders = folders; 
            this.context = context;
        }
        
        @NonNull
        @Override
        public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
            return new FolderViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
            holder.bind(folders.get(position));
            // Apply theme to the folder item
            holder.applyTheme();
        }
        
        @Override
        public int getItemCount() { return folders.size(); }
    }
    
    private static class FolderViewHolder extends RecyclerView.ViewHolder {
        private final android.widget.TextView textView;
        private final MaterialCardView cardView;
        private final ImageView iconView;
        
        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.folderNameText);
            cardView = (MaterialCardView) itemView;
            iconView = itemView.findViewById(R.id.folderIcon);
        }
        
        void bind(String folderName) {
            textView.setText(folderName);
        }
        
        void applyTheme() {
            try {
                ThemeManager themeManager = ThemeManager.getInstance();
                if (themeManager != null && themeManager.isThemeLoaded()) {
                    // Apply theme to the card
                    cardView.setCardBackgroundColor(themeManager.getColor("surfaceVariant"));
                    cardView.setStrokeColor(themeManager.getColor("outline"));
                    cardView.setStrokeWidth((int) (1 * itemView.getContext().getResources().getDisplayMetrics().density));
                    cardView.setCardElevation(2 * itemView.getContext().getResources().getDisplayMetrics().density);
                    
                    // Ensure corner radius is preserved
                    cardView.setRadius(12 * itemView.getContext().getResources().getDisplayMetrics().density);
                    
                    // Apply theme to the text
                    textView.setTextColor(themeManager.getColor("onSurface"));
                    
                    // Apply theme to the icon
                    iconView.setColorFilter(themeManager.getColor("primary"));
                    
                    // Add ripple effect
                    RippleDrawable ripple = new RippleDrawable(
                        ColorStateList.valueOf(ThemeUtils.createOptimizedRippleColor("onSurface", "card")),
                        null,
                        null
                    );
                    cardView.setForeground(ripple);
                }
            } catch (Exception e) {
                // Handle error gracefully
            }
        }
    }
}