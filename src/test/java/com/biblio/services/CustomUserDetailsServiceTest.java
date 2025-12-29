package com.biblio.services;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    private UserDAO userDAO;
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDAO = mock(UserDAO.class);
        userDetailsService = new CustomUserDetailsService(userDAO);
    }

    @Test
    void testLoadUserByUsername_userExists() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setMotDePasse("password"); // ⚠ utiliser setMotDePasse

        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("password", userDetails.getPassword());

        verify(userDAO, times(1)).findByEmail("test@example.com");
    }

    @Test
    void testLoadUserByUsername_userNotFound() {
        when(userDAO.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown@example.com"));

        assertEquals("Utilisateur introuvable: unknown@example.com", exception.getMessage());

        verify(userDAO, times(1)).findByEmail("unknown@example.com");
    }
}
