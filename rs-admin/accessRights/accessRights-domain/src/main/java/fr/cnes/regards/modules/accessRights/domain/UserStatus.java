package fr.cnes.regards.modules.accessRights.domain;

public enum UserStatus {

    WAITING_ACCES, ACCES_DENIED, ACCESS_GRANTED, ACCESS_INACTIVE;

    @Override
    public String toString() {
        return this.name();
    }
}
