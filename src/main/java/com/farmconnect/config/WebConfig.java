package com.farmconnect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** The officer dashboard is a static page in src/main/resources/static/officer. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/officer", "/officer/index.html");
        registry.addRedirectViewController("/officer/", "/officer/index.html");
    }
}
