package com.biblio.services;

import com.biblio.dao.BibliothequeDAO;
import com.biblio.dao.UserDAO;
import com.biblio.entities.Bibliotheque;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    // ===================== MOCKS =====================

    @Mock
    private UserDAO userDAO;

    @Mock
    private BibliothequeDAO bibliothequeDAO;

    @Mock
    private PasswordEncoder passwordEncoder;

    // ===================== SERVICE =====================
    @InjectMocks
    private AdminService adminService;

    // ===================== DONNÉES DE TEST =====================
    private User user;
    private Bibliotheque bibliotheque;

    @BeforeEach
    void setUp() {
        bibliotheque = new Bibliotheque();
        bibliotheque.setId(1L);
        bibliotheque.setNom("Bibliothèque Centrale");

        user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setActif(true);
        user.setRole(Role.ADMIN);
    }

    // ===================== TESTS =====================

    @Test
    void getAllUsers_shouldReturnUsersList() {
        when(userDAO.findAllOrderByDateInscriptionDesc())
                .thenReturn(List.of(user));

        List<User> result = adminService.getAllUsers();

        assertEquals(1, result.size());
        verify(userDAO).findAllOrderByDateInscriptionDesc();
    }

    @Test
    void getUserById_existingUser_shouldReturnUser() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(user));

        User result = adminService.getUserById(1L);

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    void getUserById_notFound_shouldThrowException() {
        when(userDAO.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> adminService.getUserById(99L));
    }

    @Test
    void createUser_validAdmin_shouldCreateUser() {
        when(userDAO.existsByEmail("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPwd");
        when(bibliothequeDAO.findById(1L)).thenReturn(Optional.of(bibliotheque));
        when(userDAO.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User createdUser = adminService.createUser(
                "Admin",
                "Test",
                "admin@test.com",
                "password123",
                Role.ADMIN,
                1L,
                true,
                true);

        assertEquals(Role.ADMIN, createdUser.getRole());
        assertEquals("admin@test.com", createdUser.getEmail());
        assertNotNull(createdUser.getBibliotheque());
    }

    @Test
    void createUser_existingEmail_shouldThrowException() {
        when(userDAO.existsByEmail("admin@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> adminService.createUser(
                "Admin",
                "Test",
                "admin@test.com",
                "password123",
                Role.ADMIN,
                1L,
                true,
                true));
    }

    @Test
    void deleteUser_existingUser_shouldDelete() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(user));

        adminService.deleteUser(1L);

        verify(userDAO).delete(user);
    }

    @Test
    void toggleUserStatus_shouldDeactivateUser() {
        when(userDAO.findById(1L)).thenReturn(Optional.of(user));
        when(userDAO.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = adminService.toggleUserStatus(1L);

        assertFalse(updated.getActif());
    }

    @Test
    void getSystemStats_shouldReturnCorrectCounts() {
        when(userDAO.count()).thenReturn(10L);
        when(userDAO.countByRole(Role.ADMIN)).thenReturn(2L);
        when(userDAO.countByRole(Role.BIBLIOTHECAIRE)).thenReturn(3L);
        when(userDAO.countByRole(Role.USAGER)).thenReturn(5L);
        when(userDAO.findAll()).thenReturn(List.of(user));

        var stats = adminService.getSystemStats();

        assertEquals(10L, stats.get("totalUsers"));
        assertEquals(2L, stats.get("admins"));
    }
}
