package fr.cnes.regards.microservices.backend.pojo;

public enum ProjectUserStatus {
    WAITING_ACCESS(0),
    ACCESS_DENIED(1),
    ACCESS_GRANTED(2),
    ACCESS_INACTIVE(3);

    private int value;
    ProjectUserStatus(int value) {
        value = value;
    }

    public int getValue() {
        return value;
    }
}
