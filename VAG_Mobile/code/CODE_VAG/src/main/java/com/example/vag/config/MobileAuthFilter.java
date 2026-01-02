package com.example.vag.config;

import com.example.vag.controller.mobile.MobileAuthController;
import com.example.vag.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class MobileAuthFilter extends GenericFilterBean {

    private final MobileAuthController mobileAuthController;

    public MobileAuthFilter(MobileAuthController mobileAuthController) {
        this.mobileAuthController = mobileAuthController;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        System.out.println("=== MOBILE AUTH FILTER ===");
        System.out.println("Path: " + path);
        System.out.println("Method: " + method);

        // ИСПРАВЛЕНО: Пропускаем статические ресурсы и НЕ мобильные запросы
        if (!path.contains("/api/mobile/") || isStaticResource(path)) {
            System.out.println("Not a mobile API request or static resource, skipping filter");
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");

        System.out.println("Auth Header: " + authHeader);

        // Если есть токен, устанавливаем SecurityContext даже для публичных эндпоинтов
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Прямая проверка токена без AuthenticationManager
            User user = mobileAuthController.getUserFromToken("Bearer " + token);

            if (user != null) {
                System.out.println("User authenticated directly: " + user.getUsername());
                System.out.println("User role: " + user.getRole().getName().name());

                // Создаем аутентификацию напрямую
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                token,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name()))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authentication set successfully");
            } else {
                System.out.println("Token validation failed, but continuing for public endpoint");
            }
        }

        // ИСКЛЮЧАЕМ публичные мобильные эндпоинты из обязательной проверки аутентификации
        if (isPublicMobileEndpoint(path, method)) {
            System.out.println("Public mobile endpoint, allowing access");
            chain.doFilter(request, response);
            return;
        }

        // Для защищенных эндпоинтов проверяем наличие аутентификации
        if (SecurityContextHolder.getContext().getAuthentication() == null ||
            !SecurityContextHolder.getContext().getAuthentication().isAuthenticated() ||
            "anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
            System.out.println("No auth header for protected mobile endpoint");
            handleUnauthorized(httpResponse, "Authentication required");
            return;
        }

        chain.doFilter(request, response);
    }

    // ИСПРАВЛЕНО: Метод для проверки статических ресурсов
    private boolean isStaticResource(String path) {
        return path.contains("/uploads/") ||
                path.contains("/resources/") ||
                path.contains("/vag/uploads/") ||
                path.contains("/css/") ||
                path.contains("/js/") ||
                path.contains("/images/");
    }

    // ИСПРАВЛЕНО: Определяем только публичные МОБИЛЬНЫЕ эндпоинты
    private boolean isPublicMobileEndpoint(String path, String method) {
        // Регистрация и логин - публичные
        if (path.contains("/api/mobile/auth/register") && "POST".equals(method)) {
            return true;
        }
        if (path.contains("/api/mobile/auth/login") && "POST".equals(method)) {
            return true;
        }

        // Публичные GET запросы для мобильных API
        if ("GET".equals(method)) {
            if (path.contains("/api/mobile/artworks") &&
                    !path.contains("/like") &&
                    !path.contains("/unlike") &&
                    !path.contains("/comment") &&
                    !path.matches(".*/api/mobile/artworks/\\d+/edit")) {
                return true;
            }
            if (path.contains("/api/mobile/categories")) {
                return true;
            }
            if (path.contains("/api/mobile/users/") && path.contains("/artworks")) {
                return true;
            }
            if (path.contains("/api/mobile/exhibitions")) {
                return true;
            }
        }

        // ИСПРАВЛЕНО: POST запросы для создания публикаций требуют аутентификации
        // но мы должны пропустить их через фильтр для проверки токена
        if ("POST".equals(method) && path.contains("/api/mobile/artworks/create")) {
            return false; // Требует аутентификации
        }

        return false;
    }

    private void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }
}