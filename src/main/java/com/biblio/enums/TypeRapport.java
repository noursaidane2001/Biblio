package com.biblio.enums;

public enum TypeRapport {
	 	PRETS_PAR_CATEGORIE("Prêts par catégorie"),
	    PRETS_PAR_BIBLIOTHEQUE("Prêts par bibliothèque"),
	    ROTATION_STOCK("Rotation du stock"),
	    UTILISATEURS_ACTIFS("Utilisateurs actifs"),
	    RETARDS("Retards");

	    private final String displayName;

	    TypeRapport(String displayName) {
	        this.displayName = displayName;
	    }

	    public String getDisplayName() {
	        return displayName;
	    }
}
