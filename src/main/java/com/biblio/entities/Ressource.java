package com.biblio.entities;

import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.biblio.enums.Categorie;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ressources", indexes = {
    @Index(name = "idx_ressource_titre", columnList = "titre"),
    @Index(name = "idx_ressource_auteur", columnList = "auteur"),
    @Index(name = "idx_ressource_categorie", columnList = "categorie"),
    @Index(name = "idx_ressource_isbn", columnList = "isbn", unique = true)
})
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ressource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 1, max = 200, message = "Le titre doit contenir entre 1 et 200 caractères")
    @Column(nullable = false, length = 200)
    private String titre;

    @NotBlank(message = "L'auteur est obligatoire")
    @Size(min = 2, max = 100, message = "L'auteur doit contenir entre 2 et 100 caractères")
    @Column(nullable = false, length = 100)
    private String auteur;

    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
        message = "Format ISBN invalide")
    @Column(unique = true, length = 20)
    private String isbn;

    @NotNull(message = "La catégorie est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Categorie categorie;

    @NotNull(message = "Le type de ressource est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeRessource typeRessource;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 100, message = "Le nom de l'éditeur ne peut pas dépasser 100 caractères")
    @Column(length = 100)
    private String editeur;

    private LocalDate datePublication;

    @NotNull(message = "Le nombre d'exemplaires est obligatoire")
    @Min(value = 1, message = "Le nombre d'exemplaires doit être au moins 1")
    @Column(nullable = false)
    private Integer nombreExemplaires;

    @NotNull(message = "Le nombre d'exemplaires disponibles est obligatoire")
    @Min(value = 0, message = "Le nombre d'exemplaires disponibles ne peut pas être négatif")
    @Column(nullable = false)
    private Integer exemplairesDisponibles;

    @Column(length = 500)
    private String imageCouverture;

    @Min(value = 0, message = "La popularité ne peut pas être négative")
    @Column(nullable = false)
    @Builder.Default
    private Integer popularite = 0;

    @Column(nullable = false)
    private LocalDateTime dateAjout;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "bibliotheque_id", nullable = false)
    private Bibliotheque bibliotheque;

    @OneToMany(mappedBy = "ressource", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Pret> prets = new HashSet<>();

    // ==================== GETTERS ====================
    
    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getAuteur() {
        return auteur;
    }

    public String getIsbn() {
        return isbn;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public TypeRessource getTypeRessource() {
        return typeRessource;
    }

    public String getDescription() {
        return description;
    }

    public String getEditeur() {
        return editeur;
    }

    public LocalDate getDatePublication() {
        return datePublication;
    }

    public Integer getNombreExemplaires() {
        return nombreExemplaires;
    }

    public Integer getExemplairesDisponibles() {
        return exemplairesDisponibles;
    }

    public String getImageCouverture() {
        return imageCouverture;
    }

    public Integer getPopularite() {
        return popularite;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public Long getVersion() {
        return version;
    }

    public Bibliotheque getBibliotheque() {
        return bibliotheque;
    }

    public Set<Pret> getPrets() {
        return prets;
    }

    // ==================== LIFECYCLE HOOKS ====================

    @PrePersist
    private void onCreate() {
        if (dateAjout == null) {
            dateAjout = LocalDateTime.now();
        }
        if (popularite == null) {
            popularite = 0;
        }
        validateExemplaires();
    }

    @PreUpdate
    private void onUpdate() {
        validateExemplaires();
    }

    private void validateExemplaires() {
        if (exemplairesDisponibles > nombreExemplaires) {
            throw new IllegalStateException(
                "Le nombre d'exemplaires disponibles ne peut pas dépasser le nombre total d'exemplaires"
            );
        }
    }

    // ==================== BUSINESS METHODS ====================

    public boolean isDisponible() {
        return exemplairesDisponibles > 0;
    }

    public void emprunter() {
        if (!isDisponible()) {
            throw new IllegalStateException("Aucun exemplaire disponible pour cette ressource");
        }
        exemplairesDisponibles--;
    }

    public void retourner() {
        if (exemplairesDisponibles >= nombreExemplaires) {
            throw new IllegalStateException("Tous les exemplaires sont déjà disponibles");
        }
        exemplairesDisponibles++;
    }

    public void incrementerPopularite() {
        if (this.popularite == null) {
            this.popularite = 0;
        }
        this.popularite++;
    }
}