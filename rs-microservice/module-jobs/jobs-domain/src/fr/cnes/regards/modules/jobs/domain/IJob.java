/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.nio.file.Path;
import java.util.List;

import org.springframework.hateoas.Identifiable;

public interface IJob extends Identifiable<Long> {

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
