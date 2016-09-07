package fr.cnes.regards.microservices.jobs;

import java.util.UUID;

public class JobId {

    private final UUID id;

    public JobId() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

}
