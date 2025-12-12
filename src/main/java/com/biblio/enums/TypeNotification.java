package com.biblio.enums;

public enum TypeNotification {
	DISPONIBILITE("Disponibilité"),
    RAPPEL_RETOUR("Rappel de retour"),
    RETARD("Retard"),
    RESERVATION_CONFIRMEE("Réservation confirmée"),
    SYSTEME("Système");

    private final String displayName;

    TypeNotification(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
