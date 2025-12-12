package com.biblio.entities;
import com.biblio.enums.FormatExport;
import com.biblio.enums.TypeRapport;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "rapports", indexes = {
    @Index(name = "idx_rapport_type", columnList = "typeRapport"),
    @Index(name = "idx_rapport_date", columnList = "dateGeneration")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rapport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre du rapport est obligatoire")
    @Size(min = 5, max = 200, message = "Le titre doit contenir entre 5 et 200 caractères")
    @Column(nullable = false, length = 200)
    private String titre;

    @NotNull(message = "Le type de rapport est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TypeRapport typeRapport;

    @NotNull(message = "La date de génération est obligatoire")
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dateGeneration = LocalDateTime.now();

    @NotNull(message = "La date de début de période est obligatoire")
    @Column(nullable = false)
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin de période est obligatoire")
    @Column(nullable = false)
    private LocalDate dateFin;

    @NotBlank(message = "Les données JSON sont obligatoires")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String donneesJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private FormatExport formatExport;

    @Column(length = 500)
    private String cheminFichier;

    @ManyToOne
    @JoinColumn(name = "createur_id", nullable = false)
    private User createur;

    @ManyToOne
    @JoinColumn(name = "bibliotheque_id")
    private Bibliotheque bibliotheque;

    @PrePersist
    @PreUpdate
    private void validateDates() {
        if (dateFin != null && dateDebut != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalStateException(
                "La date de fin ne peut pas être antérieure à la date de début"
            );
        }
    }

    public boolean estRecent() {
        return dateGeneration.isAfter(LocalDateTime.now().minusDays(30));
    }

    public boolean aFichierExporte() {
        return cheminFichier != null && !cheminFichier.isEmpty();
    }

    public String getNomFichier() {
        if (cheminFichier != null) {
            return cheminFichier.substring(cheminFichier.lastIndexOf("/") + 1);
        }
        return null;
    }

    public long getDureePeriodeJours() {
        if (dateDebut != null && dateFin != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin);
        }
        return 0;
    }
}