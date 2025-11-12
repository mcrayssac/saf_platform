package com.acme.saf.saf_control.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;

import static com.acme.saf.saf_control.config.SecurityConstants.PUBLIC_PATHS;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-API-KEY";
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    @Value("${saf.security.api-key}")
    private String validApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String publicPath : PUBLIC_PATHS) {
            if (pathMatcher.match(publicPath, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(HEADER_NAME);

        if (validApiKey.equals(apiKey)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
        }
    }
}
