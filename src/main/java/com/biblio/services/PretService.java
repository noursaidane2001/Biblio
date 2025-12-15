package com.biblio.services;

import com.biblio.dao.PretDAO;
import com.biblio.dao.UserDAO;
import com.biblio.entities.Pret;
import com.biblio.entities.Reservation;
import com.biblio.entities.User;
import com.biblio.enums.StatutPret;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PretService {
    private final PretDAO pretDAO;
    private final UserDAO userDAO;

    public PretService(PretDAO pretDAO, UserDAO userDAO) {
        this.pretDAO = pretDAO;
        this.userDAO = userDAO;
    }

    @Transactional
    public Pret createFromReservation(Reservation reservation) {
        Pret pret = Pret.builder()
                .utilisateur(reservation.getUsager())
                .ressource(reservation.getRessource())
                .bibliotheque(reservation.getBibliotheque())
                .dureeEmprunt(15)
                .statut(StatutPret.RESERVE)
                .build();
        pret.reserver();
        return pretDAO.save(pret);
    }

    @Transactional
    public Optional<Pret> mettreEnCoursDepuisReservation(Reservation reservation) {
        Long userId = reservation.getUsager() != null ? reservation.getUsager().getId() : null;
        Long ressourceId = reservation.getRessource() != null ? reservation.getRessource().getId() : null;
        if (userId == null || ressourceId == null) {
            return Optional.empty();
        }
        Optional<Pret> opt = pretDAO.findFirstByUtilisateurIdAndRessourceIdAndStatut(userId, ressourceId, StatutPret.RESERVE);
        if (opt.isPresent()) {
            Pret pret = opt.get();
            pret.emprunter();
            pret.setDateRetourPrevu(java.time.LocalDate.now().plusDays(pret.getDureeEmprunt()));
            pret.mettreEnCours();
            pretDAO.save(pret);
            return Optional.of(pret);
        } else {
            Pret pret = Pret.builder()
                    .utilisateur(reservation.getUsager())
                    .ressource(reservation.getRessource())
                    .bibliotheque(reservation.getBibliotheque())
                    .dureeEmprunt(15)
                    .statut(StatutPret.RESERVE)
                    .build();
            pret.reserver();
            pret = pretDAO.save(pret);
            pret.emprunter();
            pret.setDateRetourPrevu(java.time.LocalDate.now().plusDays(pret.getDureeEmprunt()));
            pret.mettreEnCours();
            pret = pretDAO.save(pret);
            return Optional.of(pret);
        }
    }

    public List<Pret> getPretsForUser(String email) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        return pretDAO.findByUtilisateurId(user.getId());
    }

    @Transactional
    public Pret marquerEmprunte(Long pretId) {
        Pret pret = pretDAO.findById(pretId)
                .orElseThrow(() -> new IllegalArgumentException("Pret introuvable"));
        pret.emprunter();
        return pretDAO.save(pret);
    }

    @Transactional
    public Pret mettreEnCours(Long pretId) {
        Pret pret = pretDAO.findById(pretId)
                .orElseThrow(() -> new IllegalArgumentException("Pret introuvable"));
        pret.mettreEnCours();
        return pretDAO.save(pret);
    }

    @Transactional
    public Pret retourner(Long pretId) {
        Pret pret = pretDAO.findById(pretId)
                .orElseThrow(() -> new IllegalArgumentException("Pret introuvable"));
        pret.retourner();
        return pretDAO.save(pret);
    }

    @Transactional
    public Pret cloturer(Long pretId) {
        Pret pret = pretDAO.findById(pretId)
                .orElseThrow(() -> new IllegalArgumentException("Pret introuvable"));
        pret.cloturer();
        return pretDAO.save(pret);
    }
}
