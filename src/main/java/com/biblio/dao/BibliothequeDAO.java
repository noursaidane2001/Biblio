package com.biblio.dao;

import com.biblio.entities.Bibliotheque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BibliothequeDAO extends JpaRepository<Bibliotheque, Long> {
    Optional<Bibliotheque> findByNom(String nom);
    
    boolean existsByNom(String nom);
    
    @Query("SELECT b FROM Bibliotheque b WHERE b.actif = true ORDER BY b.nom")
    List<Bibliotheque> findAllActives();
}
