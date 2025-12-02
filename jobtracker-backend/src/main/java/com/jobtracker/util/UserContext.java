package com.jobtracker.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class UserContext {

    public static String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        Object principal = auth.getPrincipal();

        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();  
            // Example: auth0|abc123xyz
        }

        return null;
    }

    public static String getEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        Object principal = auth.getPrincipal();

        if (principal instanceof Jwt jwt) {
            return jwt.getClaim("email");
        }

        return null;
    }
}

