package fr.cnes.regards.modules.storage.plugins.staf;

import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.modules.storage.plugin.staf.STAFDataStorage;
import fr.cnes.regards.plugins.utils.PluginUtils;

@ContextConfiguration(classes = { STAFDataStorageConfiguration.class })
@TestPropertySource(locations = { "classpath:test.properties" })
public class STAFDataStorageTest extends AbstractRegardsServiceIT {

    private static Logger LOG = LoggerFactory.getLogger(STAFDataStorageTest.class);

    @Test
    public void storeTest() {

        List<PluginParameter> parameters = Lists.newArrayList();
        List<String> packages = Lists.newArrayList();
        packages.add("fr.cnes.regards.modules.storage.plugin.staf");

        STAFDataStorage plugin = PluginUtils.getPlugin(parameters, STAFDataStorage.class, packages, Maps.newHashMap());

        plugin.prepare(Sets.newHashSet());

    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
