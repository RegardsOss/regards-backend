/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.entities.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.entities.domain.DataSet;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Repository
public interface IDataSetRepository extends IAbstractEntityRepository<DataSet> {

    @Query("from DataSet ds left join fetch ds.pluginConfigurationIds where ds.id=:id")
    DataSet findOneWithPluginConfigurations(@Param("id") Long pDataSetId);

    List<DataSet> findByGroups(String group);
}
