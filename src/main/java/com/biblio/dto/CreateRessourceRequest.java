package com.biblio.dto;

import com.biblio.enums.Categorie;
import com.biblio.enums.TypeRessource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRessourceRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 1, max = 200, message = "Le titre doit contenir entre 1 et 200 caractères")
        String titre,
        
        @NotBlank(message = "L'auteur est obligatoire")
        @Size(min = 2, max = 100, message = "L'auteur doit contenir entre 2 et 100 caractères")
        String auteur,
        
        @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
                message = "Format ISBN invalide")
        String isbn,
        
        @NotNull(message = "La catégorie est obligatoire")
        Categorie categorie,
        
        @NotNull(message = "Le type de ressource est obligatoire")
        TypeRessource typeRessource,
        
        String description,
        
        @Size(max = 100, message = "Le nom de l'éditeur ne peut pas dépasser 100 caractères")
        String editeur,
        
        LocalDate datePublication,
        
        @NotNull(message = "Le nombre d'exemplaires est obligatoire")
        @Min(value = 1, message = "Le nombre d'exemplaires doit être au moins 1")
        Integer nombreExemplaires,
        
        @NotNull(message = "Le nombre d'exemplaires disponibles est obligatoire")
        @Min(value = 0, message = "Le nombre d'exemplaires disponibles ne peut pas être négatif")
        Integer exemplairesDisponibles,
        
        String imageCouverture
) {
}
