package fr.cnes.regards.modules.search.service;

public enum AccessStatus {

    GRANTED, FORBIDDEN, LOCKED, NOT_FOUND, ERROR;

    public boolean isGranted() {
        return this.equals(GRANTED);
    }
}
