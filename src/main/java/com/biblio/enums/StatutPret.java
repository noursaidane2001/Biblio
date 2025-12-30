package com.biblio.enums;

public enum StatutPret {
	  	RESERVE("Réservé"),
	    EMPRUNTE("Emprunté"),
	    EN_COURS("En cours"),
	    BLOQUE("Bloqué"),
	    RETOURNE("Retourné"),
	    CLOTURE("Clôturé"),
	    ANNULE("Annulé");

	    private final String displayName;

	    StatutPret(String displayName) {
	        this.displayName = displayName;
	    }

	    public String getDisplayName() {
	        return displayName;
	    }

	    public boolean isActive() {
	        return this == RESERVE || this == EMPRUNTE || this == EN_COURS;
	    }
}
