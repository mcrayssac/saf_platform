package com.acme.saf.saf_control.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

import static com.acme.saf.saf_control.config.SecurityConstants.PUBLIC_PATHS;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String HEADER_NAME = "X-API-KEY";
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    @Value("${saf.security.api-key}")
    private String validApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.debug("Checking if path should be filtered: {}", path);
        for (String publicPath : PUBLIC_PATHS) {
            if (pathMatcher.match(publicPath, path)) {
                log.debug("Path {} matches public path {}, skipping filter", path, publicPath);
                return true;
            }
        }
        log.debug("Path {} will be filtered", path);
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(HEADER_NAME);
        log.debug("ApiKeyFilter invoked for path: {}", request.getRequestURI());
        log.debug("Received API key: {}, Expected API key: {}", apiKey, validApiKey);

        if (validApiKey.equals(apiKey)) {
            log.debug("API key validation successful, setting authentication");
            // Create authentication token and set it in SecurityContext
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "api-user", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authentication set in SecurityContext");
            filterChain.doFilter(request, response);
        } else {
            log.warn("API key validation failed for path: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
        }
    }
}
