package fr.cnes.regards.modules.storage.dao;

import fr.cnes.regards.modules.filecatalog.dto.StorageType;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA Repository to handle access to {@link StorageLocationConfiguration} entities.
 *
 * @author Sébatien Binda
 */
public interface IStorageLocationConfigurationRepostory extends JpaRepository<StorageLocationConfiguration, Long> {

    /**
     * We want the {@link StorageLocationConfiguration} with the lowest priority, which means the highest value of the
     * attribute priority.
     * To do so, we order by descending priority and take the first one
     *
     * @param storageType IStorageLocation type
     * @return the less prioritized
     */
    StorageLocationConfiguration findFirstByStorageTypeOrderByPriorityDesc(StorageType storageType);

    Set<StorageLocationConfiguration> findAllByStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(StorageType storageType,
                                                                                                   Long priority);

    List<StorageLocationConfiguration> findAllByStorageTypeOrderByPriorityAsc(StorageType storageType);

    /**
     * We want the active {@link StorageLocationConfiguration} with the highest priority, which means the lowest value of the
     * attribute priority.
     * To do so, we order by ascending priority and take the first one
     *
     * @param storageType        IStorageLocation type
     * @param pluginConfActivity the plugin configuration activeness
     * @return the most prioritized
     */
    StorageLocationConfiguration findFirstByStorageTypeAndPluginConfigurationActiveOrderByPriorityAsc(StorageType storageType,
                                                                                                      boolean pluginConfActivity);

    StorageLocationConfiguration findOneByStorageTypeAndPriority(StorageType storageType, long priority);

    Set<StorageLocationConfiguration> findByStorageTypeAndNameIn(StorageType storageType,
                                                                 Collection<String> confLabels);

    Set<StorageLocationConfiguration> findByNameIn(Collection<String> confLabels);

    Optional<StorageLocationConfiguration> findByName(String businessId);

    boolean existsByName(String name);
}
