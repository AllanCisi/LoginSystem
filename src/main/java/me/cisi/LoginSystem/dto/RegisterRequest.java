package me.cisi.LoginSystem.dto;

// Novo Record DTO para agrupar os dados que vêm do register.html
public record RegisterRequest(
        String username,
        String email,
        String emailCode,
        String password
) {
}