package com.biblio.services;

import com.biblio.dao.UserDAO;
import com.biblio.dto.AuthResponse;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.biblio.security.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserDAO userDAO;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private AuthenticationManager authenticationManager;
    private EmailService emailService;
    private UserLogService userLogService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDAO = mock(UserDAO.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        authenticationManager = mock(AuthenticationManager.class);
        emailService = mock(EmailService.class);
        userLogService = mock(UserLogService.class);

        authService = new AuthService(userDAO, passwordEncoder, jwtService, userDetailsService,
                authenticationManager, emailService, 3600000L, userLogService);
    }

    @Test
    void testAuthenticate_success() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setMotDePasse("encodedPassword");
        user.setActif(true);
        user.setEmailVerifie(true);

        Authentication authMock = mock(Authentication.class);
        when(authMock.getPrincipal()).thenReturn(user);
        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authMock);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");

        AuthResponse response = authService.authenticate("test@example.com", "password");

        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("refreshToken", response.refreshToken());
    }

    @Test
    void testAuthenticate_userNotFound() {
        when(userDAO.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.authenticate("unknown@example.com", "password"));
    }

    @Test
    void testAuthenticate_accountDisabled() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setMotDePasse("password");
        user.setActif(false);
        user.setEmailVerifie(true);

        when(userDAO.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class,
                () -> authService.authenticate("test@example.com", "password"));
    }

    @Test
    void testRegister_success() {
        when(userDAO.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(jwtService.generateAccessToken(any())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");

        AuthResponse response = authService.register("Nom", "Prenom", "new@example.com", "password");

        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("refreshToken", response.refreshToken());
        verify(userDAO, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testRegister_emailAlreadyUsed() {
        when(userDAO.existsByEmail("used@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("Nom", "Prenom", "used@example.com", "password"));
    }

    @Test
    void testVerifyEmail_success() {
        User user = new User();
        user.setEmailVerifie(false);
        user.setTokenVerification("token123");

        when(userDAO.findByTokenVerification("token123")).thenReturn(Optional.of(user));

        boolean result = authService.verifyEmail("token123");

        assertTrue(result);
        assertTrue(user.getEmailVerifie());
        assertNull(user.getTokenVerification());
        verify(userDAO, times(1)).save(user);
    }

    @Test
    void testVerifyEmail_invalidToken() {
        when(userDAO.findByTokenVerification("badToken")).thenReturn(Optional.empty());

        boolean result = authService.verifyEmail("badToken");

        assertFalse(result);
    }
}
