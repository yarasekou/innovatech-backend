package com.innovatech.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // ⚠️ UNE SEULE règle qui sert TOUT le dossier uploads
        // URL : /api/uploads/produits/xxx.jpg → fichier : uploads/produits/xxx.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(basePath.toUri().toString());

        System.out.println("✅ Upload dir : " + basePath);
        System.out.println("✅ Upload URL : /api/uploads/**");
    }
}