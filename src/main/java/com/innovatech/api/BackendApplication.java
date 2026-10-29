package com.innovatech.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    // ⚠️ CODE TEMPORAIRE POUR GÉNÉRER LE HASH
    @Bean
    public CommandLineRunner generatePasswordHash(PasswordEncoder passwordEncoder) {
        return args -> {
            String hashPourPasser = passwordEncoder.encode("passer");
            System.out.println("=====================================================");
            System.out.println("HASH POUR 'passer' : " + hashPourPasser);
            System.out.println("=====================================================");
        };
    }
}