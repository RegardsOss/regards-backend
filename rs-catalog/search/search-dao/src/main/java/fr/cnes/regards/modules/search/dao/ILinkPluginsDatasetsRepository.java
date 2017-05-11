/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface ILinkPluginsDatasetsRepository extends JpaRepository<LinkPluginsDatasets, Long> {

    LinkPluginsDatasets findOneByDatasetId(String pDatasetId);

}
