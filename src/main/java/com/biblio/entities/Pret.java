package com.biblio.entities;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import com.biblio.enums.Categorie;
import com.biblio.enums.StatutPret;
import com.biblio.enums.TypeRessource;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prets", indexes = {
    @Index(name = "idx_pret_statut", columnList = "statut"),
    @Index(name = "idx_pret_date_retour", columnList = "dateRetourPrevu")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateReservation;

    private LocalDateTime dateEmprunt;

    @NotNull(message = "La date de retour prévue est obligatoire")
    @Column(nullable = false)
    private LocalDate dateRetourPrevu;

    private LocalDateTime dateRetourEffectif;

    @NotNull(message = "Le statut est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutPret statut = StatutPret.RESERVE;

    @Min(value = 1, message = "La durée d'emprunt doit être au moins 1 jour")
    @Max(value = 90, message = "La durée d'emprunt ne peut pas dépasser 90 jours")
    @Column(nullable = false)
    @Builder.Default
    private Integer dureeEmprunt = 14;

    @Min(value = 0, message = "Le nombre de prolongations ne peut pas être négatif")
    @Max(value = 2, message = "Maximum 2 prolongations autorisées")
    @Column(nullable = false)
    @Builder.Default
    private Integer prolongations = 0;

    @DecimalMin(value = "0.0", message = "La pénalité ne peut pas être négative")
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal penaliteRetard = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String feedbackUsager;

    @Min(value = 1, message = "La note minimum est 1")
    @Max(value = 5, message = "La note maximum est 5")
    private Integer noteUsager;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;

    @ManyToOne
    @JoinColumn(name = "ressource_id", nullable = false)
    private Ressource ressource;

    @ManyToOne
    @JoinColumn(name = "bibliotheque_id", nullable = false)
    private Bibliotheque bibliotheque;

    @OneToMany(mappedBy = "pret", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Notification> notifications = new HashSet <> ();

    @PrePersist
    private void prePersist() {
        if (dateReservation == null) {
            dateReservation = LocalDateTime.now();
        }
        if (dateRetourPrevu == null && dureeEmprunt != null) {
            dateRetourPrevu = LocalDate.now().plusDays(dureeEmprunt);
        }
    }

    public void reserver() {
        if (statut != StatutPret.RESERVE) {
            throw new IllegalStateException("Ce prêt ne peut pas être réservé dans son état actuel");
        }
        this.dateReservation = LocalDateTime.now();
        this.dateRetourPrevu = LocalDate.now().plusDays(dureeEmprunt);
    }

    public void emprunter() {
        if (statut != StatutPret.RESERVE && statut != StatutPret.EMPRUNTE) {
            throw new IllegalStateException("Ce prêt ne peut pas passer à l'état emprunté");
        }
        this.statut = StatutPret.EMPRUNTE;
        this.dateEmprunt = LocalDateTime.now();
        if (dateRetourPrevu == null) {
            this.dateRetourPrevu = LocalDate.now().plusDays(dureeEmprunt);
        }
    }

    public void mettreEnCours() {
        if (statut != StatutPret.EMPRUNTE) {
            throw new IllegalStateException("Seul un prêt emprunté peut être mis en cours");
        }
        this.statut = StatutPret.EN_COURS;
    }

    public void retourner() {
        if (statut != StatutPret.EN_COURS && statut != StatutPret.EMPRUNTE) {
            throw new IllegalStateException("Ce prêt ne peut pas être retourné dans son état actuel");
        }
        this.statut = StatutPret.RETOURNE;
        this.dateRetourEffectif = LocalDateTime.now();
        calculerPenalite();
    }

    public void cloturer() {
        if (statut != StatutPret.RETOURNE) {
            throw new IllegalStateException("Seul un prêt retourné peut être clôturé");
        }
        this.statut = StatutPret.CLOTURE;
    }

    public void annuler() {
        if (statut == StatutPret.CLOTURE) {
            throw new IllegalStateException("Un prêt clôturé ne peut pas être annulé");
        }
        this.statut = StatutPret.ANNULE;
    }

    public boolean peutEtreProlonge() {
        return statut.isActive() && prolongations < 2 && !estEnRetard();
    }

    public void prolonger(int jours) {
        if (!peutEtreProlonge()) {
            throw new IllegalStateException("Ce prêt ne peut pas être prolongé");
        }
        this.dateRetourPrevu = dateRetourPrevu.plusDays(jours);
        this.prolongations++;
    }

    public boolean estEnRetard() {
        return dateRetourPrevu != null && LocalDate.now().isAfter(dateRetourPrevu)
            && dateRetourEffectif == null;
    }

    public long getJoursRetard() {
        if (!estEnRetard()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dateRetourPrevu, LocalDate.now());
    }

    private void calculerPenalite() {
        if (dateRetourEffectif != null && dateRetourPrevu != null) {
            LocalDate dateRetour = dateRetourEffectif.toLocalDate();
            if (dateRetour.isAfter(dateRetourPrevu)) {
                long joursRetard = ChronoUnit.DAYS.between(dateRetourPrevu, dateRetour);
                this.penaliteRetard = BigDecimal.valueOf(joursRetard * 0.50);
            }
        }
    }

    public boolean isActif() {
        return statut.isActive();
    }
}
