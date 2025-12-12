package com.biblio.enums;

public enum FormatExport {
	 	CSV("CSV"),
	    PDF("PDF"),
	    JSON("JSON");

	    private final String displayName;

	    FormatExport(String displayName) {
	        this.displayName = displayName;
	    }

	    public String getDisplayName() {
	        return displayName;
	    }

	    public String getExtension() {
	        return "." + this.name().toLowerCase();
	    }
}
