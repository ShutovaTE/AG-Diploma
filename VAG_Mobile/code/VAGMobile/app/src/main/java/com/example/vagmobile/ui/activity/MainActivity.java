package com.example.vagmobile.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import com.example.vagmobile.R;
import com.example.vagmobile.ui.fragment.HomeFragment;
import com.example.vagmobile.ui.fragment.MoreFragment;
import com.example.vagmobile.ui.fragment.ProfileFragment;
import com.example.vagmobile.util.SharedPreferencesHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public BottomNavigationView bottomNavigationView;
    private androidx.appcompat.widget.Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupBottomNavigation();
        setToolbarTitle("Арт-галерея");

        if (savedInstanceState == null) {
            // Check if we need to open a specific profile
            Intent intent = getIntent();
            if (intent != null && intent.getBooleanExtra("openProfile", false)) {
                Long userId = intent.getLongExtra("userId", -1);
                if (userId != -1) {
                    loadFragmentWithBackStack(ProfileFragment.newInstance(userId));
                } else {
                    loadFragment(new HomeFragment());
                }
            } else {
                loadFragment(new HomeFragment());
            }
        }
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        toolbar = findViewById(R.id.toolbar);

        // Setup toolbar (without setSupportActionBar since we use NoActionBar theme)
        // setSupportActionBar(toolbar); // Removed because we use NoActionBar theme
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                    setToolbarTitle("Арт-галерея");
                } else if (itemId == R.id.nav_more) {
                    selectedFragment = new MoreFragment();
                    setToolbarTitle("Поиск");
                } else if (itemId == R.id.nav_create) {
                    // Handle create button - show dialog without changing fragment
                    SharedPreferencesHelper prefs = new SharedPreferencesHelper(MainActivity.this);
                    if (!prefs.isLoggedIn()) {
                        Toast.makeText(MainActivity.this, "Пожалуйста, войдите в систему чтобы создавать контент", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    } else {
                        showCreateContentDialog();
                    }
                    return true; // Don't change fragment for create button
                } else if (itemId == R.id.nav_profile) {
                    // Check if user is logged in before showing profile
                    SharedPreferencesHelper prefs = new SharedPreferencesHelper(MainActivity.this);
                    if (!prefs.isLoggedIn()) {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        return true; // Don't change fragment
                    } else {
                        selectedFragment = ProfileFragment.newInstance(null); // null = current user profile
                        // Don't set title here - ProfileFragment will handle it
                    }
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }

                return true;
            }
        });
    }


    private void showCreateContentDialog() {
        String[] options = {"Создать публикацию", "Создать выставку"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите тип контента")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Создать публикацию
                            Intent artworkIntent = new Intent(MainActivity.this, CreateArtworkActivity.class);
                            startActivity(artworkIntent);
                            break;
                        case 1: // Создать выставку
                            Intent exhibitionIntent = new Intent(MainActivity.this, CreateExhibitionActivity.class);
                            startActivity(exhibitionIntent);
                            break;
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void loadFragmentWithBackStack(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            if (bottomNavigationView.getSelectedItemId() == R.id.nav_home) {
                super.onBackPressed();
                finishAffinity();
            } else {
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    private void setToolbarTitle(String title) {
        if (toolbar != null) {
            toolbar.setTitle(title);
        }
    }

    // Public method to open user profile from other activities/fragments
    public void openUserProfile(Long userId) {
        ProfileFragment profileFragment = ProfileFragment.newInstance(userId);
        loadFragmentWithBackStack(profileFragment);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Handle opening profile from intent
        if (intent != null && intent.getBooleanExtra("openProfile", false)) {
            Long userId = intent.getLongExtra("userId", -1);
            if (userId != -1) {
                loadFragmentWithBackStack(ProfileFragment.newInstance(userId));
            }
        }
    }
}