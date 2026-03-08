package com.veryrandomcreator.tenanthelp;

public class PropertyImage {
    private String id;
    private String label;
    private String notes;
    private String latestHash;

    public String getLatestHash() {
        return latestHash;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getNotes() {
        return notes;
    }

    public PropertyImage(String id, String label, String notes, String latestHash) {
        this.id = id;
        this.label = label;
        this.notes = notes;
        this.latestHash = latestHash;
    }
}
