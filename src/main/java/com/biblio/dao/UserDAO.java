package com.biblio.dao;

import com.biblio.entities.User;
import com.biblio.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDAO extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    boolean existsByEmail(String email);

    Optional<User> findByTokenVerification(String token);

    long countByRole(Role role);

    @Query("SELECT u FROM User u ORDER BY u.dateInscription DESC")
    List<User> findAllOrderByDateInscriptionDesc();
}
