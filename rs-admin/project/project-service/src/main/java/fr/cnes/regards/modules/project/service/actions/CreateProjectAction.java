package fr.cnes.regards.modules.project.service.actions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.modules.jobs.Job;
import fr.cnes.regards.modules.jobs.StatusInfo;
import fr.cnes.regards.modules.project.domain.Project;

public class CreateProjectAction extends Job implements ProjectAction {

    private static List<Project> projects = new ArrayList<>();

    public CreateProjectAction() {

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
