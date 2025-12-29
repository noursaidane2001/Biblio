package com.biblio.services;

import com.biblio.dao.PretDAO;
import com.biblio.dao.RessourceDAO;
import com.biblio.dao.UserDAO;
import com.biblio.entities.Pret;
import com.biblio.entities.Reservation;
import com.biblio.entities.Ressource;
import com.biblio.entities.User;
import com.biblio.enums.StatutPret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PretServiceTest {

    @Mock
    private PretDAO pretDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private ReservationService reservationService;

    @Mock
    private RessourceDAO ressourceDAO;

    @InjectMocks
    private PretService pretService;

    private User user;
    private Ressource ressource;
    private Reservation reservation;
    private Pret pret;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");

        ressource = new Ressource();
        ressource.setId(1L);

        reservation = Reservation.builder()
                .usager(user)
                .ressource(ressource)
                .build();

        pret = Pret.builder()
                .id(1L)
                .utilisateur(user)
                .ressource(ressource)
                .statut(StatutPret.RESERVE)
                .dureeEmprunt(15)
                .build();
    }

    @Test
    void createFromReservation_shouldSavePret() {
        when(pretDAO.save(any(Pret.class))).thenAnswer(i -> i.getArgument(0));
        when(ressourceDAO.save(any(Ressource.class))).thenAnswer(i -> i.getArgument(0));

        Pret result = pretService.createFromReservation(reservation);

        assertEquals(user, result.getUtilisateur());
        assertEquals(StatutPret.RESERVE, result.getStatut());
        verify(pretDAO).save(any(Pret.class));
        verify(ressourceDAO).save(any(Ressource.class));
    }

    @Test
    void mettreEnCoursDepuisReservation_existingPret_shouldReturnPret() {
        when(pretDAO.findFirstByUtilisateur_IdAndRessource_IdAndStatut(
                eq(user.getId()), eq(ressource.getId()), eq(StatutPret.RESERVE)))
                .thenReturn(Optional.of(pret));
        when(pretDAO.save(any(Pret.class))).thenAnswer(i -> i.getArgument(0));

        Optional<Pret> result = pretService.mettreEnCoursDepuisReservation(reservation);

        assertTrue(result.isPresent());
        assertEquals(StatutPret.EMPRUNTE, result.get().getStatut());
    }

    @Test
    void annulerPret_shouldCancelPretAndReservation() {
        when(pretDAO.findById(pret.getId())).thenReturn(Optional.of(pret));
        when(pretDAO.save(any(Pret.class))).thenAnswer(i -> i.getArgument(0));

        Pret result = pretService.annulerPret(pret.getId());

        assertEquals(StatutPret.ANNULE, result.getStatut());
        verify(reservationService).annulerReservationLiee(user.getId(), ressource.getId());
    }

    @Test
    void ajouterFeedbackUsager_validFeedback_shouldSaveFeedback() {
        when(pretDAO.findById(pret.getId())).thenReturn(Optional.of(pret));
        when(userDAO.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(pretDAO.save(any(Pret.class))).thenAnswer(i -> i.getArgument(0));

        Pret result = pretService.ajouterFeedbackUsager(
                pret.getId(), "Super lecture", 5, "user@test.com");

        assertEquals("Super lecture", result.getFeedbackUsager());
        assertEquals(5, result.getNoteUsager());
    }

    @Test
    void retourner_shouldIncreaseRessourceDisponibles() {
        // Le prêt doit être en EMPRUNTE pour pouvoir être retourné
        pret.setStatut(StatutPret.EMPRUNTE);

        when(pretDAO.findById(pret.getId())).thenReturn(Optional.of(pret));
        when(pretDAO.save(any(Pret.class))).thenAnswer(i -> i.getArgument(0));
        when(ressourceDAO.save(any(Ressource.class))).thenAnswer(i -> i.getArgument(0));
        ressource.setExemplairesDisponibles(2);
        pret.setRessource(ressource);

        Pret result = pretService.retourner(pret.getId());

        assertEquals(3, result.getRessource().getExemplairesDisponibles());
        verify(ressourceDAO).save(ressource);
    }
}
