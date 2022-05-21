package fr.cnes.regards.modules.dam.domain.settings;

import com.google.common.collect.ImmutableList;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.List;

public final class DamSettings {

    public static final String STORE_FILES = "store_files";

    public static final String STORAGE_LOCATION = "storage_location";

    public static final String STORAGE_SUBDIRECTORY = "storage_subdirectory";

    public static final boolean DEFAULT_STORE_FILES = false;

    public static final String DEFAULT_STORAGE_LOCATION = "";

    public static final String DEFAULT_STORAGE_SUBDIRECTORY = "";

    public static final String INDEX_NUMBER_OF_SHARDS = "index_number_of_shards";

    public static final String INDEX_NUMBER_OF_REPLICAS = "index_number_of_replicas";

    public static final long DEFAULT_INDEX_NUMBER_OF_SHARDS = 5;

    public static final long DEFAULT_INDEX_NUMBER_OF_REPLICAS = 1;

    public static final DynamicTenantSetting STORE_FILES_SETTING = new DynamicTenantSetting(STORE_FILES,
                                                                                            "Controls whether AIP entities are sent to Storage module to be stored",
                                                                                            DEFAULT_STORE_FILES);

    public static final DynamicTenantSetting STORAGE_LOCATION_SETTING = new DynamicTenantSetting(STORAGE_LOCATION,
                                                                                                 "Name of the storage location",
                                                                                                 DEFAULT_STORAGE_LOCATION);

    public static final DynamicTenantSetting STORAGE_SUBDIRECTORY_SETTING = new DynamicTenantSetting(
        STORAGE_SUBDIRECTORY,
        "Name of the subdirectory in the storage location",
        DEFAULT_STORAGE_SUBDIRECTORY);

    public static final DynamicTenantSetting INDEX_NUMBER_OF_SHARDS_SETTING = new DynamicTenantSetting(
        INDEX_NUMBER_OF_SHARDS,
        "Number of shards used by the current tenant index",
        DEFAULT_INDEX_NUMBER_OF_SHARDS);

    public static final DynamicTenantSetting INDEX_NUMBER_OF_REPLICAS_SETTING = new DynamicTenantSetting(
        INDEX_NUMBER_OF_REPLICAS,
        "Number of replicas of each shard in the current tenant index",
        DEFAULT_INDEX_NUMBER_OF_REPLICAS);

    public static final List<DynamicTenantSetting> SETTING_LIST = ImmutableList.of(STORE_FILES_SETTING,
                                                                                   STORAGE_LOCATION_SETTING,
                                                                                   STORAGE_SUBDIRECTORY_SETTING,
                                                                                   INDEX_NUMBER_OF_SHARDS_SETTING,
                                                                                   INDEX_NUMBER_OF_REPLICAS_SETTING);

    private DamSettings() {
    }

}
