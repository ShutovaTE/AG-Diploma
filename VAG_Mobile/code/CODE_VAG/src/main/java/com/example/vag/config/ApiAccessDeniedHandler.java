package com.example.vag.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ApiAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        System.out.println("=== API ACCESS DENIED HANDLER ===");
        System.out.println("Request URI: " + requestURI);
        System.out.println("Context Path: " + contextPath);
        System.out.println("Access Denied Exception: " + accessDeniedException.getMessage());
        
        // Проверяем, является ли это API запросом (с учетом context path)
        if (requestURI.contains("/api/mobile/")) {
            System.out.println("Returning JSON error for API request");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Access denied\"}");
        } else {
            // Для веб-запросов перенаправляем на страницу доступа запрещен
            System.out.println("Redirecting to access denied page for web request");
            response.sendRedirect(contextPath + "/auth/access-denied");
        }
    }
}

