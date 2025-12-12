package com.biblio.enums;

public enum Categorie {
	 	LITTERATURE("Littérature"),
	    SCIENCES("Sciences"),
	    MULTIMEDIA("Multimédia"),
	    HISTOIRE("Histoire"),
	    TECHNOLOGIE("Technologie"),
	    ARTS("Arts"),
	    JEUNESSE("Jeunesse"),
	    PHILOSOPHIE("Philosophie"),
	    ECONOMIE("Économie"),
	    DROIT("Droit");

	    private final String displayName;

	    Categorie(String displayName) {
	        this.displayName = displayName;
	    }

	    public String getDisplayName() {
	        return displayName;
	    }
}
