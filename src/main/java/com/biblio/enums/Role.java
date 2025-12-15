package com.biblio.enums;

public enum Role {
	USAGER("Usager"),
    BIBLIOTHECAIRE("Bibliothécaire"),
    ADMIN("Administrateur de bibliothèque"),
    SUPER_ADMIN("Super Administrateur");
    private final String displayName;
    Role(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
}
