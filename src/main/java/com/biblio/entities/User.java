package com.biblio.entities;
import com.biblio.enums.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "user", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User  implements UserDetails {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @NotBlank(message = "Le nom est obligatoire")
	    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
	    @Column(nullable = false, length = 50)
	    private String nom;

	    @NotBlank(message = "Le prénom est obligatoire")
	    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
	    @Column(nullable = false, length = 50)
	    private String prenom;

	    @NotBlank(message = "L'email est obligatoire")
	    @Email(message = "Format d'email invalide")
	    @Column(nullable = false, unique = true, length = 100)
	    private String email;

	    @NotBlank(message = "Le mot de passe est obligatoire")
	    @Column(nullable = false)
	    private String motDePasse;

	    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{8,20}$", message = "Format de téléphone invalide")
	    @Column(length = 20)
	    private String telephone;

	    @Column(length = 200)
	    private String adresse;

	    @Column(nullable = false)
	    @Builder.Default
	    private LocalDateTime dateInscription = LocalDateTime.now();

	    @Column(nullable = false)
	    @Builder.Default
	    private Boolean emailVerifie = false;

	    @Column(length = 100)
	    private String tokenVerification;

	    @Column(nullable = false)
	    @Builder.Default
	    private Boolean actif = true;

	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false, length = 20)
	    @Builder.Default
	    private Role role = Role.USAGER;

	    @ManyToOne
	    @JoinColumn(name = "bibliotheque_id")
	    private Bibliotheque bibliotheque;

	    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
	    @Builder.Default
	    private Set<Pret> prets = new HashSet <> ();

	    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
	    @Builder.Default
	    private Set<Notification> notifications = new HashSet <> ();

	    @OneToMany(mappedBy = "createur")
	    @Builder.Default
	    private Set<Rapport> rapports = new HashSet <> ();

	    @Override
	    public Collection<? extends GrantedAuthority> getAuthorities() {
	    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
	    return motDePasse;
	}

	@Override
	public String getUsername() {
	    return email;
	}

	@Override
	public boolean isAccountNonExpired() {
	    return true;
	}

	@Override
	public boolean isAccountNonLocked() {
	    return actif;
	}

	@Override
	public boolean isCredentialsNonExpired() {
	    return true;
	}

	@Override
	public boolean isEnabled() {
	    return actif && emailVerifie;
	}

	    public String getNomComplet() {
	    return prenom + " " + nom;
	}

	    public boolean isUsager() {
	    return role == Role.USAGER;
	}

	    public boolean isBibliothecaire() {
	    return role == Role.BIBLIOTHECAIRE;
	}

	    public boolean isAdmin() {
	    return role == Role.ADMIN;
	}

}
