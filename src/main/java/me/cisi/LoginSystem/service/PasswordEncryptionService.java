package me.cisi.LoginSystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordEncryptionService {

    private final PasswordEncoder passwordEncoder;
    private final String pepper;

    public PasswordEncryptionService(PasswordEncoder passwordEncoder,
                                     @Value("${SECURITY_PEPPER}") String pepper) {
        this.passwordEncoder = passwordEncoder;
        this.pepper = pepper;

        if (this.pepper == null || this.pepper.isBlank()) {
            throw new IllegalStateException("ERRO CRÍTICO: SECURITY_PEPPER não está configurado!");
        }
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword + this.pepper);
    }

    public boolean verifyPassword(String rawPassword, String encodedPasswordFromDb) {
        return passwordEncoder.matches(rawPassword + this.pepper, encodedPasswordFromDb);
    }
}