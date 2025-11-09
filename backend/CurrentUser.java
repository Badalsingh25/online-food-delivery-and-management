package com.hungerexpress.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}
    public static String email(){
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? String.valueOf(a.getPrincipal()) : null;
    }
}
