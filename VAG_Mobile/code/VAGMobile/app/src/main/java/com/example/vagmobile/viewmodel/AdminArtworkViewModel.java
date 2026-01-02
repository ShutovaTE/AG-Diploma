package com.example.vagmobile.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.vagmobile.repository.AdminArtworkRepository;
import java.util.Map;

public class AdminArtworkViewModel extends AndroidViewModel {
    private AdminArtworkRepository adminArtworkRepository;
    private MutableLiveData<Map<String, Object>> artworksResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> approveResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> rejectResult = new MutableLiveData<>();

    public AdminArtworkViewModel(Application application) {
        super(application);
        adminArtworkRepository = new AdminArtworkRepository(application);
    }

    public void getAdminArtworks(int page, int size, String status) {
        adminArtworkRepository.getAdminArtworks(page, size, status).observeForever(result -> {
            artworksResult.setValue(result);
        });
    }

    public void approveArtwork(Long artworkId) {
        adminArtworkRepository.approveArtwork(artworkId).observeForever(result -> {
            approveResult.setValue(result);
        });
    }

    public void rejectArtwork(Long artworkId) {
        adminArtworkRepository.rejectArtwork(artworkId).observeForever(result -> {
            rejectResult.setValue(result);
        });
    }

    public LiveData<Map<String, Object>> getArtworksResult() {
        return artworksResult;
    }

    public LiveData<Map<String, Object>> getApproveResult() {
        return approveResult;
    }

    public LiveData<Map<String, Object>> getRejectResult() {
        return rejectResult;
    }
}

