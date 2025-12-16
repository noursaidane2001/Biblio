package com.biblio.services;

import com.biblio.dao.BibliothequeDAO;
import com.biblio.dao.RessourceDAO;
import com.biblio.dao.UserDAO;
import com.biblio.entities.Bibliotheque;
import com.biblio.entities.Ressource;
import com.biblio.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RessourceService {
    private static final Logger logger = LoggerFactory.getLogger(RessourceService.class);
    
    private final RessourceDAO ressourceDAO;
    private final BibliothequeDAO bibliothequeDAO;
    private final UserDAO userDAO;

    public RessourceService(RessourceDAO ressourceDAO, BibliothequeDAO bibliothequeDAO, UserDAO userDAO) {
        this.ressourceDAO = ressourceDAO;
        this.bibliothequeDAO = bibliothequeDAO;
        this.userDAO = userDAO;
    }

    /**
     * Crée une nouvelle ressource et l'associe automatiquement à la bibliothèque du bibliothécaire
     */
    @Transactional
    public Ressource createRessource(String titre, String auteur, String isbn,
                                     com.biblio.enums.Categorie categorie,
                                     com.biblio.enums.TypeRessource typeRessource,
                                     String description, String editeur,
                                     java.time.LocalDate datePublication,
                                     Integer nombreExemplaires, Integer exemplairesDisponibles,
                                     String imageCouverture, String bibliothecaireEmail) {
        
        // Récupérer le bibliothécaire
        User bibliothecaire = userDAO.findByEmail(bibliothecaireEmail)
                .orElseThrow(() -> new IllegalArgumentException("Bibliothécaire non trouvé avec l'email: " + bibliothecaireEmail));
        
        // Vérifier que l'utilisateur est un bibliothécaire
        if (!bibliothecaire.isBibliothecaire()) {
            throw new IllegalArgumentException("Seuls les bibliothécaires peuvent créer des ressources");
        }
        
        // Récupérer la bibliothèque du bibliothécaire
        Bibliotheque bibliotheque = bibliothecaire.getBibliotheque();
        if (bibliotheque == null) {
            throw new IllegalArgumentException("Le bibliothécaire n'est associé à aucune bibliothèque");
        }
        
        // Vérifier la capacité de la bibliothèque
        if (bibliotheque.getCapaciteStock() != null) {
            Integer currentStock = ressourceDAO.sumNombreExemplairesByBibliothequeId(bibliotheque.getId());
            if (currentStock + nombreExemplaires > bibliotheque.getCapaciteStock()) {
                throw new IllegalArgumentException("L'ajout de ces exemplaires dépasse la capacité de stockage de la bibliothèque (" + bibliotheque.getCapaciteStock() + ")");
            }
        }
        
        // Vérifier l'ISBN si fourni
        if (isbn != null && !isbn.trim().isEmpty()) {
            if (ressourceDAO.existsByIsbn(isbn)) {
                throw new IllegalArgumentException("Une ressource avec cet ISBN existe déjà");
            }
        }
        
        // Créer la ressource
        Ressource ressource = Ressource.builder()
                .titre(titre)
                .auteur(auteur)
                .isbn(isbn)
                .categorie(categorie)
                .typeRessource(typeRessource)
                .description(description)
                .editeur(editeur)
                .datePublication(datePublication)
                .nombreExemplaires(nombreExemplaires)
                .exemplairesDisponibles(exemplairesDisponibles != null ? exemplairesDisponibles : nombreExemplaires)
                .imageCouverture(imageCouverture)
                .bibliotheque(bibliotheque)
                .popularite(0)
                .build();
        
        Ressource saved = ressourceDAO.save(ressource);
        logger.info("Ressource créée: {} (ID: {}) par bibliothécaire {} pour bibliothèque {}", 
                titre, saved.getId(), bibliothecaireEmail, bibliotheque.getNom());
        
        return saved;
    }

    /**
     * Récupère toutes les ressources d'une bibliothèque
     */
    public List<Ressource> getRessourcesByBibliotheque(Long bibliothequeId) {
        return ressourceDAO.findByBibliothequeId(bibliothequeId);
    }

    /**
     * Récupère toutes les ressources
     */
    public List<Ressource> getAllRessources() {
        return ressourceDAO.findAll();
    }

    /**
     * Récupère une ressource par son ID
     */
    public Ressource getById(Long id) {
        return ressourceDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ressource non trouvée avec l'ID: " + id));
    }
}