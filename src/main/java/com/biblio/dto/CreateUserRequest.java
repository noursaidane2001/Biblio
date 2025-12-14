package com.biblio.dto;

import com.biblio.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
        String nom,
        
        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
        String prenom,
        
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,
        
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
        String password,
        
        @NotNull(message = "Le rôle est obligatoire")
        Role role,
        
        Boolean emailVerifie,
        
        Boolean actif
) {
    public CreateUserRequest {
        if (emailVerifie == null) {
            emailVerifie = true; // Par défaut, l'email est vérifié pour les comptes créés par l'admin
        }
        if (actif == null) {
            actif = true; // Par défaut, le compte est actif
        }
    }
}
