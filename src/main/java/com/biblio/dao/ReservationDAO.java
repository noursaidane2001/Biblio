package com.biblio.dao;

import com.biblio.entities.Reservation;
import com.biblio.enums.Categorie;
import com.biblio.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationDAO extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUsagerId(Long usagerId);

    @Query("SELECT r FROM Reservation r WHERE r.bibliotheque.id = :bibliothequeId AND r.statut = :statut")
    List<Reservation> findByBibliothequeAndStatut(@Param("bibliothequeId") Long bibliothequeId,
                                                  @Param("statut") StatutReservation statut);

    @Query("SELECT r FROM Reservation r WHERE r.statut IN (:statuts) AND r.dateExpiration IS NOT NULL AND r.dateExpiration < :now")
    List<Reservation> findExpired(@Param("statuts") List<StatutReservation> statuts,
                                  @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Reservation r SET r.statut = :statut WHERE r.id = :id")
    void updateStatut(@Param("id") Long id, @Param("statut") StatutReservation statut);

    @Query("SELECT r FROM Reservation r WHERE r.usager.id = :usagerId AND r.ressource.id = :ressourceId AND r.statut IN (:statuts)")
    List<Reservation> findByUsagerAndRessourceAndStatutIn(@Param("usagerId") Long usagerId,
                                                          @Param("ressourceId") Long ressourceId,
                                                          @Param("statuts") List<StatutReservation> statuts);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.usager.id = :usagerId AND r.statut IN (:statuts)")
    long countActivesByUsager(@Param("usagerId") Long usagerId, @Param("statuts") List<StatutReservation> statuts);

    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.usager.id = :usagerId " +
            "AND r.statut IN (:statuts) " +
            "AND LOWER(r.ressource.titre) = LOWER(:titre) " +
            "AND r.ressource.categorie = :categorie")
    long countDuplicateNomCategorie(@Param("usagerId") Long usagerId,
                                    @Param("titre") String titre,
                                    @Param("categorie") Categorie categorie,
                                    @Param("statuts") List<StatutReservation> statuts);

    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.usager.id = :usagerId " +
            "AND r.statut IN (:statuts) " +
            "AND r.ressource.isbn = :isbn")
    long countDuplicateIsbn(@Param("usagerId") Long usagerId,
                            @Param("isbn") String isbn,
                            @Param("statuts") List<StatutReservation> statuts);

    boolean existsByUsagerIdAndRessourceIdAndStatutIn(Long usagerId, Long ressourceId, List<StatutReservation> statuts);
}
