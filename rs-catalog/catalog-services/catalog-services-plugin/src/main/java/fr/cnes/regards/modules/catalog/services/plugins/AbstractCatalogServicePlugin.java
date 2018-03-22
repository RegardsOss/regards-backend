package fr.cnes.regards.modules.catalog.services.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

public class AbstractCatalogServicePlugin {

    public static final String APPLY_TO_ALL_DATASETS_PARAM = "applyToAllDatasets";

    @PluginParameter(name = AbstractCatalogServicePlugin.APPLY_TO_ALL_DATASETS_PARAM,
            label = "Activate this service for datas of every datasets",
            description = "If this parameter is not true, then you have to configure each dataset to allow access to this service.",
            defaultValue = "false", optional = false)
    private Boolean applyToAllDatasets;

}
