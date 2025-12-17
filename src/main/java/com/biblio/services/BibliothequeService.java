package com.biblio.services;

import com.biblio.dao.BibliothequeDAO;
import com.biblio.entities.Bibliotheque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BibliothequeService {
    private static final Logger logger = LoggerFactory.getLogger(BibliothequeService.class);
    
    private final BibliothequeDAO bibliothequeDAO;

    public BibliothequeService(BibliothequeDAO bibliothequeDAO) {
        this.bibliothequeDAO = bibliothequeDAO;
    }

    /**
     * Récupère toutes les bibliothèques actives
     */
    public List<Bibliotheque> getAllActives() {
        return bibliothequeDAO.findAllActives();
    }

    /**
     * Récupère toutes les bibliothèques
     */
    public List<Bibliotheque> getAll() {
        return bibliothequeDAO.findAll();
    }

    /**
     * Récupère les bibliothèques avec pagination
     */
    public Page<Bibliotheque> getAllPaged(Pageable pageable) {
        return bibliothequeDAO.findAll(pageable);
    }

    /**
     * Récupère une bibliothèque par son ID
     */
    public Bibliotheque getById(Long id) {
        return bibliothequeDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bibliothèque non trouvée avec l'ID: " + id));
    }

    /**
     * Crée une nouvelle bibliothèque
     */
    @Transactional
    public Bibliotheque createBibliotheque(String nom, String adresse, String ville, 
                                          String telephone, Integer capaciteStock,
                                          Double latitude, Double longitude) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la bibliothèque est obligatoire");
        }
        if (adresse == null || adresse.trim().isEmpty()) {
            throw new IllegalArgumentException("L'adresse est obligatoire");
        }
        if (ville == null || ville.trim().isEmpty()) {
            throw new IllegalArgumentException("La ville est obligatoire");
        }
        String telephoneNorm = telephone != null ? telephone.trim() : null;
        if (telephoneNorm != null && !telephoneNorm.isEmpty()) {
            if (!telephoneNorm.matches("^\\+?[0-9\\s\\-()]{8,20}$")) {
                throw new IllegalArgumentException("Format de téléphone invalide");
            }
        } else {
            telephoneNorm = null;
        }
        if (capaciteStock != null && capaciteStock < 0) {
            throw new IllegalArgumentException("La capacité ne peut pas être négative");
        }
        if (bibliothequeDAO.existsByNom(nom)) {
            throw new IllegalArgumentException("Une bibliothèque avec ce nom existe déjà");
        }

        if ((latitude == null || longitude == null) && adresse != null && ville != null) {
            Double[] coords = geocodeAdresseVille(adresse, ville);
            if (coords != null) {
                latitude = latitude == null ? coords[0] : latitude;
                longitude = longitude == null ? coords[1] : longitude;
            }
        }

        Bibliotheque bibliotheque = Bibliotheque.builder()
                .nom(nom.trim())
                .adresse(adresse.trim())
                .ville(ville.trim())
                .telephone(telephoneNorm)
                .capaciteStock(capaciteStock)
                .actif(true)
                .latitude(latitude)
                .longitude(longitude)
                .build();

        Bibliotheque saved = bibliothequeDAO.save(bibliotheque);
        logger.info("Bibliothèque créée: {} (ID: {})", nom, saved.getId());
        
        return saved;
    }

    /**
     * Met à jour une bibliothèque
     */
    @Transactional
    public Bibliotheque updateBibliotheque(Long id, String nom, String adresse, String ville,
                                           String telephone, Integer capaciteStock, Boolean actif,
                                           Double latitude, Double longitude) {
        Bibliotheque bibliotheque = bibliothequeDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bibliothèque non trouvée avec l'ID: " + id));

        // Vérifier que le nom n'est pas déjà utilisé par une autre bibliothèque
        if (nom != null && !nom.equals(bibliotheque.getNom())) {
            if (bibliothequeDAO.existsByNom(nom)) {
                throw new IllegalArgumentException("Une bibliothèque avec ce nom existe déjà");
            }
            bibliotheque.setNom(nom);
        }

        if (adresse != null) bibliotheque.setAdresse(adresse);
        if (ville != null) bibliotheque.setVille(ville);
        if (telephone != null) bibliotheque.setTelephone(telephone);
        if (capaciteStock != null) bibliotheque.setCapaciteStock(capaciteStock);
        if (actif != null) bibliotheque.setActif(actif);
        if (latitude != null) bibliotheque.setLatitude(latitude);
        if (longitude != null) bibliotheque.setLongitude(longitude);

        Bibliotheque updated = bibliothequeDAO.save(bibliotheque);
        logger.info("Bibliothèque mise à jour: {} (ID: {})", updated.getNom(), updated.getId());
        
        return updated;
    }

    /**
     * Supprime une bibliothèque
     */
    @Transactional
    public void deleteBibliotheque(Long id) {
        Bibliotheque bibliotheque = bibliothequeDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bibliothèque non trouvée avec l'ID: " + id));
        
        logger.info("Suppression de la bibliothèque: {} (ID: {})", bibliotheque.getNom(), bibliotheque.getId());
        
        bibliothequeDAO.delete(bibliotheque);
    }

    private Double[] geocodeAdresseVille(String adresse, String ville) {
        try {
            String q = (adresse + ", " + ville).trim();
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", "BiblioApp/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                if (root.isArray() && root.size() > 0) {
                    JsonNode n = root.get(0);
                    double lat = Double.parseDouble(n.get("lat").asText());
                    double lon = Double.parseDouble(n.get("lon").asText());
                    return new Double[]{lat, lon};
                }
            }
        } catch (Exception e) {
            logger.warn("Echec géocodage adresse/ville: {}", e.getMessage());
        }
        return null;
    }
}
