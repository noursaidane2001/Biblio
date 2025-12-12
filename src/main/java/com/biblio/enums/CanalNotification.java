package com.biblio.enums;

public enum CanalNotification {
	 	EMAIL("Email"),
	    PUSH("Push"),
	    INTERNE("Interne");

	    private final String displayName;

	    CanalNotification(String displayName) {
	        this.displayName = displayName;
	    }

	    public String getDisplayName() {
	        return displayName;
	    }
}
