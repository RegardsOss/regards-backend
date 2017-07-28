package fr.cnes.regards.framework.modules.plugins.service; 
 
import java.util.ArrayList; 
import java.util.List; 
 
import org.junit.Before; 
import org.junit.Test; 
import org.mockito.Mockito; 
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
 
import fr.cnes.regards.framework.amqp.IPublisher; 
import fr.cnes.regards.framework.module.rest.exception.ModuleException; 
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository; 
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration; 
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData; 
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter; 
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver; 
 
public class ComplexPluginTest { 
 
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginServiceTest.class); 
 
    private IPluginConfigurationRepository pluginConfRepositoryMocked; 
 
    private IPluginService pluginServiceMocked; 
 
    private IPublisher publisherMocked; 
 
    private IRuntimeTenantResolver runtimeTenantResolver; 
 
    /** 
     * This method is run before all tests 
     */ 
    @Before 
    public void init() { 
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class); 
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn("tenant"); 
 
        publisherMocked = Mockito.mock(IPublisher.class); 
        // create a mock repository 
        pluginConfRepositoryMocked = Mockito.mock(IPluginConfigurationRepository.class); 
        pluginServiceMocked = new PluginService(pluginConfRepositoryMocked, publisherMocked, runtimeTenantResolver); 
        pluginServiceMocked.addPluginPackage("fr.cnes.regards.framework.modules.plugins.test"); 
    } 
 
    @Test 
    public void test() throws ModuleException { 
        PluginMetaData result = pluginServiceMocked.getPluginMetaDataById("complexPlugin"); 
 
        Long pPluginConfigurationId = 10L; 
        final List<PluginConfiguration> pluginConfs = new ArrayList<>(); 
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration(result, 
                "a configuration from PluginServiceUtility", new ArrayList<PluginParameter>(), 0); 
        aPluginConfiguration.setId(pPluginConfigurationId); 
 
        pluginConfs.add(aPluginConfiguration); 
 
        Mockito.when(pluginConfRepositoryMocked.findByPluginIdOrderByPriorityOrderDesc("complexPlugin")) 
                .thenReturn(pluginConfs); 
        Mockito.when(pluginConfRepositoryMocked.findById(aPluginConfiguration.getId())) 
                .thenReturn(aPluginConfiguration); 
        Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true); 
 
        pluginServiceMocked.getPlugin(pPluginConfigurationId); 
        LOGGER.info("Plop"); 
    } 
 
} 