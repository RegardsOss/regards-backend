package fr.cnes.regards.microservices.jobs;

import java.nio.file.Path;
import java.security.acl.Owner;
import java.util.ArrayList;
import java.util.List;

public abstract class Job implements IJob {

    private int priority;

    private Path workspace;

    private final StatusInfo statusInfo;

    private final List<Output> result;

    // private List<Parameter> parameters;

    private Owner owner;

    public Job() {
        super();
        this.statusInfo = new StatusInfo();
        this.result = new ArrayList<>();
    }

    @Override
    public final int getPriority() {
        return this.priority;
    }

    @Override
    public final List<Output> getResults() {
        return this.result;
    }

    @Override
    public final StatusInfo getStatus() {
        return this.statusInfo;
    }

}
