/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.jobs.domain.Job;

/**
 * Interface for an JPA auto-generated CRUD repository managing Jobs.
 *
 */
public interface IJobRepository extends JpaRepository<Job, Long> {

}
