package com.biblio.services;

import com.biblio.dao.BibliothequeDAO;
import com.biblio.dao.RessourceDAO;
import com.biblio.dao.UserDAO;
import com.biblio.entities.Bibliotheque;
import com.biblio.entities.Ressource;
import com.biblio.entities.User;
import com.biblio.enums.Categorie;
import com.biblio.enums.Role;
import com.biblio.enums.TypeRessource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RessourceServiceTest {

    @Mock
    private RessourceDAO ressourceDAO;
    @Mock
    private BibliothequeDAO bibliothequeDAO;
    @Mock
    private UserDAO userDAO;

    private RessourceService ressourceService;

    @BeforeEach
    void setUp() {
        ressourceService = new RessourceService(ressourceDAO, bibliothequeDAO, userDAO);
    }

    @Test
    void createRessource_ShouldThrowException_WhenCapacityExceeded() {
        // Arrange
        String email = "biblio@test.com";
        Bibliotheque bibliotheque = Bibliotheque.builder()
                .id(1L)
                .nom("Test Lib")
                .capaciteStock(10)
                .build();
        
        User user = User.builder()
                .email(email)
                .role(Role.BIBLIOTHECAIRE)
                .bibliotheque(bibliotheque)
                .build();

        when(userDAO.findByEmail(email)).thenReturn(Optional.of(user));
        when(ressourceDAO.sumNombreExemplairesByBibliothequeId(1L)).thenReturn(8); // Current stock 8

        // Act & Assert
        // Try to add 3 copies, 8+3 = 11 > 10
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ressourceService.createRessource(
                    "Title", "Author", "ISBN123", Categorie.LITTERATURE, TypeRessource.LIVRE,
                    "Desc", "Editeur", LocalDate.now(), 3, 3, "img", email
            );
        });

        assertTrue(exception.getMessage().contains("dépasse la capacité de stockage"));
    }

    @Test
    void createRessource_ShouldSucceed_WhenCapacityNotExceeded() {
        // Arrange
        String email = "biblio@test.com";
        Bibliotheque bibliotheque = Bibliotheque.builder()
                .id(1L)
                .nom("Test Lib")
                .capaciteStock(10)
                .build();
        
        User user = User.builder()
                .email(email)
                .role(Role.BIBLIOTHECAIRE)
                .bibliotheque(bibliotheque)
                .build();

        when(userDAO.findByEmail(email)).thenReturn(Optional.of(user));
        when(ressourceDAO.sumNombreExemplairesByBibliothequeId(1L)).thenReturn(5); // Current stock 5
        when(ressourceDAO.save(any(Ressource.class))).thenAnswer(invocation -> {
            Ressource r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        // Act
        // Try to add 3 copies, 5+3 = 8 <= 10
        Ressource result = ressourceService.createRessource(
                "Title", "Author", "ISBN123", Categorie.LITTERATURE, TypeRessource.LIVRE,
                "Desc", "Editeur", LocalDate.now(), 3, 3, "img", email
        );

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getNombreExemplaires());
    }

    @Test
    void createRessource_ShouldSucceed_WhenCapacityIsNull() {
        // Arrange
        String email = "biblio@test.com";
        Bibliotheque bibliotheque = Bibliotheque.builder()
                .id(1L)
                .nom("Test Lib")
                .capaciteStock(null) // Unlimited
                .build();
        
        User user = User.builder()
                .email(email)
                .role(Role.BIBLIOTHECAIRE)
                .bibliotheque(bibliotheque)
                .build();

        when(userDAO.findByEmail(email)).thenReturn(Optional.of(user));
        // sum method might not be called if capacity is null, but if it is, it's fine.
        // Based on my implementation: "if (bibliotheque.getCapaciteStock() != null)" -> verify mock not called.
        
        when(ressourceDAO.save(any(Ressource.class))).thenAnswer(invocation -> {
            Ressource r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        // Act
        Ressource result = ressourceService.createRessource(
                "Title", "Author", "ISBN123", Categorie.LITTERATURE, TypeRessource.LIVRE,
                "Desc", "Editeur", LocalDate.now(), 1000, 1000, "img", email
        );

        // Assert
        assertNotNull(result);
        verify(ressourceDAO, never()).sumNombreExemplairesByBibliothequeId(any());
    }
}
