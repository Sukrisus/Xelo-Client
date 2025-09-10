package com.origin.launcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import android.util.Log;
import com.google.android.material.card.MaterialCardView;

public class SupportFragment extends BaseThemedFragment {

    private MaterialCardView githubButton;
    private MaterialCardView discordButton;
    private MaterialCardView donateButton;
    
    private static final String GITHUB_URL = "https://github.com/Xelo-Client/Xelo-Client";
    private static final String DISCORD_URL = "https://discord.gg/CHUchrEWwc";
    private static final String DONATE_URL = "https://xeloclient.in/donate.html";
    private static final String TAG = "SupportFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_support, container, false);
        
        // Initialize back button
        initializeBackButton(view);
        
        githubButton = view.findViewById(R.id.github_button);
        discordButton = view.findViewById(R.id.discord_button);
        donateButton = view.findViewById(R.id.donate_button);
        
        setupButtonListeners();
        
        return view;
    }
    
    private void initializeBackButton(View view) {
        ImageView backButton = view.findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                try {
                    requireActivity().getSupportFragmentManager().popBackStack();
                } catch (Exception e) {
                    // Handle error gracefully
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            });
        }
    }
    
    private void setupButtonListeners() {
        if (githubButton != null) {
            githubButton.setOnClickListener(v -> openUrl(GITHUB_URL, "GitHub"));
        }
        
        if (discordButton != null) {
            discordButton.setOnClickListener(v -> openUrl(DISCORD_URL, "Discord"));
        }
        if (donateButton != null) {
            donateButton.setOnClickListener(v -> openUrl(DONATE_URL, "Donate"));
        }
    }
    
    private void openUrl(String url, String appName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening " + appName + " URL", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Unable to open " + appName, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        DiscordRPCHelper.getInstance().updateMenuPresence("Support");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Update Discord RPC when leaving support
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
    
    @Override
    protected void onApplyTheme() {
        // Apply theme to the back button
        View rootView = getView();
        if (rootView != null) {
            ImageView backButton = rootView.findViewById(R.id.back_button);
            if (backButton != null) {
                backButton.setColorFilter(ThemeManager.getInstance().getColor("onBackground"));
            }
        }
    }
}