package com.acme.saf.saf_control.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry r) {
    r.addMapping("/**")
     .allowedOrigins("http://localhost:5173")
     .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
     .allowedHeaders("*")
     .allowCredentials(true);
  }
}

