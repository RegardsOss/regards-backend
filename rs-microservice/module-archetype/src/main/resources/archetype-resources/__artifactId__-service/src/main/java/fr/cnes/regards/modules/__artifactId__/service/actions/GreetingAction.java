package fr.cnes.regards.modules.${artifactId}.service.actions;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import fr.cnes.regards.microservices.jobs.IJob;
import fr.cnes.regards.microservices.jobs.Output;
import fr.cnes.regards.microservices.jobs.StatusInfo;
import fr.cnes.regards.modules.${artifactId}.domain.Greeting;

public class GreetingAction extends Job {

    private static final String template = "Hello, %s!";

    private final AtomicLong counter = new AtomicLong();

    private final String name_;

    public GreetingAction(String pName) {
        super();
        name_ = pName;
    }

    @Override
    public StatusInfo cancel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StatusInfo execute() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasResult() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean needWorkspace() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public StatusInfo restart() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StatusInfo stop() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setWorkspace(Path pPath) {
        // TODO Auto-generated method stub

    }
    
}
