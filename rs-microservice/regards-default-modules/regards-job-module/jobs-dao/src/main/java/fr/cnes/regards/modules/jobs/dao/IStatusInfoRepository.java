/**
 *
 */
package fr.cnes.regards.modules.jobs.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.jobs.domain.StatusInfo;

/**
 * @author lmieulet
 *
 */
public interface IStatusInfoRepository extends JpaRepository<StatusInfo, String> {

}
