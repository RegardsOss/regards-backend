/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.jobs.domain.JobInfo;

/**
 * Interface for an JPA auto-generated CRUD repository managing Jobs.
 *
 */
public interface IJobInfoRepository extends JpaRepository<JobInfo, Long> {

}
