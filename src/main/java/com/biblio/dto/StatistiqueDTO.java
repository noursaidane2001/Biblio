package com.biblio.dto;

import java.time.LocalDate;

public record StatistiqueDTO(
        LocalDate date,
        String bibliotheque,
        String categorie,
        long nombrePrets,
        double tauxRotation
) {}

