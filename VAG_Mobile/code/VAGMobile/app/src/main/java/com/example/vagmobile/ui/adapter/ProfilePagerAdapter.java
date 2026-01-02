package com.example.vagmobile.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.vagmobile.ui.fragment.UserArtworksFragment;
import com.example.vagmobile.ui.fragment.UserExhibitionsFragment;

public class ProfilePagerAdapter extends FragmentStateAdapter {

    private final Long userId;
    private final boolean isOwnProfile;

    public ProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity, Long userId, boolean isOwnProfile) {
        super(fragmentActivity);
        this.userId = userId;
        this.isOwnProfile = isOwnProfile;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return UserArtworksFragment.newInstance(userId, isOwnProfile);
            case 1:
                return UserExhibitionsFragment.newInstance(userId);
            default:
                return UserArtworksFragment.newInstance(userId, isOwnProfile);
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Два таба: Публикации и Выставки
    }
}
