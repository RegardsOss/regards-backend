package fr.cnes.regards.framework.modules.jobs.domain.event;

import java.util.List;

/**
 * Type of event that occured on a job.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public enum JobEventType {

    ABORTED, FAILED, RUNNING, SUCCEEDED;

    public static List<JobEventType> runnings() {
        return List.of(ABORTED, FAILED, SUCCEEDED);
    }

}
