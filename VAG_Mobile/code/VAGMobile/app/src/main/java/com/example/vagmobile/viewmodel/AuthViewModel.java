package com.example.vagmobile.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.vagmobile.model.User;
import com.example.vagmobile.repository.AuthRepository;

import java.util.Map;

public class AuthViewModel extends AndroidViewModel {
    private AuthRepository authRepository;
    private MutableLiveData<Map<String, Object>> loginResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> registerResult = new MutableLiveData<>();

    public AuthViewModel(Application application) {
        super(application);
        authRepository = new AuthRepository(application.getApplicationContext());
    }

    public void login(String username, String password) {
        authRepository.login(username, password).observeForever(result -> {
            loginResult.setValue(result);
        });
    }

    public void register(User user) {
        authRepository.register(user).observeForever(result -> {
            registerResult.setValue(result);
        });
    }

    public LiveData<Map<String, Object>> getLoginResult() {
        return loginResult;
    }

    public LiveData<Map<String, Object>> getRegisterResult() {
        return registerResult;
    }
}