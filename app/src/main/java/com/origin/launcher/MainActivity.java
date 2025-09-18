package com.origin.launcher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction; // Add this import
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.graphics.Color;

public class MainActivity extends BaseThemedActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_DISCLAIMER_SHOWN = "disclaimer_shown";
    private static final String KEY_THEMES_DIALOG_SHOWN = "themes_dialog_shown";
    private SettingsFragment settingsFragment;
    private int currentFragmentIndex = 0; // Move this to class level
    private LinearProgressIndicator globalProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        // Check if this is the first launch
        checkFirstLaunch();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // Apply theme to bottom navigation
        ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
        
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String presenceActivity = "";
            int newIndex = -1;
            
            if (item.getItemId() == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
                presenceActivity = "In Home";
                newIndex = 0;
            } else if (item.getItemId() == R.id.navigation_dashboard) {
                selectedFragment = new DashboardFragment();
                presenceActivity = "In Dashboard";
                newIndex = 1;
            } else if (item.getItemId() == R.id.navigation_settings) {
                if (settingsFragment == null) {
                    settingsFragment = new SettingsFragment();
                }
                selectedFragment = settingsFragment;
                presenceActivity = "In Settings";
                newIndex = 2;
            }

            if (selectedFragment != null) {
                // Determine direction based on tab indices
                boolean isForward = newIndex > getCurrentFragmentIndex();
                
                navigateToFragmentWithAnimation(selectedFragment, isForward);
                
                // Update current fragment index
                setCurrentFragmentIndex(newIndex);
                
                // Update Discord presence
                DiscordRPCHelper.getInstance().updatePresence(presenceActivity, "Using the best MCPE Client");
                
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
            setCurrentFragmentIndex(0); // Set initial index
        }
    }
    
    // Remove the duplicate navigateToFragment method - keep only this one
    private int getCurrentFragmentIndex() {
        return currentFragmentIndex;
    }

    private void setCurrentFragmentIndex(int index) {
        this.currentFragmentIndex = index;
    }

    private void navigateToFragmentWithAnimation(Fragment fragment, boolean isForward) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        if (isForward) {
            transaction.setCustomAnimations(
                R.anim.slide_in_right, 
                R.anim.slide_out_left, 
                R.anim.slide_in_left,  
                R.anim.slide_out_right 
            );
        } else {
            transaction.setCustomAnimations(
                R.anim.slide_in_left,  
                R.anim.slide_out_right,  
                R.anim.slide_in_right,  
                R.anim.slide_out_left 
            );
        }
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        boolean disclaimerShown = prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false);
        boolean themesDialogShown = prefs.getBoolean(KEY_THEMES_DIALOG_SHOWN, false);
        
        if (isFirstLaunch) {
            showFirstLaunchDialog(prefs, disclaimerShown, themesDialogShown);
            // Mark as not first launch anymore
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        } else if (!themesDialogShown) {
            // Show themes dialog if it hasn't been shown yet (for existing users)
            showThemesDialog(prefs, disclaimerShown);
        } else if (!disclaimerShown) {
            showDisclaimerDialog(prefs);
        }
    }

    private void showFirstLaunchDialog(SharedPreferences prefs, boolean disclaimerShown, boolean themesDialogShown) {
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Welcome to Xelo Client")
                .setMessage("Launch Minecraft once before doing anything, to make the config load properly")
                .setIcon(R.drawable.ic_info)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    dialog.dismiss();
                    // Show themes dialog after first launch dialog if not shown yet
                    if (!themesDialogShown) {
                        showThemesDialog(prefs, disclaimerShown);
                    } else if (!disclaimerShown) {
                        // If themes dialog was already shown but disclaimer wasn't, show disclaimer
                        showDisclaimerDialog(prefs);
                    }
                })
                .setCancelable(false)
                .show();
    }
    
    private void showThemesDialog(SharedPreferences prefs, boolean disclaimerShown) {
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("THEMES!!🎉")
                .setMessage("xelo client now supports custom themes! download themes from https://themes.xeloclient.in or make your own themes from https://docs.xeloclient.com")
                .setIcon(R.drawable.ic_info)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    dialog.dismiss();
                    // Mark themes dialog as shown
                    prefs.edit().putBoolean(KEY_THEMES_DIALOG_SHOWN, true).apply();
                    // Show disclaimer dialog after themes dialog if not shown yet
                    if (!disclaimerShown) {
                        showDisclaimerDialog(prefs);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showDisclaimerDialog(SharedPreferences prefs) {
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Important Disclaimer")
                .setMessage("This application is not affiliated with, endorsed by, or related to Mojang Studios, Microsoft Corporation, or any of their subsidiaries. " +
                           "Minecraft is a trademark of Mojang Studios. This is an independent third-party launcher. " +
                           "\n\nBy clicking 'I Understand', you acknowledge that you use this launcher at your own risk and that the developers are not responsible for any issues that may arise.")
                .setIcon(R.drawable.ic_warning)
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

    public void showGlobalProgress(int max) {
        if (globalProgress == null) {
            globalProgress = findViewById(R.id.global_download_progress);
        }
        if (globalProgress != null) {
            globalProgress.setIndeterminate(false);
            globalProgress.setMax(max);
            globalProgress.setProgress(0);
            globalProgress.setVisibility(View.VISIBLE);
        }
    }

    public void updateGlobalProgress(int value) {
        if (globalProgress != null) {
            globalProgress.setIndeterminate(false);
            globalProgress.setProgress(value);
        }
    }

    public void hideGlobalProgress() {
        if (globalProgress != null) {
            globalProgress.setVisibility(View.GONE);
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