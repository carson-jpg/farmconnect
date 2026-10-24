package com.farmconnect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** The officer dashboard (static/officer) and the company portal (static/company) are static pages. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/officer", "/officer/index.html");
        registry.addRedirectViewController("/officer/", "/officer/index.html");
        registry.addRedirectViewController("/company", "/company/index.html");
        registry.addRedirectViewController("/company/", "/company/index.html");
    }
}
