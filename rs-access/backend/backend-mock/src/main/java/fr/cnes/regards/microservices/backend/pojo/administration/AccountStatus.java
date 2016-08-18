package fr.cnes.regards.microservices.backend.pojo.administration;

/**
 * Created by lmieulet on 02/08/16.
 */
public enum AccountStatus {
    INACTIVE(0),
    ACCEPTED(1),
    ACTIVE(2),
    LOCKED(3),
    PENDING(4);

    private int value;

    AccountStatus(int value) {
        value = value;
    }

    public int getValue() {
        return value;
    }
}
