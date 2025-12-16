package com.biblio.services;

import com.biblio.dao.ReservationDAO;
import com.biblio.dao.RessourceDAO;
import com.biblio.dao.UserDAO;
import com.biblio.entities.Bibliotheque;
import com.biblio.entities.Reservation;
import com.biblio.entities.Ressource;
import com.biblio.entities.User;
import com.biblio.enums.StatutReservation;
import com.biblio.enums.StatutPret;
import com.biblio.services.PretService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final int DEFAULT_RETRAIT_HEURES = 72;

    private final ReservationDAO reservationDAO;
    private final RessourceDAO ressourceDAO;
    private final UserDAO userDAO;
    private final EmailService emailService;
    private final PretService pretService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public ReservationService(ReservationDAO reservationDAO, RessourceDAO ressourceDAO, UserDAO userDAO, EmailService emailService,
                              org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
                              PretService pretService) {
        this.reservationDAO = reservationDAO;
        this.ressourceDAO = ressourceDAO;
        this.userDAO = userDAO;
        this.emailService = emailService;
        this.messagingTemplate = messagingTemplate;
        this.pretService = pretService;
    }

    @Transactional
    public Reservation creerReservation(Long ressourceId, String usagerEmail) {
        User usager = userDAO.findByEmail(usagerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usager introuvable"));
        Ressource ressource = ressourceDAO.findById(ressourceId)
                .orElseThrow(() -> new IllegalArgumentException("Ressource introuvable"));
        Bibliotheque bibliotheque = ressource.getBibliotheque();
        if (bibliotheque == null) {
            throw new IllegalStateException("La ressource n'est associée à aucune bibliothèque");
        }

        List<StatutReservation> statutsActifsReservation = List.of(StatutReservation.EN_ATTENTE);
        long dejaReserveMemeIsbn = (ressource.getIsbn() != null)
                ? reservationDAO.countDuplicateIsbn(usager.getId(), ressource.getIsbn(), statutsActifsReservation)
                : 0;

        // Fallback si pas d'ISBN ou si la méthode retourne 0 mais qu'on veut être sûr par ID
        if (dejaReserveMemeIsbn == 0) {
             boolean exists = reservationDAO.existsByUsagerIdAndRessourceIdAndStatutIn(
                usager.getId(), ressource.getId(), statutsActifsReservation);
             if (exists) dejaReserveMemeIsbn = 1;
        }

        if (dejaReserveMemeIsbn > 0) {
            throw new IllegalStateException("Une réservation en attente existe déjà pour cet ISBN");
        }
        long reservationsActives = reservationDAO.countActivesByUsager(usager.getId(), statutsActifsReservation);
        if (reservationsActives >= 2) {
            throw new IllegalStateException("Limite atteinte: vous avez déjà 2 réservations en attente.");
        }

        Reservation reservation = Reservation.builder()
                .ressource(ressource)
                .usager(usager)
                .bibliotheque(bibliotheque)
                .statut(StatutReservation.EN_ATTENTE)
                .dateDemande(LocalDateTime.now())
                .build();

        Reservation saved = reservationDAO.save(reservation);

        pretService.createFromReservation(saved);
        notifierCreation(usager, saved);
        notifierBibliothecaireNouvelle(bibliotheque, saved);
        pushReservationsEnAttente(bibliotheque.getId());

        logger.info("Réservation créée pour ressource {} par usager {}", ressourceId, usagerEmail);
        return saved;
    }

    public List<Reservation> listerReservationsUsager(String usagerEmail) {
        User usager = userDAO.findByEmail(usagerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usager introuvable"));
        return reservationDAO.findByUsagerId(usager.getId());
    }

    public List<Reservation> listerEnAttentePourBibliotheque(String emailBibliothecaire) {
        User bibliothecaire = chargerBibliothecaire(emailBibliothecaire);
        Bibliotheque biblio = bibliothecaire.getBibliotheque();
        if (biblio == null) {
            throw new IllegalStateException("Bibliothécaire sans bibliothèque associée");
        }
        return reservationDAO.findByBibliothequeAndStatut(biblio.getId(), StatutReservation.EN_ATTENTE);
    }

    public java.util.Optional<Reservation> trouverReservationLiee(Long usagerId, Long ressourceId) {
        List<StatutReservation> statuts = java.util.List.of(StatutReservation.CONFIRMEE, StatutReservation.EN_ATTENTE);
        List<Reservation> list = reservationDAO.findByUsagerAndRessourceAndStatutIn(usagerId, ressourceId, statuts);
        return list.stream().findFirst();
    }

    @Transactional
    public boolean annulerReservationLiee(Long usagerId, Long ressourceId) {
        java.util.Optional<Reservation> opt = trouverReservationLiee(usagerId, ressourceId);
        if (opt.isEmpty()) {
            return false;
        }
        Reservation reservation = opt.get();
        reservation.setStatut(StatutReservation.ANNULEE);
        if (reservation.isExemplaireVerrouille()) {
            Ressource r = reservation.getRessource();
            r.setExemplairesDisponibles((r.getExemplairesDisponibles() == null ? 0 : r.getExemplairesDisponibles()) + 1);
            reservation.setExemplaireVerrouille(false);
            ressourceDAO.save(r);
        }
        reservationDAO.save(reservation);
        pushReservationsEnAttente(reservation.getBibliotheque().getId());
        return true;
    }

    @Transactional
    public Reservation confirmerReservation(Long reservationId, String emailBibliothecaire, String commentaire) {
        Reservation reservation = chargerReservation(reservationId);
        User bibliothecaire = chargerBibliothecaire(emailBibliothecaire);
        validerMemeBibliotheque(reservation, bibliothecaire);

        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new IllegalStateException("Réservation non en attente");
        }

        Ressource ressource = reservation.getRessource();
        if (ressource.getExemplairesDisponibles() == null || ressource.getExemplairesDisponibles() <= 0) {
            throw new IllegalStateException("Aucun exemplaire disponible pour confirmer la réservation");
        }

        ressource.setExemplairesDisponibles(ressource.getExemplairesDisponibles() - 1);
        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservation.setDateConfirmation(LocalDateTime.now());
        reservation.setDeadlineRetrait(LocalDateTime.now().plusHours(DEFAULT_RETRAIT_HEURES));
        reservation.setDateExpiration(reservation.getDeadlineRetrait());
        reservation.setExemplaireVerrouille(true);
        reservation.setCommentaire(commentaire);

        reservationDAO.save(reservation);
        ressourceDAO.save(ressource);
        pretService.mettreEnCoursDepuisReservation(reservation);

        notifierConfirmation(reservation);
        pushReservationsEnAttente(bibliothecaire.getBibliotheque().getId());
        return reservation;
    }

    @Transactional
    public Reservation marquerEmprunte(Long reservationId, String emailBibliothecaire) {
        Reservation reservation = chargerReservation(reservationId);
        User bibliothecaire = chargerBibliothecaire(emailBibliothecaire);
        validerMemeBibliotheque(reservation, bibliothecaire);

        if (reservation.getStatut() != StatutReservation.CONFIRMEE) {
            throw new IllegalStateException("Seules les réservations confirmées peuvent être mises en emprunt en cours");
        }
        reservation.setStatut(StatutReservation.EMPRUNT_EN_COURS);
        reservationDAO.save(reservation);
        pushReservationsEnAttente(reservation.getBibliotheque().getId());
        return reservation;
    }

    @Transactional
    public Reservation annulerParUsager(Long reservationId, String usagerEmail) {
        Reservation reservation = chargerReservation(reservationId);
        User usager = userDAO.findByEmail(usagerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usager introuvable"));
        if (!reservation.getUsager().getId().equals(usager.getId())) {
            throw new IllegalArgumentException("Vous ne pouvez annuler que vos propres réservations");
        }
        if (reservation.getStatut() == StatutReservation.CONFIRMEE || reservation.getStatut() == StatutReservation.EN_ATTENTE) {
            reservation.setStatut(StatutReservation.ANNULEE);
            if (reservation.isExemplaireVerrouille()) {
                Ressource r = reservation.getRessource();
                r.setExemplairesDisponibles((r.getExemplairesDisponibles() == null ? 0 : r.getExemplairesDisponibles()) + 1);
                reservation.setExemplaireVerrouille(false);
                ressourceDAO.save(r);
            }
            reservationDAO.save(reservation);

            // Annuler le prêt lié si existant
            pretService.annulerPretLie(usager.getId(), reservation.getRessource().getId());

            pushReservationsEnAttente(reservation.getBibliotheque().getId());
            return reservation;
        } else {
            throw new IllegalStateException("La réservation ne peut pas être annulée dans son état actuel");
        }
    }

    @Transactional
    public Reservation rejeterReservation(Long reservationId, String emailBibliothecaire, String raison) {
        Reservation reservation = chargerReservation(reservationId);
        User bibliothecaire = chargerBibliothecaire(emailBibliothecaire);
        validerMemeBibliotheque(reservation, bibliothecaire);
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new IllegalStateException("Seules les réservations en attente peuvent être rejetées");
        }
        reservation.setStatut(StatutReservation.ANNULEE);
        reservation.setCommentaire(raison);
        if (reservation.isExemplaireVerrouille()) {
            Ressource r = reservation.getRessource();
            r.setExemplairesDisponibles((r.getExemplairesDisponibles() == null ? 0 : r.getExemplairesDisponibles()) + 1);
            reservation.setExemplaireVerrouille(false);
            ressourceDAO.save(r);
        }
        reservationDAO.save(reservation);

        // Annuler le prêt lié si existant (status RESERVE)
        pretService.annulerPretLie(reservation.getUsager().getId(), reservation.getRessource().getId());

        notifierRejet(reservation);
        pushReservationsEnAttente(reservation.getBibliotheque().getId());
        return reservation;
    }

    @Transactional
    public int expirerReservations() {
        List<Reservation> expirables = reservationDAO.findExpired(
                List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE),
                LocalDateTime.now()
        );
        int count = 0;
        for (Reservation res : expirables) {
            res.setStatut(StatutReservation.EXPIREE);
            if (res.isExemplaireVerrouille()) {
                Ressource r = res.getRessource();
                r.setExemplairesDisponibles(r.getExemplairesDisponibles() + 1);
                ressourceDAO.save(r);
            }
            reservationDAO.save(res);
            notifierExpiration(res);
            count++;
        }
        if (count > 0 && !expirables.isEmpty()) {
            pushReservationsEnAttente(expirables.get(0).getBibliotheque().getId());
        }
        return count;
    }

    private Reservation chargerReservation(Long id) {
        return reservationDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Réservation introuvable"));
    }

    private User chargerBibliothecaire(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        if (!user.isBibliothecaire() && !user.isAdmin() && !user.isSuperAdmin()) {
            throw new IllegalArgumentException("Accès réservé aux bibliothécaires ou admins");
        }
        return user;
    }

    private void validerMemeBibliotheque(Reservation reservation, User bibliothecaire) {
        if (bibliothecaire.getBibliotheque() == null ||
                bibliothecaire.getBibliotheque().getId() != reservation.getBibliotheque().getId()) {
            throw new IllegalArgumentException("Réservation hors de votre bibliothèque");
        }
    }

    private void notifierCreation(User usager, Reservation reservation) {
        try {
            emailService.sendVerificationEmail(usager.getEmail(), usager.getNom(), usager.getPrenom(),
                    "reservation-" + reservation.getId());
        } catch (Exception e) {
            logger.warn("Notification creation reservation échouée pour {}", usager.getEmail(), e);
        }
    }

    private void notifierBibliothecaireNouvelle(Bibliotheque bibliotheque, Reservation reservation) {
        // TODO: Implémenter une vraie notification (email aux bibliothécaires de la bibliothèque)
        logger.info("Nouvelle réservation à confirmer pour la bibliothèque {}", bibliotheque.getNom());
    }

    private void notifierConfirmation(Reservation reservation) {
        try {
            String titre = reservation.getRessource() != null ? reservation.getRessource().getTitre() : null;
            String deadline = reservation.getDeadlineRetrait() != null ? reservation.getDeadlineRetrait().toString() : null;
            emailService.sendReservationConfirmationEmail(
                    reservation.getUsager().getEmail(),
                    reservation.getUsager().getNom(),
                    reservation.getUsager().getPrenom(),
                    titre,
                    deadline
            );
        } catch (Exception e) {
            logger.warn("Notification confirmation échouée pour {}", reservation.getUsager().getEmail(), e);
        }
    }

    private void notifierRejet(Reservation reservation) {
        logger.info("Réservation {} rejetée", reservation.getId());
    }

    private void notifierExpiration(Reservation reservation) {
        logger.info("Réservation {} expirée", reservation.getId());
    }

    private void pushReservationsEnAttente(Long bibliothequeId) {
        try {
            int totalGlobal = 0;
            if (bibliothequeId != null) {
                totalGlobal = reservationDAO.findByBibliothequeAndStatut(bibliothequeId, StatutReservation.EN_ATTENTE).size();
                messagingTemplate.convertAndSend("/topic/reservations/en-attente/" + bibliothequeId, totalGlobal);
            }
            // Topic global pour tous les bibliothécaires/admins
            messagingTemplate.convertAndSend("/topic/reservations/en-attente", totalGlobal);
        } catch (Exception e) {
            logger.warn("pushReservationsEnAttente failed: {}", e.getMessage());
        }
    }
}
