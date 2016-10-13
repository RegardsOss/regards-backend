/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service.actions;

import java.nio.file.Path;

import fr.cnes.regards.modules.jobs.Job;
import fr.cnes.regards.modules.jobs.StatusInfo;

/**
 *
 * Class DeleteProjectAction
 *
 * Business action to delete a project.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class DeleteProjectAction extends Job implements IProjectAction {

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
    public void setWorkspace(final Path pPath) {
        // TODO Auto-generated method stub

    }

    @Override
    public StatusInfo stop() {
        // TODO Auto-generated method stub
        return null;
    }

}
