package com.example.vagmobile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.vagmobile.model.User;
import com.example.vagmobile.repository.UserRepository;
import com.example.vagmobile.repository.ExhibitionRepository;
import java.util.Map;

public class UserViewModel extends ViewModel {
    private UserRepository userRepository;
    private ExhibitionRepository exhibitionRepository;
    private MutableLiveData<Map<String, Object>> currentUserResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> userResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> userArtworksResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> userExhibitionsResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> likedArtworksResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> updateProfileResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> changePasswordResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> deleteArtworkResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> artistsResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> artistsWithArtworksResult = new MutableLiveData<>();

    public UserViewModel() {
        userRepository = new UserRepository();
        exhibitionRepository = new ExhibitionRepository();
    }

    // Метод для установки контекста в репозиторий
    public void setContext(android.content.Context context) {
        if (userRepository != null) {
            userRepository.setContext(context);
        }
        if (exhibitionRepository != null) {
            exhibitionRepository.setContext(context);
        }
    }

    public void getAllArtists() {
        userRepository.getAllArtists().observeForever(result -> {
            artistsResult.setValue(result);
        });
    }

    public void getArtistsWithArtworks() {
        userRepository.getAllArtists().observeForever(result -> {
            artistsWithArtworksResult.setValue(result);
        });
    }

    public void getCurrentUser() {
        userRepository.getCurrentUser().observeForever(result -> {
            currentUserResult.setValue(result);
        });
    }

    public void getUser(Long userId) {
        userRepository.getUser(userId).observeForever(result -> {
            userResult.setValue(result);
        });
    }

    public void getUserArtworks(Long userId, int page, int size) {
        UserRepository repoWithContext = new UserRepository(null); // TODO: Pass context if needed
        repoWithContext.getUserArtworks(userId, page, size).observeForever(result -> {
            userArtworksResult.setValue(result);
        });
    }

    public void getUserExhibitions(Long userId, int page, int size) {
        exhibitionRepository.getUserExhibitions(userId, page, size).observeForever(result -> {
            userExhibitionsResult.setValue(result);
        });
    }

    public void getLikedArtworks(int page, int size) {
        // TODO: Implement this method
    }

    public void updateProfile(String username, String email, String description) {
        userRepository.updateProfile(username, email, description).observeForever(result -> {
            updateProfileResult.setValue(result);
        });
    }

    public void updateProfileWithPassword(String username, String email, String description, String currentPassword) {
        userRepository.updateProfileWithPassword(username, email, description, currentPassword).observeForever(result -> {
            updateProfileResult.setValue(result);
        });
    }

    public void changePassword(String currentPassword, String newPassword) {
        userRepository.changePassword(currentPassword, newPassword, false).observeForever(result -> {
            changePasswordResult.setValue(result);
        });
    }

    public void changePasswordSkipValidation(String currentPassword, String newPassword) {
        userRepository.changePassword(currentPassword, newPassword, true).observeForever(result -> {
            changePasswordResult.setValue(result);
        });
    }

    public void deleteArtwork(Long artworkId) {
        userRepository.deleteArtwork(artworkId).observeForever(result -> {
            deleteArtworkResult.setValue(result);
        });
    }

    // Getters for LiveData
    public LiveData<Map<String, Object>> getArtistsResult() {
        return artistsResult;
    }

    public LiveData<Map<String, Object>> getArtistsWithArtworksResult() {
        return artistsWithArtworksResult;
    }

    public LiveData<Map<String, Object>> getCurrentUserResult() {
        return currentUserResult;
    }

    public LiveData<Map<String, Object>> getUserResult() {
        return userResult;
    }

    public LiveData<Map<String, Object>> getUserArtworksResult() {
        return userArtworksResult;
    }

    public LiveData<Map<String, Object>> getUserExhibitionsResult() {
        return userExhibitionsResult;
    }

    public LiveData<Map<String, Object>> getLikedArtworksResult() {
        return likedArtworksResult;
    }

    public LiveData<Map<String, Object>> getUpdateProfileResult() {
        return updateProfileResult;
    }

    public LiveData<Map<String, Object>> getChangePasswordResult() {
        return changePasswordResult;
    }

    public LiveData<Map<String, Object>> getDeleteArtworkResult() {
        return deleteArtworkResult;
    }
}