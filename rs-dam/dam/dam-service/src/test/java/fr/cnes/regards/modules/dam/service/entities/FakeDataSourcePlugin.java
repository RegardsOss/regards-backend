package fr.cnes.regards.modules.dam.service.entities;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.model.domain.Model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Data source plugin
 *
 * @author Christophe Mertz
 */
@Plugin(id = "FakeDataSourcePlugin", version = "2.0-SNAPSHOT", description = "FakeDataSourcePlugin",
    author = "REGARDS Team", contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI",
    url = "https://github.com/RegardsOss")
public class FakeDataSourcePlugin implements IDataSourcePlugin {

    public static final String MODEL = "model";

    @PluginParameter(label = MODEL, name = MODEL)
    private Model dataModel;

    @Override
    public int getRefreshRate() {
        return 0;
    }

    @Override
    public String getModelName() {
        return dataModel.getName();
    }

    @Override
    public List<DataObjectFeature> findAll(String tenant, CrawlingCursor cursor, OffsetDateTime from) {
        return List.of();
    }
}
