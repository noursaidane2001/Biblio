package com.biblio.dao.impl;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class UserDAOImpl implements UserDAO {
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<User> findByEmail(String email) {
        TypedQuery<User> query = entityManager.createQuery(
            "SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);
        try {
            User user = query.getSingleResult();
            return Optional.of(user);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
            .setParameter("email", email)
            .getSingleResult();
        return count > 0;
    }

    @Override
    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        } else {
            return entityManager.merge(user);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        User user = entityManager.find(User.class, id);
        return Optional.ofNullable(user);
    }
    
    @Override
    public Optional<User> findByTokenVerification(String token) {
        TypedQuery<User> query = entityManager.createQuery(
            "SELECT u FROM User u WHERE u.tokenVerification = :token", User.class);
        query.setParameter("token", token);
        try {
            User user = query.getSingleResult();
            return Optional.of(user);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }
}
