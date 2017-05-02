/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.dao.stubs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.test.repository.RepositoryStub;

/***
 * {@link PluginConfiguration} Repository stub.
 *
 * @author Christophe Mertz
 * @author Sébastien Binda
 */
@Repository
@Primary
@Profile("test")
public class PluginConfigurationRepositoryStub extends RepositoryStub<PluginConfiguration>
        implements IPluginConfigurationRepository {

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
     * A {@link List} of values
     */
    static final List<String> DYNAMICVALUES = Arrays.asList(RED, BLUE, GREEN);

    /**
     * A {@link List} of {@link PluginParameter}
     */
    static final List<PluginParameter> PARAMETERS2 = PluginParametersFactory.build()
            .addParameterDynamic("param-dyn21", RED, DYNAMICVALUES)
            .addParameterDynamic("param-dyn31", GREEN, DYNAMICVALUES).addParameter("param41", "value41")
            .addParameter("param51", "value51").addParameter("param61", "value61").getParameters();

    /**
     * A list of {@link PluginParameter}
     */
    static final List<PluginParameter> INTERFACEPARAMETERS = PluginParametersFactory.build()
            .addParameter("param31", "value31").addParameter("param32", "value32").addParameter("param33", "value33")
            .addParameter("param34", "value34").addParameter("param35", "value35").getParameters();

    /**
     * A {@link PluginConfiguration}
     */
    private final PluginConfiguration pluginConfiguration1 = new PluginConfiguration(getPluginMetaData(),
            "a configuration", INTERFACEPARAMETERS, 0);

    /**
     * A list of {@link PluginParameter} with a dynamic {@link PluginParameter}
     */
    private final PluginConfiguration pluginConfiguration2 = new PluginConfiguration(getPluginMetaData(),
            "second configuration", PARAMETERS2, 0);

    public PluginConfigurationRepositoryStub() {
        getEntities().add(getPluginConfigurationWithDynamicParameter());
        getEntities().add(getPluginConfigurationWithParameters());
    }

    public PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(Integer.class.getCanonicalName());
        pluginMetaData.getInterfaceNames().add("TestInterface");
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

    @Override
    public PluginConfiguration findOneWithPluginParameter(Long pId) {
        return null;
    }

    @Override
    public PluginConfiguration findById(Long pId) {
        return getEntities().stream().filter(e -> e.getId().equals(pId)).findFirst().orElse(null);
    }

    @Override
    public PluginConfiguration findOneByLabel(String pConfigurationLabel) {
        List<PluginConfiguration> confs = getEntities();
        Optional<PluginConfiguration> conf = confs.stream().filter(c -> c.getLabel().equals(pConfigurationLabel))
                .findFirst();
        if (conf.isPresent()) {
            return conf.get();
        }
        return null;
    }

}
