package fr.cnes.regards.modules.storage.domain;

import java.util.UUID;

public class UniformResourceName {

    private UUID resourceName;

    public UniformResourceName() {

    }

    public UUID getResourceName() {
        return resourceName;
    }

    public UniformResourceName generateUnifiedResourceName() {
        this.resourceName = UUID.randomUUID();
        return this;
    }

}
