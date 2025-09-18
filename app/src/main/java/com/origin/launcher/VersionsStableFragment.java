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
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class VersionsStableFragment extends BaseThemedFragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_versions_stable, container, false);
        
        try {
            LinearLayout versionsContainer = view.findViewById(R.id.versionsContainerStable);
            LinearProgressIndicator progressBar = view.findViewById(R.id.download_progress_stable);
            if (versionsContainer != null) {
                populateFromRepo(versionsContainer);
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

    private void populateFromRepo(LinearLayout container) {
        new Thread(() -> {
            try {
                VersionsRepository repo = new VersionsRepository();
                java.util.List<VersionsRepository.VersionEntry> entries = repo.getVersions(requireContext());
                java.util.List<VersionsRepository.VersionEntry> stable = new java.util.ArrayList<>();
                for (VersionsRepository.VersionEntry ve : entries) {
                    if (!ve.isBeta) stable.add(ve);
                }
                requireActivity().runOnUiThread(() -> {
                    container.removeAllViews();
                    for (int i = 0; i < stable.size(); i++) {
                        VersionsRepository.VersionEntry e = stable.get(i);
                        addVersionCard(container, e.title, "", e.url);
                    }
                });
            } catch (Exception ex) {
                Log.e("VersionsStable", "Failed to load versions", ex);
            }
        }).start();
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

        android.view.ContextThemeWrapper buttonCtx = new android.view.ContextThemeWrapper(
            requireContext(), com.google.android.material.R.style.Widget_Material3_Button
        );
        MaterialButton downloadBtn = new MaterialButton(buttonCtx, null, 0);
        downloadBtn.setText("Download");
        // Match Home fragment button sizing/shape
        downloadBtn.setTextSize(14);
        downloadBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        downloadBtn.setPadding(pad, pad, pad, pad);
        downloadBtn.setCornerRadius((int) (28 * getResources().getDisplayMetrics().density));
        // Apply theme like Home fragment does
        ThemeUtils.applyThemeToButton(downloadBtn, requireContext());
        downloadBtn.setOnClickListener(v -> startDownload(url, title));

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

    private void startDownload(String url, String title) {
        View root = getView();
        if (root == null) return;
        LinearProgressIndicator progress = root.findViewById(R.id.download_progress_stable);
        if (progress != null) {
            progress.setIndeterminate(true);
            progress.setVisibility(View.VISIBLE);
        }
        new Thread(() -> {
            boolean ok = false;
            try {
                String fileName = buildApkFileNameFromTitle(title);
                File versionsDir = new File(requireContext().getExternalFilesDir(null), "versions");
                if (!versionsDir.exists()) versionsDir.mkdirs();
                File outFile = new File(versionsDir, fileName);
                downloadToFile(url, outFile);
                ok = true;
            } catch (Exception ex) {
                Log.e("VersionsStable", "Download failed", ex);
            }
            boolean finalOk = ok;
            requireActivity().runOnUiThread(() -> {
                if (progress != null) progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(), finalOk ? "Downloaded" : "Download failed", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private String buildApkFileNameFromTitle(String title) {
        // Extract version numbers and remove dots to form filename like 121100.apk
        String version = title.replaceAll("[^0-9\\.]", "");
        version = version.replace(".", "");
        if (version.isEmpty()) version = String.valueOf(System.currentTimeMillis());
        return version + ".apk";
    }

    private void downloadToFile(String urlStr, File outFile) throws Exception {
        java.net.HttpURLConnection conn = null;
        try {
            java.net.URL url = new java.net.URL(urlStr);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.connect();
            if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());
            try (java.io.InputStream in = conn.getInputStream();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}