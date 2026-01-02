package com.example.vag.config;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        System.out.println("=== API AUTHENTICATION ENTRY POINT ===");
        System.out.println("Request URI: " + requestURI);
        System.out.println("Context Path: " + contextPath);
        System.out.println("Auth Exception: " + authException.getMessage());
        
        // Проверяем, является ли это API запросом (с учетом context path)
        if (requestURI.contains("/api/mobile/")) {
            System.out.println("Returning JSON error for API request");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Authentication required\"}");
        } else {
            // Для веб-запросов перенаправляем на страницу логина
            System.out.println("Redirecting to login page for web request");
            response.sendRedirect(contextPath + "/auth/login");
        }
    }
}

