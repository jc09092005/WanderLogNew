package com.example.wanderlognew;

import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.json.JSONObject;

/**
 * MainActivity serves as the central activity that manages navigation
 * between fragments via a custom bottom navigation bar and a top toolbar.
 */
public class MainActivity extends AppCompatActivity {

    // Bottom navigation tabs
    LinearLayout tabHome, tabAdd, tabSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge layout and lock orientation to portrait
        EdgeToEdge.enable(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        // Attempt to load translated bottom navigation labels from cache
        JSONObject nav = TranslatedStore.get("bottom_nav_strings.json");
        if (nav != null) {
            ((TextView) findViewById(R.id.tab_home_label)).setText(nav.optString("home", "Home"));
            ((TextView) findViewById(R.id.tab_add_label)).setText(nav.optString("add", "Add"));
            ((TextView) findViewById(R.id.tab_account_label)).setText(nav.optString("settings", "Settings"));
        }

        // Fallback: Load strings using JSON loader (from assets)
        JsonStringLoader jsonLoader = new JsonStringLoader(this, "bottom_nav_strings.json");

        // String references
        TextView homeLabel = findViewById(R.id.tab_home_label);
        TextView addLabel = findViewById(R.id.tab_add_label);
        TextView settingsLabel = findViewById(R.id.tab_account_label);
        homeLabel.setText(jsonLoader.getString("home"));
        addLabel.setText(jsonLoader.getString("add"));
        settingsLabel.setText(jsonLoader.getString("settings"));

        // Initialise bottom navigation tab views
        tabHome = findViewById(R.id.tab_home);
        tabAdd = findViewById(R.id.tab_add);
        tabSettings = findViewById(R.id.tab_settings);

        // Setup toolbar without title
        Toolbar toolbar = findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Handle system window insets (status/navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bottom tab click listeners to load fragments
        tabHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment());
            clearBackStack();
        });

        tabAdd.setOnClickListener(v -> {
            loadFragment(new AddFragment());
            clearBackStack();
        });

        tabSettings.setOnClickListener(v -> {
            loadFragment(new SettingsFragment());
            clearBackStack();
        });

        // Listen for back stack changes to toggle toolbar back button
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(canGoBack);

                // Tint the back arrow icon if it exists
                Toolbar tb = findViewById(R.id.top_toolbar);
                Drawable icon = tb.getNavigationIcon();
                if (icon != null) {
                    Drawable wrapped = DrawableCompat.wrap(icon.mutate());
                    DrawableCompat.setTint(wrapped, ContextCompat.getColor(this, R.color.main_blue)); // Use white or main_blue
                    tb.setNavigationIcon(wrapped);
                }
            }
        });

        // Load default (HomeFragment) only if this is the first launch
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }


    // Loads the given fragment into the main content container
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    // Clears the fragment back stack to prevent navigation stacking
    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    // Handles the top back button action
    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }
}
