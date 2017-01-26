/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.StatusInfo;

/**
 * @author Léo Mieulet
 *
 */
public interface IStatusInfoRepository extends JpaRepository<StatusInfo, String> {

    /**
     * 
     * @param pJobStatus
     *            the {@link JobStatus} to used for the request
     * @return List of {@link StatusInfo}
     */
    List<StatusInfo> findAllByStatus(JobStatus pJobStatus);
}
