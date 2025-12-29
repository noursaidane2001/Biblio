package com.biblio.services;

import com.biblio.dao.UserLogDAO;
import com.biblio.entities.Bibliotheque;
import com.biblio.entities.User;
import com.biblio.entities.UserLog;
import com.biblio.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLogServiceTest {

    // ===================== MOCK =====================
    @Mock
    private UserLogDAO userLogDAO;

    // ===================== SERVICE =====================
    @InjectMocks
    private UserLogService userLogService;

    // ===================== DONNÉES DE TEST =====================
    private User user;
    private UserLog userLog;

    @BeforeEach
    void setUp() {
        Bibliotheque bibliotheque = new Bibliotheque();
        bibliotheque.setId(1L);
        bibliotheque.setNom("Bibliothèque Centrale");

        user = new User();
        user.setId(10L);
        user.setEmail("user@test.com");
        user.setRole(Role.ADMIN);
        user.setBibliotheque(bibliotheque);

        userLog = UserLog.builder()
                .id(1L)
                .utilisateur(user)
                .action("LOGIN")
                .message("Connexion réussie")
                .level("INFO")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===================== TESTS =====================

    @Test
    void log_shouldSaveUserLog() {
        userLogService.log(user, "LOGIN", "Connexion réussie", "INFO");

        verify(userLogDAO, times(1)).save(any(UserLog.class));
    }

    @Test
    void log_withoutLevel_shouldDefaultToINFO() {
        userLogService.log(user, "LOGIN", "Connexion réussie", null);

        verify(userLogDAO).save(argThat(log -> log.getLevel().equals("INFO")));
    }

    @Test
    void getRecentLogs_withoutUserId_shouldReturnLogs() {
        Page<UserLog> page = new PageImpl<>(List.of(userLog));
        when(userLogDAO.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page);

        List<Map<String, Object>> result = userLogService.getRecentLogs(10, null);

        assertEquals(1, result.size());
        assertEquals("Connexion réussie", result.get(0).get("message"));
        assertEquals("user@test.com", result.get(0).get("user"));
    }

    @Test
    void getRecentLogs_withUserId_shouldReturnUserLogs() {
        Page<UserLog> page = new PageImpl<>(List.of(userLog));
        when(userLogDAO.findByUtilisateur_IdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(page);

        List<Map<String, Object>> result = userLogService.getRecentLogs(5, 10L);

        assertEquals(1, result.size());
        assertEquals("LOGIN", result.get(0).get("action"));
        assertEquals("ADMIN", result.get(0).get("role"));
        assertEquals("Bibliothèque Centrale", result.get(0).get("bibliotheque"));
    }

    @Test
    void getRecentLogs_limitTooHigh_shouldCapAt200() {
        Page<UserLog> page = new PageImpl<>(List.of(userLog));
        when(userLogDAO.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page);

        List<Map<String, Object>> result = userLogService.getRecentLogs(1000, null);

        assertEquals(1, result.size());
        verify(userLogDAO).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }
}
