package com.inkwell.security;

import com.inkwell.user.Role;

public record AuthUser(Long id, String username, Role role) {
    public boolean isAdmin() { return role == Role.ADMIN; }
}
