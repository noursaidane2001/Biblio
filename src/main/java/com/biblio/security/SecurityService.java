package com.biblio.security;

import com.biblio.entities.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    public boolean isEmailVerified(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return Boolean.TRUE.equals(user.getEmailVerifie());
    }

    public boolean canBorrowBook(Long livreId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return Boolean.TRUE.equals(user.getActif()) && Boolean.TRUE.equals(user.getEmailVerifie()) && user.isUsager();
    }

    public boolean isOwner(Long userId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        return userId != null && user.getId() != null && user.getId().equals(userId);
    }
}
