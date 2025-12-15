package com.biblio.config;

import com.biblio.dao.BibliothequeDAO;
import com.biblio.dao.UserDAO;
import com.biblio.entities.Bibliotheque;
import com.biblio.entities.User;
import com.biblio.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Initialise les données de base au démarrage de l'application
 * Crée un super administrateur par défaut si aucun n'existe
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final UserDAO userDAO;
    private final BibliothequeDAO bibliothequeDAO;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserDAO userDAO, BibliothequeDAO bibliothequeDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.bibliothequeDAO = bibliothequeDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Créer un super administrateur par défaut s'il n'existe pas
        if (userDAO.countByRole(Role.SUPER_ADMIN) == 0) {
            logger.info("Aucun super administrateur trouvé. Création d'un super admin par défaut...");
            
            User superAdmin = new User();
            superAdmin.setNom("Admin");
            superAdmin.setPrenom("Super");
            superAdmin.setEmail("superadmin@biblio.com");
            superAdmin.setMotDePasse(passwordEncoder.encode("admin123")); // Mot de passe par défaut
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setEmailVerifie(true);
            superAdmin.setActif(true);
            superAdmin.setDateInscription(LocalDateTime.now());
            
            userDAO.save(superAdmin);
            logger.info("Super administrateur créé avec succès:");
            logger.info("  Email: superadmin@biblio.com");
            logger.info("  Mot de passe: admin123");
            logger.info("  ⚠️  IMPORTANT: Changez ce mot de passe après la première connexion !");
        } else {
            logger.debug("Super administrateur déjà présent dans la base de données");
        }
    }
}
