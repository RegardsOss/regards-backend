/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.nio.file.Path;
import java.util.List;

public interface IJob extends Runnable {

    int getPriority();

    List<Output> getResults();

    StatusInfo getStatus();

    boolean hasResult();

    boolean needWorkspace();

    void setWorkspace(Path path);

}
