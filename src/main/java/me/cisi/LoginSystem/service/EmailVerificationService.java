package me.cisi.LoginSystem.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final String EMAIL_FROM = System.getenv("EMAIL_USER");

    public EmailVerificationService(JavaMailSender mailSender, StringRedisTemplate redisTemplate) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
    }

    public void sendVerificationCode(String toEmail) {
        String code = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set("EMAIL_CODE:" + toEmail, code, Duration.ofMinutes(10));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(EMAIL_FROM);
        message.setTo(toEmail);
        message.setSubject("Seu código de verificação - Portfolio");
        message.setText("Olá!\n\nSeu código de verificação é: " + code + "\n\nEste código expira em 10 minutos.");

        mailSender.send(message);
    }

    public boolean verifyCode(String email, String codeProvided) {
        String savedCode = redisTemplate.opsForValue().get("EMAIL_CODE:" + email);
        if (savedCode != null && savedCode.equals(codeProvided)) {
            redisTemplate.delete("EMAIL_CODE:" + email);
            return true;
        }
        return false;
    }

    public void sendPasswordResetCode(String toEmail) {
        String code = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set("RESET_CODE:" + toEmail, code, Duration.ofMinutes(10));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(EMAIL_FROM);
        message.setTo(toEmail);
        message.setSubject("Redefinição de Senha - LoginSystem");
        message.setText("Olá!\n\nRecebemos um pedido para alterar a sua senha.\nSeu código de redefinição é: " + code + "\n\nEste código expira em 10 minutos.");

        mailSender.send(message);
    }

    public boolean isValidPasswordResetCode(String email, String codeProvided) {
        String savedCode = redisTemplate.opsForValue().get("RESET_CODE:" + email);
        return savedCode != null && savedCode.equals(codeProvided);
    }

    public void consumePasswordResetCode(String email) {
        redisTemplate.delete("RESET_CODE:" + email);
    }
}