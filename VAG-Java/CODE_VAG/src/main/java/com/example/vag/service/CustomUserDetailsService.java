package com.example.vag.service;

import com.example.vag.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface CustomUserDetailsService extends UserDetailsService {
    User getCurrentUser();
}