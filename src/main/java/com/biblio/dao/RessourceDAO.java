package com.biblio.dao;

import com.biblio.entities.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface RessourceDAO extends JpaRepository<Ressource, Long> {
    boolean existsByIsbn(String isbn);
    List<Ressource> findByBibliothequeId(Long bibliothequeId);

    @Query("SELECT COALESCE(SUM(r.nombreExemplaires), 0) FROM Ressource r WHERE r.bibliotheque.id = :bibliothequeId")
    Integer sumNombreExemplairesByBibliothequeId(@Param("bibliothequeId") Long bibliothequeId);
}