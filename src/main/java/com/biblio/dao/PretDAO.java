package com.biblio.dao;

import com.biblio.entities.Pret;
import com.biblio.enums.StatutPret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PretDAO extends JpaRepository<Pret, Long> {
    List<Pret> findByUtilisateurId(Long utilisateurId);
    Optional<Pret> findFirstByUtilisateurIdAndRessourceIdAndStatut(Long utilisateurId, Long ressourceId, StatutPret statut);
}
