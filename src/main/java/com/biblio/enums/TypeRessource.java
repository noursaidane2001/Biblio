package com.biblio.enums;

public enum TypeRessource {
	LIVRE("Livre"),
    DVD("DVD"),
    CD("CD"),
    REVUE("Revue"),
    EBOOK("E-Book"),
    AUDIOBOOK("Livre Audio");

    private final String displayName;

    TypeRessource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
