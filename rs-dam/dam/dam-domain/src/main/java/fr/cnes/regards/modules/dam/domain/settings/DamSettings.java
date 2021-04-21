package fr.cnes.regards.modules.dam.domain.settings;

import com.google.common.collect.ImmutableList;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.List;

public final class DamSettings {

    private DamSettings() {}

    public static final String STORE_FILES = "store_files";
    public static final String STORAGE_LOCATION = "storage_location";
    public static final String STORAGE_SUBDIRECTORY = "storage_subdirectory";

    public static final boolean DEFAULT_STORE_FILES = false;
    public static final String DEFAULT_STORAGE_LOCATION = "";
    public static final String DEFAULT_STORAGE_SUBDIRECTORY = "";

    public static final DynamicTenantSetting STORE_FILES_SETTING = new DynamicTenantSetting(
            STORE_FILES,
            "Controls whether AIP entities are sent to Storage module to be stored",
            DEFAULT_STORE_FILES
    );
    public static final DynamicTenantSetting STORAGE_LOCATION_SETTING = new DynamicTenantSetting(
            STORAGE_LOCATION,
            "Name of the storage location",
            DEFAULT_STORAGE_LOCATION
    );
    public static final DynamicTenantSetting STORAGE_SUBDIRECTORY_SETTING = new DynamicTenantSetting(
            STORAGE_SUBDIRECTORY,
            "Name of the subdirectory in the storage location",
            DEFAULT_STORAGE_SUBDIRECTORY
    );

    public static final List<DynamicTenantSetting> SETTING_LIST = ImmutableList.of(
            STORE_FILES_SETTING,
            STORAGE_LOCATION_SETTING,
            STORAGE_SUBDIRECTORY_SETTING
    );

}
