package com.biblio.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBibliothequeRequest(
        @NotBlank(message = "Le nom de la bibliothèque est obligatoire")
        String nom,
        
        @NotBlank(message = "L'adresse est obligatoire")
        String adresse,
        
        @NotBlank(message = "La ville est obligatoire")
        String ville,
        
        @Pattern(regexp = "^$|^\\+?[0-9\\s\\-()]{8,20}$", message = "Format de téléphone invalide")
        String telephone,
        
        @Min(value = 0, message = "La capacité ne peut pas être négative")
        Integer capaciteStock
) {
    public CreateBibliothequeRequest {
        // Normaliser les chaînes vides en null
        if (telephone != null && telephone.trim().isEmpty()) {
            telephone = null;
        }
    }
}
