package me.cisi.LoginSystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import me.cisi.LoginSystem.dto.AuthRequest;
import me.cisi.LoginSystem.dto.RegisterRequest;
import me.cisi.LoginSystem.entity.User;
import me.cisi.LoginSystem.repository.UserRepository;
import me.cisi.LoginSystem.service.EmailVerificationService;
import me.cisi.LoginSystem.service.MfaService;
import me.cisi.LoginSystem.service.PasswordEncryptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncryptionService passwordEncryptionService;
    private final EmailVerificationService emailVerificationService;
    private final MfaService mfaService;

    public AuthController(UserRepository userRepository,
                          PasswordEncryptionService passwordEncryptionService,
                          EmailVerificationService emailVerificationService,
                          MfaService mfaService,
                          AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncryptionService = passwordEncryptionService;
        this.emailVerificationService = emailVerificationService;
        this.mfaService = mfaService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body("O e-mail é obrigatório.");
        if (userRepository.findByEmail(email).isPresent()) return ResponseEntity.status(HttpStatus.CONFLICT).body("Este e-mail já está em uso.");
        try {
            emailVerificationService.sendVerificationCode(email);
            return ResponseEntity.ok("Código enviado.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro.");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) return ResponseEntity.status(HttpStatus.CONFLICT).body("E-mail já registado.");
        if (!emailVerificationService.verifyCode(request.email(), request.emailCode())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Código inválido.");

        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setEmail(request.email());
        newUser.setPassword(passwordEncryptionService.hashPassword(request.password()));
        userRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body("Conta criada com sucesso!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isPresent() && userOpt.get().isMfaEnabled()) {
            if (!passwordEncryptionService.verifyPassword(request.password(), userOpt.get().getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("E-mail ou senha incorretos");
            }
            return ResponseEntity.ok(Map.of(
                    "require2fa", true,
                    "email", userOpt.get().getEmail()
            ));
        }

        return executeSpringAuthentication(request.email(), request.password(), httpRequest);
    }

    @PostMapping("/login/verify-2fa")
    public ResponseEntity<?> verifyLogin2fa(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");
        String password = request.get("password");
        String code = request.get("code");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Código 2FA inválido");
        }

        return executeSpringAuthentication(email, password, httpRequest);
    }

    @PostMapping("/send-reset-code")
    public ResponseEntity<?> sendResetCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body("O e-mail é obrigatório.");
        if (userRepository.findByEmail(email).isPresent()) {
            try { emailVerificationService.sendPasswordResetCode(email); } catch (Exception e) { e.printStackTrace(); }
        }
        return ResponseEntity.ok("Enviado.");
    }

    @PostMapping("/verify-reset-step1")
    public ResponseEntity<?> verifyResetStep1(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        if (!emailVerificationService.isValidPasswordResetCode(email, code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Código de e-mail inválido ou expirado.");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro de segurança.");

        boolean require2fa = userOpt.get().isMfaEnabled();
        return ResponseEntity.ok(Map.of("require2fa", require2fa));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");
        String totpCode = request.get("totpCode");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro de segurança.");

        User user = userOpt.get();

        if (user.isMfaEnabled()) {
            if (totpCode == null || totpCode.isBlank() || !mfaService.verifyCode(user.getMfaSecret(), totpCode)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Código do Autenticador incorreto.");
            }
        }

        if (!emailVerificationService.isValidPasswordResetCode(email, code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Código de e-mail inválido ou expirado.");
        }

        user.setPassword(passwordEncryptionService.hashPassword(newPassword));
        userRepository.save(user);
        emailVerificationService.consumePasswordResetCode(email);

        return ResponseEntity.ok("Senha alterada com sucesso!");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logout realizado.");
    }

    private ResponseEntity<?> executeSpringAuthentication(String email, String password, HttpServletRequest httpRequest) {
        try {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);
            Authentication authentication = authenticationManager.authenticate(token);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            return ResponseEntity.ok(Map.of("require2fa", false, "message", "Login realizado com sucesso!"));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("E-mail ou senha incorretos");
        }
    }
}