package fr.cnes.regards.modules.dam.domain.entities.metadata;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dataset metadata. This object is only used by Elasticsearch<br/>
 * <p>
 * Register all access rights for data object level :
 * <ul>
 * <li>Map keys represent groups with full access on metadata (i.e. AccessLevel set to FULL_ACCESS)</li>
 * <li>Map value represents the data access rights for the related group : true if and only if DataAccessLevel set to
 * INHERITED_ACCESS
 * (CUSTOM_ACCESS not supported at the moment)</li>
 * </ul>
 *
 * @author oroussel
 */
public class DatasetMetadata {

    /**
     * Associated data objects groups.
     * Same groups as dataset ones except some if rights don't permit access to data objects
     */
    private final ConcurrentHashMap<String, DataObjectGroup> dataObjectsGroups = new ConcurrentHashMap<>();

    public DatasetMetadata() {
        // Do nothing
    }

    public void addDataObjectGroup(@Nullable DataObjectGroup dataObjectGroup) {
        if (dataObjectGroup != null) {
            this.dataObjectsGroups.put(dataObjectGroup.getGroupName(), dataObjectGroup);
        }
    }

    public void addDataObjectGroup(String groupName,
                                   Boolean datasetAccess,
                                   Boolean dataFileAccess,
                                   Boolean dataObjectAccess,
                                   String metaDataObjectAccessFilterPlugin,
                                   String dataObjectAccessFilterPlugin) {
        this.dataObjectsGroups.put(groupName,
                                   new DataObjectGroup(groupName,
                                                       datasetAccess,
                                                       dataFileAccess,
                                                       dataObjectAccess,
                                                       metaDataObjectAccessFilterPlugin,
                                                       dataObjectAccessFilterPlugin));
    }

    public Map<String, DataObjectGroup> getDataObjectsGroups() {
        return dataObjectsGroups;
    }
}
