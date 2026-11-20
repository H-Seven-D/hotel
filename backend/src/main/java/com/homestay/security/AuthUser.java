package com.homestay.security;

public record AuthUser(Long id, String username, String realName, String role, Long sessionId) {
}
