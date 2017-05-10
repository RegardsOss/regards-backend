/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.configuration.domain.LinkUIPluginsDatasets;

/**
 * JPA Respository to access {@link LinkUIPluginsDatasets} entities
 *
 * @author Sébastien Binda
 *
 */
public interface ILinkUIPluginsDatasetsRepository extends JpaRepository<LinkUIPluginsDatasets, Long> {

    @EntityGraph(value = "graph.link.configurations")
    LinkUIPluginsDatasets findOneByDatasetId(String pDatasetId);

}
