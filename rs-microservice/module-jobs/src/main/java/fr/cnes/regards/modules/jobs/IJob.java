package fr.cnes.regards.modules.jobs;

import java.nio.file.Path;
import java.util.List;

public interface IJob {

    StatusInfo cancel();

    StatusInfo execute();

    int getPriority();

    List<Output> getResults();

    StatusInfo getStatus();

    boolean hasResult();

    boolean needWorkspace();

    StatusInfo restart();

    void setWorkspace(Path path);

    StatusInfo stop();

}
