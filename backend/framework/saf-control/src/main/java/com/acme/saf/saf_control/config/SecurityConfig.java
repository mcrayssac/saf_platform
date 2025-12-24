package com.acme.saf.saf_control.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;

import com.acme.saf.saf_control.security.ApiKeyFilter;

import java.util.concurrent.Callable;

import static com.acme.saf.saf_control.config.SecurityConstants.PUBLIC_PATHS;

@Configuration
public class SecurityConfig {

    private final ApiKeyFilter apiKeyFilter;

    public SecurityConfig(ApiKeyFilter apiKeyFilter) {
        this.apiKeyFilter = apiKeyFilter;
    }

    @Bean
    SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http)) // Enable CORS
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiKeyFilter, AbstractPreAuthenticatedProcessingFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll() //Les endpoints publics
                        .anyRequest().authenticated() //Toutes les autres requêtes nécessitent le filtre
                );
        return http.build();
    }

    /**
     * Propagate SecurityContext to async threads (for WebFlux/Reactive)
     */
    @Bean
    public CallableProcessingInterceptor callableProcessingInterceptor() {
        return new CallableProcessingInterceptor() {
            @Override
            public <T> void beforeConcurrentHandling(org.springframework.web.context.request.NativeWebRequest request, Callable<T> task) {
                // Capture the SecurityContext before async processing
            }

            @Override
            public <T> void preProcess(org.springframework.web.context.request.NativeWebRequest request, Callable<T> task) {
                // Set the SecurityContext in the async thread
                SecurityContext context = SecurityContextHolder.getContext();
                if (context != null && context.getAuthentication() != null) {
                    SecurityContextHolder.setContext(context);
                }
            }
        };
    }
}
