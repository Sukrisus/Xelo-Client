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

public class VersionsFragment extends BaseThemedFragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_versions, container, false);
        
        // Initialize back button
        initializeBackButton(view);
        
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
    
    @Override
    public void onResume() {
        super.onResume();
        DiscordRPCHelper.getInstance().updateMenuPresence("version manager");
    }
    
    @Override
    public void onPause() {
        super.onPause();
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