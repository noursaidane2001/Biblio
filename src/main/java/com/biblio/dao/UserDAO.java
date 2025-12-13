package com.biblio.dao;

import com.biblio.entities.User;
import java.util.Optional;

public interface UserDAO {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByTokenVerification(String token);
}
