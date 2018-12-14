package fr.cnes.regards.modules.storage.dao;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IPrioritizedDataStorageRepository extends JpaRepository<PrioritizedDataStorage, Long> {

    /**
     * We want the {@link PrioritizedDataStorage} with the lowest priority, which means the highest value of the
     * attribute priority.
     * To do so, we order by descending priority and take the first one
     * @param dataStorageType IDataStorage type
     * @return the less prioritized
     */
    PrioritizedDataStorage findFirstByDataStorageTypeOrderByPriorityDesc(DataStorageType dataStorageType);

    Optional<PrioritizedDataStorage> findOneByDataStorageConfigurationId(Long pluginConfId);

    Optional<PrioritizedDataStorage> findOneById(Long pluginConfId);

    Set<PrioritizedDataStorage> findAllByDataStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(
            DataStorageType dataStorageType, Long priority);

    List<PrioritizedDataStorage> findAllByDataStorageTypeOrderByPriorityAsc(DataStorageType dataStorageType);

    /**
     * We want the active {@link PrioritizedDataStorage} with the highest priority, which means the lowest value of the
     * attribute priority.
     * To do so, we order by ascending priority and take the first one
     * @param dataStorageType IDataStorage type
     * @param pluginConfActivity the plugin configuration activeness
     * @return the most prioritized
     */
    PrioritizedDataStorage findFirstByDataStorageTypeAndDataStorageConfigurationActiveOrderByPriorityAsc(
            DataStorageType dataStorageType, boolean pluginConfActivity);

    PrioritizedDataStorage findOneByDataStorageTypeAndPriority(DataStorageType dataStorageType, long priority);

    @Query(value = "SELECT data_storage_conf_id FROM {h-schema}ta_data_file_plugin_conf WHERE data_file_id IN ("
            + "SELECT id FROM {h-schema}t_data_file WHERE aip_ip_id IN (:aipQuery))", nativeQuery = true)
    Set<Long> findAllIdUsedByAipInQuery(@Param("aipQuery") String aipQuery);

    Set<PrioritizedDataStorage> findAllByIdIn(Set<Long> allIdUsedByAipInQuery);
}
