package com.origin.launcher;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.BufferedReader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ModulesFragment extends BaseThemedFragment {
    private ScrollView modulesScrollView;
    private LinearLayout modulesContainer;
    private File configFile;
    private List<ModuleItem> moduleItems;

    private static class ModuleItem {
        private final String name;
        private final String description;
        private final String configKey;
        private boolean enabled;

        ModuleItem(String name, String description, String configKey) {
            this.name = name;
            this.description = description;
            this.configKey = configKey;
            this.enabled = false;
        }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getConfigKey() { return configKey; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modules, container, false);
        modulesScrollView = view.findViewById(R.id.modulesScrollView);
        modulesContainer = view.findViewById(R.id.modulesContainer);

        configFile = new File(requireContext().getExternalFilesDir(null), "origin_mods/config.json");
        initializeModulesContainer();
        loadModuleItems();
        loadModuleStates();
        populateModules();
        return view;
    }

    @Override
    protected void onApplyTheme() {
        super.onApplyTheme();
        refreshScrollViewBackground();
        refreshModuleCardBackgroundsWithAnimation();
    }

    private void loadModuleItems() {
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
    }

    private void populateModules() {
        modulesContainer.removeAllViews();
        for (ModuleItem item : moduleItems) {
            View v = createModuleView(item);
            modulesContainer.addView(v);
        }
    }

    private View createModuleView(ModuleItem module) {
        MaterialCardView moduleCard = new MaterialCardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density)
        );
        cardParams.height = (int) (110 * getResources().getDisplayMetrics().density);
        moduleCard.setLayoutParams(cardParams);

        float cornerRadius = 16 * getResources().getDisplayMetrics().density;
        moduleCard.setRadius(cornerRadius);
        moduleCard.setPreventCornerOverlap(false);
        moduleCard.setUseCompatPadding(false);
        moduleCard.setClipChildren(true);
        moduleCard.setClipToPadding(false);
        moduleCard.setCardBackgroundColor(ThemeManager.getInstance().getColor("surfaceVariant"));
        moduleCard.setCardElevation(6 * getResources().getDisplayMetrics().density);
        moduleCard.setMaxCardElevation(8 * getResources().getDisplayMetrics().density);
        moduleCard.setStrokeWidth(0);
        moduleCard.setClickable(true);
        moduleCard.setFocusable(true);
        moduleCard.setBackground(null);

        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackground(null);
        mainLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        mainLayout.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density)
        );

        LinearLayout topArea = new LinearLayout(getContext());
        topArea.setOrientation(LinearLayout.HORIZONTAL);
        topArea.setGravity(Gravity.CENTER_VERTICAL);
        topArea.setBackground(null);
        topArea.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        topParams.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        topArea.setLayoutParams(topParams);

        ImageView iconView = new ImageView(getContext());
        iconView.setImageResource(R.drawable.wrench);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconView.setBackground(null);
        iconView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density)
        );
        iconParams.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density));
        iconView.setLayoutParams(iconParams);
        iconView.setColorFilter(ThemeManager.getInstance().getColor("onSurface"));

        TextView moduleNameText = new TextView(getContext());
        moduleNameText.setText(module.getName());
        moduleNameText.setTextSize(16);
        moduleNameText.setTypeface(null, android.graphics.Typeface.BOLD);
        moduleNameText.setBackground(null);
        moduleNameText.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        moduleNameText.setSingleLine(false);
        ThemeUtils.applyThemeToTextView(moduleNameText, "onSurface");
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        moduleNameText.setLayoutParams(nameParams);

        MaterialSwitch moduleSwitch = new MaterialSwitch(getContext());
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        moduleSwitch.setLayoutParams(switchParams);
        moduleSwitch.setChecked(module.isEnabled());
        ThemeUtils.applyThemeToSwitch(moduleSwitch, requireContext());
        moduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            module.setEnabled(isChecked);
            onModuleToggle(module, isChecked);
        });

        topArea.addView(iconView);
        topArea.addView(moduleNameText);
        topArea.addView(moduleSwitch);

        TextView moduleDescriptionText = new TextView(getContext());
        moduleDescriptionText.setText(module.getDescription());
        moduleDescriptionText.setTextSize(14);
        moduleDescriptionText.setBackground(null);
        moduleDescriptionText.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        moduleDescriptionText.setSingleLine(false);
        moduleDescriptionText.setMaxLines(3);
        moduleDescriptionText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ThemeUtils.applyThemeToTextView(moduleDescriptionText, "onSurfaceVariant");
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = 0;
        moduleDescriptionText.setLayoutParams(descParams);

        mainLayout.addView(topArea);
        mainLayout.addView(moduleDescriptionText);
        moduleCard.addView(mainLayout);

        return moduleCard;
    }

    private void refreshModuleCardBackgroundsWithAnimation() {
        try {
            if (modulesContainer != null) {
                for (int i = 0; i < modulesContainer.getChildCount(); i++) {
                    View child = modulesContainer.getChildAt(i);
                    if (child instanceof MaterialCardView) {
                        MaterialCardView card = (MaterialCardView) child;
                        int currentBackground = card.getCardBackgroundColor().getDefaultColor();
                        int targetBackground = ThemeManager.getInstance().getColor("surfaceVariant");
                        ThemeUtils.animateBackgroundColorTransition(card, currentBackground, targetBackground, 300);
                        float cornerRadius = 16 * getResources().getDisplayMetrics().density;
                        card.setRadius(cornerRadius);
                        card.setCardElevation(6 * getResources().getDisplayMetrics().density);
                        card.setMaxCardElevation(8 * getResources().getDisplayMetrics().density);
                        card.setStrokeWidth(0);
                        card.setPreventCornerOverlap(false);
                        card.setUseCompatPadding(false);
                        card.setClipChildren(true);
                        card.setClipToPadding(false);
                        card.setBackground(null);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void initializeModulesContainer() {
        if (modulesContainer != null) {
            modulesContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            modulesContainer.setBackground(null);
            modulesContainer.setClipChildren(false);
            modulesContainer.setClipToPadding(false);
            int padding = (int) (8 * getResources().getDisplayMetrics().density);
            modulesContainer.setPadding(0, padding, 0, padding);
        }
        if (modulesScrollView != null) {
            modulesScrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            modulesScrollView.setBackground(null);
            modulesScrollView.setClipChildren(false);
            modulesScrollView.setClipToPadding(false);
            modulesScrollView.setFillViewport(true);
        }
    }

    private void refreshScrollViewBackground() {
        try {
            initializeModulesContainer();
            View rootView = getView();
            if (rootView != null) {
                rootView.setBackgroundColor(ThemeManager.getInstance().getColor("background"));
            }
        } catch (Exception ignored) {}
    }

    private void onModuleToggle(ModuleItem module, boolean isEnabled) {
        updateConfigFile(module.getConfigKey(), isEnabled);
        Toast.makeText(requireContext(), module.getName() + (isEnabled ? " enabled" : " disabled"), Toast.LENGTH_SHORT).show();
    }

    private void loadModuleStates() {
        try {
            if (!configFile.exists()) {
                createDefaultConfig();
            }
            StringBuilder content = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            JSONObject config = new JSONObject(content.toString());
            for (ModuleItem module : moduleItems) {
                if (config.has(module.getConfigKey())) {
                    module.setEnabled(config.getBoolean(module.getConfigKey()));
                }
            }
        } catch (Exception ignored) {}
    }

    private void createDefaultConfig() {
        try {
            JSONObject defaultConfig = new JSONObject();
            defaultConfig.put("Nohurtcam", false);
            defaultConfig.put("night_vision", false);
            defaultConfig.put("Nofog", false);
            defaultConfig.put("particles_disabler", false);
            defaultConfig.put("java_clouds", false);
            defaultConfig.put("java_cubemap", true);
            defaultConfig.put("classic_skins", false);
            defaultConfig.put("no_flipbook_animations", true);
            defaultConfig.put("no_shadows", false);
            defaultConfig.put("xelo_title", true);
            defaultConfig.put("white_block_outline", false);
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultConfig.toString(2));
            }
        } catch (Exception ignored) {}
    }

    private void updateConfigFile(String key, boolean value) {
        try {
            JSONObject config;
            if (configFile.exists()) {
                StringBuilder content = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(configFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }
                }
                config = new JSONObject(content.toString());
            } else {
                config = new JSONObject();
            }
            config.put(key, value);
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(config.toString(2));
            }
        } catch (Exception ignored) {}
    }
}

