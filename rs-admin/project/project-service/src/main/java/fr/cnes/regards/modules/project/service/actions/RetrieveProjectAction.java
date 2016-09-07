package fr.cnes.regards.modules.project.service.actions;

import java.nio.file.Path;

import fr.cnes.regards.microservices.jobs.Job;
import fr.cnes.regards.microservices.jobs.StatusInfo;

public class RetrieveProjectAction extends Job implements ProjectAction {

    private final String projectId;

    public RetrieveProjectAction(String projectId) {
        this.projectId = projectId;
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
    public void setWorkspace(Path pPath) {
        // TODO Auto-generated method stub

    }

    @Override
    public StatusInfo stop() {
        // TODO Auto-generated method stub
        return null;
    }

}
