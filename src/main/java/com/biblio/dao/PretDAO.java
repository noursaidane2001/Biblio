package com.biblio.dao;

import com.biblio.entities.Pret;
import com.biblio.enums.StatutPret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PretDAO extends JpaRepository<Pret, Long> {
    List<Pret> findByUtilisateur_Id(Long utilisateurId);
    Optional<Pret> findFirstByUtilisateur_IdAndRessource_IdAndStatut(Long utilisateurId, Long ressourceId, StatutPret statut);
    List<Pret> findByBibliotheque_IdAndStatut(Long bibliothequeId, StatutPret statut);
    
    @Query("SELECT p FROM Pret p WHERE p.statut = :statut")
    List<Pret> findByStatut(@Param("statut") StatutPret statut);
    
    @Query("SELECT p FROM Pret p WHERE p.bibliotheque.id = :bibliothequeId AND p.statut = :statut")
    List<Pret> findByBibliothequeAndStatut(@Param("bibliothequeId") Long bibliothequeId, @Param("statut") StatutPret statut);
    
    @Query("SELECT p FROM Pret p WHERE p.utilisateur.id = :utilisateurId")
    List<Pret> findByUtilisateurId(@Param("utilisateurId") Long utilisateurId);
    
    @Query("SELECT p FROM Pret p WHERE p.utilisateur.id = :utilisateurId AND p.ressource.id = :ressourceId AND p.statut = :statut")
    Optional<Pret> findFirstByUtilisateurIdAndRessourceIdAndStatut(
            @Param("utilisateurId") Long utilisateurId,
            @Param("ressourceId") Long ressourceId,
            @Param("statut") StatutPret statut
    );

    @Query("SELECT COUNT(p) FROM Pret p WHERE p.utilisateur.id = :utilisateurId AND p.statut IN (:statuts)")
    long countActifsByUtilisateur(@Param("utilisateurId") Long utilisateurId,
                                  @Param("statuts") java.util.List<StatutPret> statuts);
}
