/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

/**
 * Interface for an JPA auto-generated CRUD repository managing Jobs.
 *
 */
public interface IJobInfoRepository extends JpaRepository<JobInfo, Long> {

    List<JobInfo> findAllByStatusStatus(JobStatus pStatus);
}
