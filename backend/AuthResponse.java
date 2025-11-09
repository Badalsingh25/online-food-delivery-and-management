package com.hungerexpress.auth;

public record AuthResponse(String token, String refreshToken, String email, String role, String fullName) {}
