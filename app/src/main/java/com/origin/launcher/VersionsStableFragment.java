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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class VersionsStableFragment extends BaseThemedFragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_versions_stable, container, false);
        
        try {
            LinearLayout versionsContainer = view.findViewById(R.id.versionsContainerStable);
            if (versionsContainer != null) {
                // Populate a few stable MCPE version cards
                addVersionCard(versionsContainer,
                    "MCPE 1.21.30",
                    "Latest stable release of Minecraft Bedrock (MCPE)",
                    "https://www.minecraft.net/en-us/article/minecraft-update-1-21-30");
                addVersionCard(versionsContainer,
                    "MCPE 1.21.20",
                    "Stability fixes and minor improvements",
                    "https://www.minecraft.net/en-us/article/minecraft-update-1-21-20");
                addVersionCard(versionsContainer,
                    "MCPE 1.21.10",
                    "Feature polish and bug fixes",
                    "https://www.minecraft.net/en-us/article/minecraft-update-1-21-10");
            }
        } catch (Exception e) {
            Log.e("VersionsStable", "Failed to initialize version cards", e);
        }
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

    private void addVersionCard(LinearLayout container, String title, String subtitle, String url) {
        // Create card
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

        // Main horizontal layout
        LinearLayout main = new LinearLayout(requireContext());
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density)
        );
        main.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Text column
        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(textParams);

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        ThemeUtils.applyThemeToTextView(titleView, "onSurface");

        TextView subView = new TextView(requireContext());
        subView.setText(subtitle);
        subView.setTextSize(14);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        subView.setLayoutParams(subParams);
        ThemeUtils.applyThemeToTextView(subView, "onSurfaceVariant");

        textCol.addView(titleView);
        textCol.addView(subView);

        // Right action column
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.setMarginStart((int) (16 * getResources().getDisplayMetrics().density));
        actions.setLayoutParams(actionsParams);

        MaterialButton downloadBtn = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        downloadBtn.setText("Download");
        downloadBtn.setTag("outlined");
        ThemeUtils.applyThemeToButton(downloadBtn, requireContext());
        downloadBtn.setOnClickListener(v -> openUrl(url));

        actions.addView(downloadBtn);

        main.addView(textCol);
        main.addView(actions);
        card.addView(main);

        // Add card and spacing
        container.addView(card);
        View spacer = new View(requireContext());
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (12 * getResources().getDisplayMetrics().density)
        );
        spacer.setLayoutParams(spacerParams);
        container.addView(spacer);
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }
}