/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class JobInfoTest {

    @Test
    public void testDomain() {
        final JobInfo jobInfo = new JobInfo();
        final String pClassName = "fr.cnes.someClass";
        final Boolean isArchived = Boolean.TRUE;
        final Long id = 17L;
        final JobParameters parameters = new JobParameters();
        final String owner = "system";
        final List<Output> result = new ArrayList<>();
        final Integer priority = 454;
        final Path workspace = FileSystems.getDefault().getPath("logs", "access.log");

        final StatusInfo status = new StatusInfo();

        jobInfo.setArchived(isArchived);
        jobInfo.setClassName(pClassName);
        jobInfo.setId(id);
        jobInfo.setOwner(owner);
        jobInfo.setParameters(parameters);
        jobInfo.setPriority(priority);
        jobInfo.setResult(result);
        jobInfo.setStatus(status);
        jobInfo.setWorkspace(workspace);

        Assertions.assertThat(jobInfo.getClassName()).isEqualTo(pClassName);
        Assertions.assertThat(jobInfo.isArchived()).isEqualTo(isArchived);
        Assertions.assertThat(jobInfo.getId()).isEqualTo(id);
        Assertions.assertThat(jobInfo.getParameters()).isEqualTo(parameters);
        Assertions.assertThat(jobInfo.getOwner()).isEqualTo(owner);
        Assertions.assertThat(jobInfo.getResult()).isEqualTo(result);
        Assertions.assertThat(jobInfo.getPriority()).isEqualTo(priority);
        Assertions.assertThat(jobInfo.getStatus()).isEqualTo(status);
        Assertions.assertThat(jobInfo.getWorkspace()).isEqualTo(workspace);

        Assertions.assertThat(jobInfo.needWorkspace()).isEqualTo(true);
        jobInfo.setWorkspace(null);
        Assertions.assertThat(jobInfo.needWorkspace()).isEqualTo(false);

        final OffsetDateTime now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);

        final JobConfiguration jobConfiguration = new JobConfiguration("", parameters, pClassName, now, now, priority,
                workspace, owner);
        new JobInfo(jobConfiguration);
    }
}
