# 📚 Biblio - Système de Gestion de Réseau de Bibliothèques

Bienvenue dans **Biblio**, une application complète de gestion de réseau de bibliothèques développée avec **Spring Boot** et **Thymeleaf**. Ce projet permet de gérer plusieurs bibliothèques, leurs ressources (livres, revues, etc.), les utilisateurs, ainsi que les flux d'emprunts et de réservations.

## 🚀 Fonctionnalités Principales

### 🌍 Partie Publique (Utilisateurs non connectés)
- **Carte Interactive :** Visualisation géographique des bibliothèques via l'API **Leaflet**.
- **Catalogue :** Consultation des ressources disponibles dans chaque bibliothèque.


### 🔐 Authentification & Sécurité
- **Multi-méthodes :** Authentification classique (Email/Mot de passe) et **OAuth2 (Google)**.
- **JWT (JSON Web Token) :** Sécurisation des échanges API.

### 👥 Rôles et Espaces

1.  **👑 Super Admin**
    *   Gestion globale du réseau de bibliothèques.
    *   Création et gestion des comptes Administrateurs.
    *   Vue d'ensemble des statistiques du réseau.

2.  **🏢 Admin Bibliothèque**
    *   Gestion complète de sa bibliothèque .
    *   Gestion du personnel (Bibliothécaires).
    *   Gestion du catalogue de ressources (Ajout, modification, stock).

3.  **📚 Bibliothécaire**
    *   Gestion des opérations quotidiennes.
    *   Enregistrement des prêts et retours.
    *   Gestion des abonnés (Usagers).

4.  **👤 Usager**
    *   Recherche de ressources.
    *   Réservation de livres.
    *   Consultation de l'historique des emprunts.
    *   Gestion du profil.

## 🛠️ Stack Technique

*   **Backend :** Java 17, Spring Boot 3+ (Spring Data JPA, Spring Security, Spring Web).
*   **Base de Données :** MySQL.
*   **Frontend :** Thymeleaf, Bootstrap 5, Leaflet JS (Cartographie).
*   **Outils :** Maven, Lombok.
*   **Autres :** JavaMailSender (Notifications Email), OpenPDF (Génération de rapports), JWT.

## ⚙️ Prérequis

*   **Java 17** ou supérieur installé.
*   **Maven** installé.
*   **MySQL** serveur installé et en cours d'exécution.

## 📦 Installation et Lancement

1.  **Cloner le dépôt :**
    ```bash
    git clone <votre-url-repo>
    cd Biblio
    ```

2.  **Configuration de la Base de Données :**
    

3.  **Configuration Email (Optionnel pour le dev) :**
  
4.  **Lancer l'application :**
   

5.  **Accéder à l'application :**
    *   Ouvrez votre navigateur sur : [http://localhost:8080](http://localhost:8080)

## 🔑 Comptes par Défaut

Lors du premier démarrage, un compte **Super Administrateur** est créé automatiquement :

*   **Email :** `superadmin@biblio.com`
*   **Mot de passe :** `admin123`

> ⚠️ **Important :** Pour des raisons de sécurité, veuillez changer ce mot de passe dès votre première connexion.

## 🗺️ Structure du Projet

*   `src/main/java/com/biblio` : Code source Java.
    *   `/config` : Configurations (Sécurité, DataInit, etc.).
    *   `/controllers` : Contrôleurs Web et API.
    *   `/entities` : Modèles de données (JPA).
    *   `/services` : Logique métier.
    *   `/dao` : Accès aux données (Repositories).
*   `src/main/resources` : Ressources statiques.
    *   `/templates` : Vues Thymeleaf (HTML).
    *   `/static` : CSS, JS, Images.
    *   `application.properties` : Configuration de l'application.

