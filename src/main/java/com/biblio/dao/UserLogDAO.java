package com.biblio.dao;

import com.biblio.entities.UserLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLogDAO extends JpaRepository<UserLog, Long> {
    Page<UserLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<UserLog> findByUtilisateur_IdOrderByCreatedAtDesc(Long utilisateurId, Pageable pageable);
}

