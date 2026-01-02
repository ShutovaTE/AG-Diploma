package com.example.vagmobile.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.vagmobile.repository.ArtworkRepository;
import okhttp3.MultipartBody;
import java.util.Map;

public class ArtworkViewModel extends AndroidViewModel {

    private ArtworkRepository artworkRepository;
    private MutableLiveData<Map<String, Object>> artworksResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> artworkResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> likedArtworksResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> toggleLikeResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> addCommentResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> searchResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> categoryArtworksResult = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> deleteResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> artworkForAdminResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> allUserArtworksResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> createResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> updateResult = new MutableLiveData<>();

    public ArtworkViewModel(Application application) {
        super(application);
        artworkRepository = new ArtworkRepository(application);
    }

    public MutableLiveData<Map<String, Object>> getArtworksResult() {
        return artworksResult;
    }

    public MutableLiveData<Map<String, Object>> getArtworkResult() {
        return artworkResult;
    }

    public MutableLiveData<Map<String, Object>> getLikedArtworksResult() {
        return likedArtworksResult;
    }

    public MutableLiveData<Map<String, Object>> getToggleLikeResult() {
        return toggleLikeResult;
    }

    public MutableLiveData<Map<String, Object>> getAddCommentResult() {
        return addCommentResult;
    }

    public MutableLiveData<Map<String, Object>> getSearchResult() {
        return searchResult;
    }

    public MutableLiveData<Map<String, Object>> getCategoryArtworksResult() {
        return categoryArtworksResult;
    }

    public MutableLiveData<Map<String, Object>> getArtworkForAdminResult() {
        return artworkForAdminResult;
    }

    public MutableLiveData<Map<String, Object>> getCreateResult() {
        return createResult;
    }

    public MutableLiveData<Map<String, Object>> getUpdateResult() {
        return updateResult;
    }

    public void getArtworks(int page, int size) {
        artworkRepository.getArtworks(page, size).observeForever(artworksResult::setValue);
    }

    public void getArtwork(Long id) {
        artworkRepository.getArtwork(id).observeForever(artworkResult::setValue);
    }

    public void getArtworkForAdmin(Long id) {
        artworkRepository.getArtworkForAdmin(id).observeForever(artworkForAdminResult::setValue);
    }

    public void getLikedArtworks(int page, int size) {
        artworkRepository.getLikedArtworks(page, size).observeForever(likedArtworksResult::setValue);
    }

    public void toggleLike(Long artworkId, boolean isCurrentlyLiked) {
        System.out.println("ArtworkViewModel: Toggle like - artworkId: " + artworkId + ", isCurrentlyLiked: " + isCurrentlyLiked);

        artworkRepository.toggleLike(artworkId, isCurrentlyLiked).observeForever(toggleLikeResult::setValue);
    }

    public void refreshArtwork(Long artworkId) {
        artworkRepository.getArtwork(artworkId).observeForever(artworkResult::setValue);
    }

    public void addComment(Long artworkId, String content) {
        artworkRepository.addComment(artworkId, content).observeForever(addCommentResult::setValue);
    }

    public void searchArtworks(String query, int page, int size) {
        artworkRepository.searchArtworks(query, page, size).observeForever(searchResult::setValue);
    }

    public void getCategoryArtworks(Long categoryId, int page, int size) {
        artworkRepository.getCategoryArtworks(categoryId, page, size).observeForever(categoryArtworksResult::setValue);
    }

    public void createArtwork(String title, String description, String categoryIds, MultipartBody.Part image) {
        artworkRepository.createArtwork(title, description, categoryIds, image).observeForever(createResult::setValue);
    }

    public void updateArtwork(Long artworkId, String title, String description, String categoryIds, MultipartBody.Part image) {
        artworkRepository.updateArtwork(artworkId, title, description, categoryIds, image).observeForever(updateResult::setValue);
    }

    public void createArtworkSimple(String title, String description) {
        artworkRepository.createArtworkSimple(title, description).observeForever(createResult::setValue);
    }


    public void loadUserArtworks(Long userId) {
        artworkRepository.getUserArtworks(userId, 0, 100);
    }

    public LiveData<Map<String, Object>> getAllUserArtworksResult() {
        return allUserArtworksResult;
    }

    public void getAllUserArtworks(Long userId, int page, int size) {
        artworkRepository.getAllUserArtworks(userId, page, size)
                .observeForever(result -> allUserArtworksResult.setValue(result));
    }

    public LiveData<Map<String, Object>> getDeleteResult() {
        return deleteResult;
    }

    public void deleteArtwork(Long artworkId) {
        artworkRepository.deleteArtwork(artworkId)
                .observeForever(deleteResult::setValue);
    }
}