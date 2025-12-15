package com.biblio.dao;

import com.biblio.entities.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RessourceDAO extends JpaRepository<Ressource, Long> {
    boolean existsByIsbn(String isbn);
    List<Ressource> findByBibliothequeId(Long bibliothequeId);
}
