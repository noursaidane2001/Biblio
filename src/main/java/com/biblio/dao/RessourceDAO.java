package com.biblio.dao;

import com.biblio.entities.Bibliotheque;
import com.biblio.entities.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RessourceDAO extends JpaRepository<Ressource, Long> {
    Optional<Ressource> findByIsbn(String isbn);
    
    boolean existsByIsbn(String isbn);
    
    List<Ressource> findByBibliotheque(Bibliotheque bibliotheque);
    
    @Query("SELECT r FROM Ressource r WHERE r.bibliotheque.id = :bibliothequeId")
    List<Ressource> findByBibliothequeId(@Param("bibliothequeId") Long bibliothequeId);
}
