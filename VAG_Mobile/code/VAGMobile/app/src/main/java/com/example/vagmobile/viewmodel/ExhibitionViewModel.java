package com.example.vagmobile.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.vagmobile.repository.ExhibitionRepository;
import java.util.Map;

public class ExhibitionViewModel extends AndroidViewModel {

    private ExhibitionRepository exhibitionRepository;
    private MutableLiveData<Map<String, Object>> exhibitionsResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> exhibitionResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> createResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> updateResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> deleteResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> exhibitionArtworksResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> addArtworkResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> removeArtworkResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> userExhibitionsResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> userArtworksForExhibitionResult = new MutableLiveData<>();

    public ExhibitionViewModel(Application application) {
        super(application);
        exhibitionRepository = new ExhibitionRepository(application);
    }

    public MutableLiveData<Map<String, Object>> getExhibitionsResult() {
        return exhibitionsResult;
    }

    public MutableLiveData<Map<String, Object>> getExhibitionResult() {
        return exhibitionResult;
    }

    public MutableLiveData<Map<String, Object>> getCreateResult() {
        return createResult;
    }

    public MutableLiveData<Map<String, Object>> getUpdateResult() {
        return updateResult;
    }

    public MutableLiveData<Map<String, Object>> getDeleteResult() {
        return deleteResult;
    }

    public MutableLiveData<Map<String, Object>> getExhibitionArtworksResult() {
        return exhibitionArtworksResult;
    }

    public MutableLiveData<Map<String, Object>> getAddArtworkResult() {
        return addArtworkResult;
    }

    public MutableLiveData<Map<String, Object>> getRemoveArtworkResult() {
        return removeArtworkResult;
    }

    public MutableLiveData<Map<String, Object>> getUserExhibitionsResult() {
        return userExhibitionsResult;
    }

    public MutableLiveData<Map<String, Object>> getUserArtworksForExhibitionResult() {
        return userArtworksForExhibitionResult;
    }

    public void getExhibitions(int page, int size) {
        exhibitionRepository.getExhibitions(page, size).observeForever(exhibitionsResult::setValue);
    }

    public void getExhibition(Long id) {
        exhibitionRepository.getExhibition(id).observeForever(exhibitionResult::setValue);
    }

    public void createExhibition(String title, String description, boolean authorOnly) {
        exhibitionRepository.createExhibition(title, description, authorOnly).observeForever(createResult::setValue);
    }

    public void updateExhibition(Long exhibitionId, String title, String description, boolean authorOnly) {
        exhibitionRepository.updateExhibition(exhibitionId, title, description, authorOnly).observeForever(updateResult::setValue);
    }

    public void deleteExhibition(Long exhibitionId) {
        exhibitionRepository.deleteExhibition(exhibitionId).observeForever(deleteResult::setValue);
    }

    public void getExhibitionArtworks(Long exhibitionId, int page, int size) {
        exhibitionRepository.getExhibitionArtworks(exhibitionId, page, size).observeForever(exhibitionArtworksResult::setValue);
    }

    public void addArtworkToExhibition(Long exhibitionId, Long artworkId) {
        exhibitionRepository.addArtworkToExhibition(exhibitionId, artworkId).observeForever(addArtworkResult::setValue);
    }

    public void removeArtworkFromExhibition(Long exhibitionId, Long artworkId) {
        exhibitionRepository.removeArtworkFromExhibition(exhibitionId, artworkId).observeForever(removeArtworkResult::setValue);
    }

    public void getUserExhibitions(Long userId, int page, int size) {
        exhibitionRepository.getUserExhibitions(userId, page, size).observeForever(userExhibitionsResult::setValue);
    }

    public void getUserArtworksForExhibition(Long exhibitionId, int page, int size) {
        exhibitionRepository.getUserArtworksForExhibition(exhibitionId, page, size).observeForever(userArtworksForExhibitionResult::setValue);
    }
}
