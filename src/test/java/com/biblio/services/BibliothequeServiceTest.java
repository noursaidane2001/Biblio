package com.biblio.services;

import com.biblio.dao.BibliothequeDAO;
import com.biblio.entities.Bibliotheque;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BibliothequeServiceTest {

    @Mock
    private BibliothequeDAO bibliothequeDAO;

    @InjectMocks
    private BibliothequeService bibliothequeService;

    private Bibliotheque bibliotheque;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        bibliotheque = Bibliotheque.builder()
                .id(1L)
                .nom("Bibliothèque Centrale")
                .adresse("Rue Principale")
                .ville("Tunis")
                .telephone("+21612345678")
                .capaciteStock(100)
                .actif(true)
                .latitude(36.8)
                .longitude(10.2)
                .build();
    }

    @Test
    void testGetAllActives() {
        when(bibliothequeDAO.findAllActives()).thenReturn(List.of(bibliotheque));

        List<Bibliotheque> result = bibliothequeService.getAllActives();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bibliothequeDAO, times(1)).findAllActives();
    }

    @Test
    void testGetAll() {
        when(bibliothequeDAO.findAll()).thenReturn(List.of(bibliotheque));

        List<Bibliotheque> result = bibliothequeService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bibliothequeDAO, times(1)).findAll();
    }

    @Test
    void testGetAllPaged() {
        Page<Bibliotheque> page = new PageImpl<>(List.of(bibliotheque));
        when(bibliothequeDAO.findAll(any(Pageable.class))).thenReturn(page);

        Page<Bibliotheque> result = bibliothequeService.getAllPaged(Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(bibliothequeDAO, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetById_Found() {
        when(bibliothequeDAO.findById(1L)).thenReturn(Optional.of(bibliotheque));

        Bibliotheque result = bibliothequeService.getById(1L);

        assertNotNull(result);
        assertEquals("Bibliothèque Centrale", result.getNom());
    }

    @Test
    void testGetById_NotFound() {
        when(bibliothequeDAO.findById(2L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> bibliothequeService.getById(2L));
        assertEquals("Bibliothèque non trouvée avec l'ID: 2", exception.getMessage());
    }

    @Test
    void testCreateBibliotheque_Success() {
        when(bibliothequeDAO.existsByNom(anyString())).thenReturn(false);
        when(bibliothequeDAO.save(any(Bibliotheque.class))).thenReturn(bibliotheque);

        Bibliotheque result = bibliothequeService.createBibliotheque(
                "Bibliothèque Centrale", "Rue Principale", "Tunis",
                "+21612345678", 100, 36.8, 10.2
        );

        assertNotNull(result);
        assertEquals("Bibliothèque Centrale", result.getNom());

        ArgumentCaptor<Bibliotheque> captor = ArgumentCaptor.forClass(Bibliotheque.class);
        verify(bibliothequeDAO).save(captor.capture());
        assertEquals("Tunis", captor.getValue().getVille());
    }

    @Test
    void testCreateBibliotheque_DuplicateName() {
        when(bibliothequeDAO.existsByNom("Bibliothèque Centrale")).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                bibliothequeService.createBibliotheque("Bibliothèque Centrale", "Rue Principale", "Tunis",
                        "+21612345678", 100, 36.8, 10.2)
        );

        assertEquals("Une bibliothèque avec ce nom existe déjà", exception.getMessage());
    }

    @Test
    void testUpdateBibliotheque_Success() {
        when(bibliothequeDAO.findById(1L)).thenReturn(Optional.of(bibliotheque));
        when(bibliothequeDAO.existsByNom("Nouvelle Bibliothèque")).thenReturn(false);
        when(bibliothequeDAO.save(any(Bibliotheque.class))).thenReturn(bibliotheque);

        Bibliotheque result = bibliothequeService.updateBibliotheque(
                1L, "Nouvelle Bibliothèque", "Nouvelle Rue", "Sousse",
                "+21698765432", 200, true, 36.9, 10.3
        );

        assertNotNull(result);
        verify(bibliothequeDAO).save(any(Bibliotheque.class));
    }

    @Test
    void testDeleteBibliotheque_Success() {
        when(bibliothequeDAO.findById(1L)).thenReturn(Optional.of(bibliotheque));

        bibliothequeService.deleteBibliotheque(1L);

        verify(bibliothequeDAO).delete(bibliotheque);
    }

    @Test
    void testDeleteBibliotheque_NotFound() {
        when(bibliothequeDAO.findById(2L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                bibliothequeService.deleteBibliotheque(2L)
        );

        assertEquals("Bibliothèque non trouvée avec l'ID: 2", exception.getMessage());
    }
}
