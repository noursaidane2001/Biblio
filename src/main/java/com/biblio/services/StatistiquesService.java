package com.biblio.services;

import com.biblio.dao.PretDAO;
import com.biblio.dao.RessourceDAO;
import com.biblio.dto.StatistiqueDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatistiquesService {
    private final PretDAO pretDAO;
    private final RessourceDAO ressourceDAO;

    public StatistiquesService(PretDAO pretDAO, RessourceDAO ressourceDAO) {
        this.pretDAO = pretDAO;
        this.ressourceDAO = ressourceDAO;
    }

    public List<StatistiqueDTO> getToutesLesStatistiques() {
        List<StatistiqueDTO> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        long totalPrets = pretDAO.count();
        Integer totalStock = ressourceDAO.sumTotalExemplaires();
        double tauxRotationGlobal = 0.0;
        if (totalStock != null && totalStock > 0) {
            tauxRotationGlobal = (double) totalPrets / totalStock * 100.0;
        }

        List<Object[]> pretsByCategorie = pretDAO.countPretsByCategorie();
        for (Object[] row : pretsByCategorie) {
            String categorie = row[0] != null ? row[0].toString() : "-";
            long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            list.add(new StatistiqueDTO(today, "-", categorie, count, tauxRotationGlobal));
        }

        List<Object[]> pretsByBibliotheque = pretDAO.countPretsByBibliotheque();
        for (Object[] row : pretsByBibliotheque) {
            String bibliotheque = row[0] != null ? row[0].toString() : "-";
            long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            list.add(new StatistiqueDTO(today, bibliotheque, "-", count, tauxRotationGlobal));
        }

        return list;
    }

    public Double getTauxRotationGlobal() {
        long totalPrets = pretDAO.count();
        Integer totalStock = ressourceDAO.sumTotalExemplaires();
        if (totalStock == null || totalStock <= 0) return 0.0;
        return (double) totalPrets / totalStock * 100.0;
    }
}

