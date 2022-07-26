package fr.cnes.regards.modules.dam.domain.entities.metadata;

import java.util.Map;
import java.util.Set;
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
     * Information about a group access to a specific dataset for data objects.
     *
     * @author Sébastien Binda
     */
    public static class DataObjectGroup {

        /**
         * Group name
         */
        private final String groupName;

        /**
         * Does the group have access to data files ?
         */
        private final Boolean dataObjectAccess;

        /**
         * Does the group have access to the dataset ?
         */
        private final Boolean datasetAccess;

        /**
         * Does the group have access to the dataobjects metadatas ?
         */
        private final String metaDataObjectAccessFilterPluginBusinessId;

        /**
         * Identifier of the plugin configuration used to define specific access to data objects metadatas.<br/>
         * Can be null, in this case all dataobjects of the dataset are available for the group.
         */
        private final String dataObjectAccessFilterPluginBusinessId;

        public DataObjectGroup(String groupName,
                               Boolean datasetAccess,
                               Boolean dataObjectAccess,
                               String metaDataObjectAccessFilterPlugin,
                               String dataObjectAccessFilterPlugin) {
            super();
            this.groupName = groupName;
            this.dataObjectAccess = dataObjectAccess;
            this.datasetAccess = datasetAccess;
            this.metaDataObjectAccessFilterPluginBusinessId = metaDataObjectAccessFilterPlugin;
            this.dataObjectAccessFilterPluginBusinessId = dataObjectAccessFilterPlugin;
        }

        public String getGroupName() {
            return groupName;
        }

        public Boolean getDataObjectAccess() {
            return dataObjectAccess;
        }

        public String getDataObjectAccessFilterPluginBusinessId() {
            return dataObjectAccessFilterPluginBusinessId;
        }

        public Boolean getDatasetAccess() {
            return datasetAccess;
        }

        public String getMetaDataObjectAccessFilterPluginBusinessId() {
            return metaDataObjectAccessFilterPluginBusinessId;
        }

    }

    /**
     * Associated data objects groups.
     * Same groups as dataset ones except some if rights don't permit access to data objects
     */
    private final ConcurrentHashMap<String, DataObjectGroup> dataObjectsGroups = new ConcurrentHashMap<>();

    public Set<String> getDataObjectsGroups() {
        return dataObjectsGroups.keySet();
    }

    public void addDataObjectGroup(String groupName,
                                   Boolean datasetAccess,
                                   Boolean dataObjectAccess,
                                   String metaDataObjectAccessFilterPlugin,
                                   String dataObjectAccessFilterPlugin) {
        this.dataObjectsGroups.put(groupName,
                                   new DataObjectGroup(groupName,
                                                       datasetAccess,
                                                       dataObjectAccess,
                                                       metaDataObjectAccessFilterPlugin,
                                                       dataObjectAccessFilterPlugin));
    }

    public Map<String, DataObjectGroup> getDataObjectsGroupsMap() {
        return dataObjectsGroups;
    }
}
