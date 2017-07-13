/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.dao;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;

/**
 * Interface for a JPA auto-generated CRUD repository managing Jobs.
 * 
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
public interface IJobInfoRepository extends CrudRepository<JobInfo, Long> {
    /**
     * @param status the {@link JobStatus} to used for the request
     * @return a list of {@link JobInfo}
     */
    List<JobInfo> findAllByStatusStatus(JobStatus status);

    @EntityGraph(attributePaths = { "parameters", "results" })
    JobInfo findById(Long id);
}
