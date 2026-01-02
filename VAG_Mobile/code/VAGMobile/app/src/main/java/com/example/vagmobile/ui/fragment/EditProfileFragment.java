package com.example.vagmobile.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.vagmobile.R;
import com.example.vagmobile.model.DocPage;
import com.example.vagmobile.ui.activity.LoginActivity;
import com.example.vagmobile.ui.activity.AdminCategoriesActivity;
import com.example.vagmobile.ui.activity.AdminArtworksActivity;
import com.example.vagmobile.ui.activity.LikedArtworksActivity;
import com.example.vagmobile.ui.activity.EditProfileActivity;
import com.example.vagmobile.ui.fragment.DocumentationFragment;
import com.example.vagmobile.util.SharedPreferencesHelper;

public class EditProfileFragment extends Fragment {

    private TextView tvUsername, tvEmail, tvDescription;
    private Button btnViewProfile, btnLogout, btnAdminCategories, btnAdminArtworks,
            btnLikedArtworks, btnLogin, btnDocumentation, btnEditProfile;
    private LinearLayout loggedInLayout, guestLayout, adminSection;
    private SharedPreferencesHelper prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        initViews(view);
        setupUI();

        return view;
    }

    private void initViews(View view) {
        tvUsername = view.findViewById(R.id.tv_username);
        tvEmail = view.findViewById(R.id.tv_email);
        tvDescription = view.findViewById(R.id.tv_description);
        btnViewProfile = view.findViewById(R.id.btn_view_profile);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnAdminCategories = view.findViewById(R.id.btn_admin_categories);
        btnAdminArtworks = view.findViewById(R.id.btn_admin_artworks);
        btnLikedArtworks = view.findViewById(R.id.btn_liked_artworks);
        btnLogin = view.findViewById(R.id.btn_login);
        btnDocumentation = view.findViewById(R.id.btn_documentation);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        loggedInLayout = view.findViewById(R.id.logged_in_layout);
        guestLayout = view.findViewById(R.id.guest_layout);
        adminSection = view.findViewById(R.id.admin_section);
    }

    private void setupUI() {
        prefs = new SharedPreferencesHelper(getContext());
        updateUserInfo();

        if (btnViewProfile != null) {
            btnViewProfile.setOnClickListener(v -> {
                // Возврат к основному профилю
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(intent);
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });
        }

        if (btnLikedArtworks != null) {
            btnLikedArtworks.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LikedArtworksActivity.class);
                startActivity(intent);
            });
        }

        if (btnAdminCategories != null) {
            btnAdminCategories.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AdminCategoriesActivity.class);
                startActivity(intent);
            });
        }

        if (btnAdminArtworks != null) {
            btnAdminArtworks.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AdminArtworksActivity.class);
                startActivity(intent);
            });
        }

        if (btnDocumentation != null) {
            btnDocumentation.setOnClickListener(v -> {
                // Открываем список страниц документации
                DocumentationFragment documentationFragment = new DocumentationFragment();

                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, documentationFragment)
                            .addToBackStack("documentation")
                            .commit();
                }
            });
        }

        updateVisibility();
    }

    private void updateUserInfo() {
        if (prefs.isLoggedIn()) {
            String username = prefs.getUsername();
            String email = prefs.getEmail();

            if (tvUsername != null) {
                tvUsername.setText(username != null ? username : "Пользователь");
            }
            if (tvEmail != null) {
                tvEmail.setText(email != null ? email : "email@example.com");
            }
            if (tvDescription != null) {
                tvDescription.setText("Настройки профиля");
            }
        }
    }

    private void updateVisibility() {
        boolean isLoggedIn = prefs.isLoggedIn();
        String userRole = prefs.getUserRole();

        if (loggedInLayout != null) {
            loggedInLayout.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        }

        if (guestLayout != null) {
            guestLayout.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        }

        if (adminSection != null) {
            adminSection.setVisibility(isLoggedIn && "ADMIN".equals(userRole) ? View.VISIBLE : View.GONE);
        }
    }

    private void logout() {
        prefs.clearUserData();
        updateUserInfo();
        updateVisibility();

        Toast.makeText(getContext(), "Вы успешно вышли из системы", Toast.LENGTH_SHORT).show();

        // Возврат на главную страницу после выхода
        if (getActivity() != null) {
            // Очистить back stack и перейти на HomeFragment
            getActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // Загрузить HomeFragment
            getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new com.example.vagmobile.ui.fragment.HomeFragment())
                .commit();

            // Установить активный пункт меню на "Главная"
            if (getActivity() instanceof com.example.vagmobile.ui.activity.MainActivity) {
                ((com.example.vagmobile.ui.activity.MainActivity) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserInfo();
        updateVisibility();
    }
}
