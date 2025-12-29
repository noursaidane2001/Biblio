package com.biblio.services;

import com.biblio.dao.*;
import com.biblio.entities.*;
import com.biblio.enums.StatutPret;
import com.biblio.enums.StatutReservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationDAO reservationDAO;
    @Mock
    private RessourceDAO ressourceDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private EmailService emailService;
    @Mock
    private PretService pretService;
    @Mock
    private PretDAO pretDAO;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ReservationService reservationService;

    private User usager;
    private Ressource ressource;
    private Bibliotheque bibliotheque;

    @BeforeEach
    void setup() {
        bibliotheque = new Bibliotheque();
        bibliotheque.setId(1L);
        bibliotheque.setNom("Bibliothèque Centrale");

        usager = new User();
        usager.setId(10L);
        usager.setEmail("user@test.com");
        usager.setNom("Ahmed");
        usager.setPrenom("Test");

        ressource = new Ressource();
        ressource.setId(5L);
        ressource.setTitre("Clean Code");
        ressource.setIsbn("123456");
        ressource.setBibliotheque(bibliotheque);
    }

    @Test
    void creerReservation_OK() {
        // GIVEN (préconditions)
        when(userDAO.findByEmail("user@test.com"))
                .thenReturn(Optional.of(usager));

        when(ressourceDAO.findById(5L))
                .thenReturn(Optional.of(ressource));

        when(pretDAO.countActifsByUtilisateur(usager.getId(), List.of(StatutPret.BLOQUE)))
                .thenReturn(0L);

        when(reservationDAO.countDuplicateIsbn(
                usager.getId(),
                ressource.getIsbn(),
                List.of(StatutReservation.EN_ATTENTE)))
                .thenReturn(0L);

        when(reservationDAO.existsByUsagerIdAndRessourceIdAndStatutIn(
                usager.getId(),
                ressource.getId(),
                List.of(StatutReservation.EN_ATTENTE)))
                .thenReturn(false);

        when(reservationDAO.countActivesByUsager(
                usager.getId(),
                List.of(StatutReservation.EN_ATTENTE)))
                .thenReturn(0L);

        when(reservationDAO.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN (action)
        Reservation reservation = reservationService.creerReservation(
                ressource.getId(),
                usager.getEmail()
        );

        // THEN (vérifications)
        assertNotNull(reservation);
        assertEquals(StatutReservation.EN_ATTENTE, reservation.getStatut());
        assertEquals(usager, reservation.getUsager());
        assertEquals(ressource, reservation.getRessource());

        verify(reservationDAO, times(1)).save(any(Reservation.class));
        verify(pretService, times(1)).createFromReservation(any(Reservation.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), ArgumentMatchers.<Object>any());
    }
}
