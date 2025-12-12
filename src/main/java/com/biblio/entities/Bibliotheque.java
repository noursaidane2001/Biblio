package com.biblio.entities;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
/*import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;*/
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bibliotheques", indexes = {@Index(name = "idx_bibliotheque_ville", columnList = "ville")})
@Builder
public class Bibliotheque {
	  @Id
	  private Long id;
	  @NotBlank(message = "Le nom de la bibliothèque est obligatoire")
	  @Column(nullable = false, unique = true, length = 100)
	    private String nom;
      @NotBlank(message = "L'adresse est obligatoire")
	  @Column(nullable = false)
	    private String adresse;

	  @NotBlank(message = "La ville est obligatoire")
	    @Column(nullable = false, length = 100)
	    private String ville;

	    @NotBlank(message = "Le code postal est obligatoire")
	    @Pattern(regexp = "\\d{4,5}", message = "Format de code postal invalide")
	    @Column(nullable = false, length = 10)
	    private String codePostal;

	    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{8,20}$", message = "Format de téléphone invalide")
	    @Column(length = 20)
	    private String telephone;

	    @Email(message = "Format d'email invalide")
	    @Column(length = 100)
	    private String email;

	    @Column(length = 200)
	    private String heuresOuverture;

	    @Min(value = 0, message = "La capacité ne peut pas être négative")
	    private Integer capaciteStock;

	    @Column(nullable = false)
	    @Builder.Default
	    private Boolean actif = true;

	    @OneToMany(mappedBy = "bibliotheque", cascade = CascadeType.ALL, orphanRemoval = true)
	    @Builder.Default
	    private Set<Ressource> ressources = new HashSet<>();

	    @OneToMany(mappedBy = "bibliotheque")
	    @Builder.Default
	    private Set<User> bibliothecaires = new HashSet<>();

	    @OneToMany(mappedBy = "bibliotheque")
	    @Builder.Default
	    private Set<Pret> prets = new HashSet<>();

	 /*   public void addRessource(Ressource ressource) {
	        ressources.add(ressource);
	        ressource.setBibliotheque(this);
	    }

	    public void removeRessource(Ressource ressource) {
	        ressources.remove(ressource);
	        ressource.setBibliotheque(null);
	    }
*/
}
