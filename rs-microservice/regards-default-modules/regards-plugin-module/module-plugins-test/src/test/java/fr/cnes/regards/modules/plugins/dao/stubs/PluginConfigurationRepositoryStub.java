/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao.stubs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.RepositoryStub;
import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;

/***
 * {@link PluginConfiguration} Repository stub.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 *
 */
@Repository
@Primary
public class PluginConfigurationRepositoryStub extends RepositoryStub<PluginConfiguration>
        implements IPluginConfigurationRepository {

    public PluginConfigurationRepositoryStub() {
        getEntities().add(getPluginConfigurationWithDynamicParameter());
        getEntities().add(getPluginConfigurationWithParameters());
    }

    /**
     * Version
     */
    static final String VERSION = "12345-6789-11";

    /**
     * RED constant {@link String}
     */
    static final String RED = "red";

    /**
     * GREEN constant {@link String}
     */
    static final String GREEN = "green";

    /**
     * BLUE constant {@link String}
     */
    static final String BLUE = "blue";

    /**
     * A {@link PluginParameter}
     */
    static final PluginParameter PARAMETER1 = PluginParametersFactory.build().addParameter("param11", "value11")
            .getParameters().get(0);

    /**
     * A {@link List} of values
     */
    static final List<String> DYNAMICVALUES = Arrays.asList(RED, BLUE, GREEN);

    /**
     * A {@link PluginParameter}
     */
    static final PluginParameter PARAMETER2 = PluginParametersFactory.build()
            .addParameterDynamic("param-dyn21", RED, DYNAMICVALUES).getParameters().get(0);

    /**
     * A list of {@link PluginParameter}
     */
    static final List<PluginParameter> INTERFACEPARAMETERS = PluginParametersFactory.build()
            .addParameter("param31", "value31").addParameter("param32", "value32").addParameter("param33", "value33")
            .addParameter("param34", "value34").addParameter("param35", "value35").getParameters();

    /**
     * A {@link PluginConfiguration}
     */
    private final PluginConfiguration pluginConfiguration1 = new PluginConfiguration(this.getPluginMetaData(),
            "a configuration", INTERFACEPARAMETERS, 0);

    /**
     * A list of {@link PluginParameter} with a dynamic {@link PluginParameter}
     */
    private final PluginConfiguration pluginConfiguration2 = new PluginConfiguration(this.getPluginMetaData(),
            "second configuration", Arrays.asList(PARAMETER1, PARAMETER2), 0);

    public PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(Integer.class.getCanonicalName());
        pluginMetaData.setPluginId("plugin-id");
        pluginMetaData.setAuthor("CS-SI");
        pluginMetaData.setVersion(VERSION);
        return pluginMetaData;
    }

    public PluginConfiguration getPluginConfigurationWithParameters() {
        pluginConfiguration1.setId(01L);
        return pluginConfiguration1;
    }

    public PluginConfiguration getPluginConfigurationWithDynamicParameter() {
        pluginConfiguration2.setId(02L);
        return pluginConfiguration2;
    }

    @Override
    public List<PluginConfiguration> findByPluginIdOrderByPriorityOrderDesc(final String pPluginId) {
        try (Stream<PluginConfiguration> stream = getEntities().stream()) {
            final List<PluginConfiguration> plgConfs = new ArrayList<>();
            stream.filter(p -> p.getPluginId().equals(pPluginId)).forEach(p -> plgConfs.add(p));
            return plgConfs;
        }
    }

}
