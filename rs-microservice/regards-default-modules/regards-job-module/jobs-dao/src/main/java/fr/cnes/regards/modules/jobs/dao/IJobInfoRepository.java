/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

/**
 * Interface for an JPA auto-generated CRUD repository managing Jobs.
 * 
 * @author Léo Mieulet
 * @author Christophe Mertz
 *
 */
public interface IJobInfoRepository extends CrudRepository<JobInfo, Long> {

    /**
     * 
     * @param pStatus
     *            the {@link JobStatus} to used for the request
     * @return a list of {@link JobInfo}
     */
    List<JobInfo> findAllByStatusStatus(JobStatus pStatus);
}
