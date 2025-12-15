package com.biblio.dao;

import com.biblio.entities.Reservation;
import com.biblio.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
}
