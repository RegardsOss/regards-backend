package fr.cnes.regards.modules.users.domain;

public enum AccountStatus {
    INACTIVE, ACCEPTED, ACTIVE, LOCKED, PENDING;

    @Override
    public String toString() {
        return this.name();
    }
}
