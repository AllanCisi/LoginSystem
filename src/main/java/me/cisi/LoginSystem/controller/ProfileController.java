package me.cisi.LoginSystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.cisi.LoginSystem.entity.User;
import me.cisi.LoginSystem.repository.UserRepository;
import me.cisi.LoginSystem.service.MfaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final MfaService mfaService;

    public ProfileController(UserRepository userRepository, MfaService mfaService) {
        this.userRepository = userRepository;
        this.mfaService = mfaService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String email;
        String googleName = "";

        if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
            googleName = oauth2User.getAttribute("name");
        } else {
            email = auth.getName();
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;

        if (userOpt.isEmpty()) {
            if (auth.getPrincipal() instanceof OAuth2User) {
                user = new User();
                user.setEmail(email);
                String baseUsername = googleName != null ? googleName.replaceAll("\\s+", "").toLowerCase() : email.split("@")[0];
                user.setUsername(baseUsername + "_" + UUID.randomUUID().toString().substring(0, 5));
                user.setPassword(UUID.randomUUID().toString());
                user = userRepository.save(user);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utilizador não encontrado.");
            }
        } else {
            user = userOpt.get();
        }

        try {
            String qrCodeUri = "";
            String setupSecret = "";

            if (!user.isMfaEnabled()) {
                if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
                    setupSecret = mfaService.generateSecret();
                    user.setMfaSecret(setupSecret);
                    userRepository.save(user);
                } else {
                    setupSecret = user.getMfaSecret();
                }
                qrCodeUri = mfaService.generateQrCodeImageUri(setupSecret, user.getEmail());
            }

            CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            String csrfHeader = (csrf != null) ? csrf.getHeaderName() : "X-XSRF-TOKEN";
            String csrfTokenValue = (csrf != null) ? csrf.getToken() : "";

            return ResponseEntity.ok(Map.of(
                    "email", user.getEmail(),
                    "username", user.getUsername(),
                    "mfaEnabled", user.isMfaEnabled(),
                    "qrCode", qrCodeUri,
                    "tempSecret", setupSecret,
                    "csrfHeader", csrfHeader,
                    "csrfToken", csrfTokenValue
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao gerar QR Code");
        }
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enable2fa(@RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String email;
        if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
        } else {
            email = auth.getName();
        }

        User user = userRepository.findByEmail(email).orElseThrow();

        String code = request.get("code");
        String secret = request.get("secret");

        if (mfaService.verifyCode(secret, code)) {
            user.setMfaEnabled(true);
            user.setMfaSecret(secret);
            userRepository.save(user);
            return ResponseEntity.ok("2FA ativado com sucesso!");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Código inválido. Tente novamente.");
    }
}