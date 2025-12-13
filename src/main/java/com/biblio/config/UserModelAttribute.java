package com.biblio.config;

import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ControllerAdvice pour ajouter les attributs utilisateur aux modèles Thymeleaf.
 * Ne s'applique qu'aux contrôleurs qui retournent des vues (pas aux @RestController).
 */
@ControllerAdvice(assignableTypes = {
    com.biblio.controllers.HomeController.class,
    com.biblio.controllers.DashboardController.class,
    com.biblio.controllers.UserController.class,
    com.biblio.controllers.LoginController.class,
    com.biblio.controllers.RegistrationController.class
})
@Component
public class UserModelAttribute {
    private final UserDAO userDAO;

    public UserModelAttribute(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @ModelAttribute
    public void addUserAttributes(Authentication authentication, Model model) {
        // Vérifier si les attributs utilisateur sont déjà présents pour éviter les requêtes répétées
        if (model.containsAttribute("currentUser")) {
            return;
        }
        
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            // Si le principal est déjà un User (entité), réutiliser directement
            // Sinon, charger depuis la base de données
            User user;
            if (userDetails instanceof User) {
                user = (User) userDetails;
            } else {
                // Fallback : charger depuis la base de données
                user = userDAO.findByEmail(userDetails.getUsername()).orElse(null);
            }
            
            if (user != null) {
                model.addAttribute("currentUser", user);
                model.addAttribute("userFullName", user.getNomComplet());
                model.addAttribute("userEmail", user.getUsername()); // getUsername() retourne l'email
            }
        }
    }
}

