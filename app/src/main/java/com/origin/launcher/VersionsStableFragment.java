package com.origin.launcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import android.util.Log;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

public class VersionsStableFragment extends BaseThemedFragment {

    private LinearLayout versionsContainer;
    private java.util.List<VersionItem> versionItems;

    private static class VersionItem {
        String name;
        String description;
        String downloadUrl;
        VersionItem(String name, String description, String downloadUrl) {
            this.name = name;
            this.description = description;
            this.downloadUrl = downloadUrl;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_versions_stable, container, false);
        
        versionsContainer = view.findViewById(R.id.versionsContainerStable);
        initializeVersions();
        populateVersionCards();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        DiscordRPCHelper.getInstance().updateMenuPresence("version switcher - stable");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }

    private void initializeVersions() {
        versionItems = new java.util.ArrayList<>();
        // Sample stable MCPE versions; replace URLs with real downloads as needed
        versionItems.add(new VersionItem("1.21.30", "Latest stable features and fixes", "https://example.com/mcpe/1.21.30.apk"));
        versionItems.add(new VersionItem("1.21.20", "Stability improvements", "https://example.com/mcpe/1.21.20.apk"));
        versionItems.add(new VersionItem("1.21.0", "1.21 release", "https://example.com/mcpe/1.21.0.apk"));
    }

    private void populateVersionCards() {
        if (versionsContainer == null) return;
        versionsContainer.removeAllViews();
        for (int i = 0; i < versionItems.size(); i++) {
            View card = createVersionCard(versionItems.get(i));
            versionsContainer.addView(card);
            if (i < versionItems.size() - 1) {
                View spacer = new View(requireContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (12 * getResources().getDisplayMetrics().density)
                );
                spacer.setLayoutParams(params);
                versionsContainer.addView(spacer);
            }
        }
    }

    private View createVersionCard(VersionItem item) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(cardParams);
        card.setRadius(12 * getResources().getDisplayMetrics().density);
        card.setCardElevation(0);
        card.setClickable(true);
        card.setFocusable(true);
        ThemeUtils.applyThemeToCard(card, requireContext());
        card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));

        LinearLayout mainLayout = new LinearLayout(requireContext());
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density)
        );
        mainLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout textLayout = new LinearLayout(requireContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        textLayout.setLayoutParams(textParams);

        TextView nameText = new TextView(requireContext());
        nameText.setText("MCPE " + item.name);
        nameText.setTextSize(16);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        ThemeUtils.applyThemeToTextView(nameText, "onSurface");

        TextView descText = new TextView(requireContext());
        descText.setText(item.description);
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

        LinearLayout rightContainer = new LinearLayout(requireContext());
        rightContainer.setOrientation(LinearLayout.HORIZONTAL);
        rightContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rightParams.setMarginStart((int) (16 * getResources().getDisplayMetrics().density));
        rightContainer.setLayoutParams(rightParams);

        MaterialButton downloadButton = new MaterialButton(requireContext());
        downloadButton.setText("Download");
        ThemeUtils.applyThemeToButton(downloadButton, requireContext());
        downloadButton.setOnClickListener(v -> openUrl(item.downloadUrl));

        rightContainer.addView(downloadButton);
        mainLayout.addView(textLayout);
        mainLayout.addView(rightContainer);
        card.addView(mainLayout);
        return card;
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onApplyTheme() {
        // Rebuild cards to refresh colors when theme changes
        populateVersionCards();
    }
}