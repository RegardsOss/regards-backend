package fr.cnes.regards.modules.users.domain;

public enum UserVisibility {
    READABLE, WRITEABLE, HIDDEN;

    @Override
    public String toString() {
        return this.name();
    }
}
