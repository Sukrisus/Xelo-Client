package com.origin.launcher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.graphics.Color;

public class MainActivity extends BaseThemedActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_DISCLAIMER_SHOWN = "disclaimer_shown";
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        // Check if this is the first launch
        checkFirstLaunch();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // Apply theme to bottom navigation
        ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
        
        final int[] lastItemId = {R.id.navigation_home};
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String presenceActivity = "";
            
            if (item.getItemId() == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
                presenceActivity = "In Home";
            } else if (item.getItemId() == R.id.navigation_dashboard) {
                selectedFragment = new DashboardFragment();
                presenceActivity = "In Dashboard";
            } else if (item.getItemId() == R.id.navigation_settings) {
                // Keep reference to settings fragment for activity results
                if (settingsFragment == null) {
                    settingsFragment = new SettingsFragment();
                }
                selectedFragment = settingsFragment;
                presenceActivity = "In Settings";
            }

            if (selectedFragment != null) {
                boolean forward = item.getItemId() > lastItemId[0];
                lastItemId[0] = item.getItemId();
                getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                        forward ? R.anim.slide_in_right : R.anim.slide_in_left,
                        forward ? R.anim.slide_out_left : R.anim.slide_out_right,
                        forward ? R.anim.slide_in_left : R.anim.slide_in_right,
                        forward ? R.anim.slide_out_right : R.anim.slide_out_left
                    )
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
                
                // Update Discord presence with custom text
                DiscordRPCHelper.getInstance().updatePresence(presenceActivity, "Using the best MCPE Client");
                
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
        }
    }

    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        boolean disclaimerShown = prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false);
        
        if (isFirstLaunch) {
            showFirstLaunchDialog(prefs, disclaimerShown);
            // Mark as not first launch anymore
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        } else if (!disclaimerShown) {
            showDisclaimerDialog(prefs);
        }
    }

    private void showFirstLaunchDialog(SharedPreferences prefs, boolean disclaimerShown) {
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Welcome to Xelo Client")
                .setMessage("Launch Minecraft once before doing anything, to make the config load properly")
                .setIcon(R.drawable.ic_info) // You can use any icon you have, or remove this line
                .setPositiveButton("Proceed", (dialog, which) -> {
                    dialog.dismiss();
                    // Show disclaimer dialog after first launch dialog if not shown yet
                    if (!disclaimerShown) {
                        showDisclaimerDialog(prefs);
                    }
                })
                .setCancelable(false) // Prevents dismissing by tapping outside or back button
                .show();
    }

    private void showDisclaimerDialog(SharedPreferences prefs) {
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Important Disclaimer")
                .setMessage("This application is not affiliated with, endorsed by, or related to Mojang Studios, Microsoft Corporation, or any of their subsidiaries. " +
                           "Minecraft is a trademark of Mojang Studios. This is an independent third-party launcher. " +
                           "\n\nBy clicking 'I Understand', you acknowledge that you use this launcher at your own risk and that the developers are not responsible for any issues that may arise.")
                .setIcon(R.drawable.ic_warning) // You can use a warning icon or remove this line
                .setPositiveButton("I Understand", (dialog, which) -> {
                    dialog.dismiss();
                    // Mark disclaimer as shown
                    prefs.edit().putBoolean(KEY_DISCLAIMER_SHOWN, true).apply();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "MainActivity onActivityResult: requestCode=" + requestCode + 
              ", resultCode=" + resultCode + ", data=" + (data != null ? "present" : "null"));
        
        // Forward the result to the settings fragment if it's a Discord login
        if (requestCode == DiscordLoginActivity.DISCORD_LOGIN_REQUEST_CODE && settingsFragment != null) {
            Log.d(TAG, "Forwarding Discord login result to SettingsFragment");
            settingsFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Update presence when app comes to foreground
        DiscordRPCHelper.getInstance().updatePresence("Using Xelo Client", "Using the best MCPE Client");
    }
    
    @Override
    protected void onApplyTheme() {
        super.onApplyTheme();
        
        // Refresh bottom navigation theme with animation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            try {
                // Get current background color safely
                int currentBackground = Color.parseColor("#141414"); // Default fallback
                if (bottomNavigationView.getBackground() != null) {
                    try {
                        currentBackground = ((android.graphics.drawable.ColorDrawable) bottomNavigationView.getBackground()).getColor();
                    } catch (Exception e) {
                        // Use default if we can't get current color
                    }
                }
                
                int targetBackground = ThemeManager.getInstance().getColor("surface");
                
                // Animate background color transition
                ThemeUtils.animateBackgroundColorTransition(bottomNavigationView, currentBackground, targetBackground, 300);
                
                // Apply other theme properties
                ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
            } catch (Exception e) {
                // Fallback to immediate theme application
                ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Update presence when app goes to background  
        DiscordRPCHelper.getInstance().updatePresence("Xelo Client", "Using the best MCPE Client");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up RPC helper
        DiscordRPCHelper.getInstance().cleanup();
    }
}