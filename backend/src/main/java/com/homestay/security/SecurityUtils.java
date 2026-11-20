package com.homestay.security;

import com.homestay.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            throw new BusinessException(401, "登录状态已失效");
        }
        return user;
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(currentUser().role());
    }
}
