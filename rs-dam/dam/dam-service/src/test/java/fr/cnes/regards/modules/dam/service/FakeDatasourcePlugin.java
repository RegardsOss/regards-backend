package fr.cnes.regards.modules.dam.service;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;

import java.time.OffsetDateTime;
import java.util.List;

@Plugin(id = "FakeDatasourcePlugin",
        version = "beta",
        description = "For test only",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class FakeDatasourcePlugin implements IDataSourcePlugin {

    public static final String MODEL_PARAM = "model";

    @PluginParameter(name = MODEL_PARAM, label = "Data model name")
    private String model;

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public int getRefreshRate() {
        return 0;
    }

    @Override
    public List<DataObjectFeature> findAll(String tenant,
                                           CrawlingCursor cursor,
                                           OffsetDateTime from,
                                           OffsetDateTime to) {
        return null;
    }
}
