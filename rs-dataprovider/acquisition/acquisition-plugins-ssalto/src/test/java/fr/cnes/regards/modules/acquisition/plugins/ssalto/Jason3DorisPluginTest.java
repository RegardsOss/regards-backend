/*
 * $Id$
 *
 * HISTORIQUE
 *
 * VERSION : 5.5 : DM : SIPNG-DM-154-CN : 03/12/2014 : Création plugins JASON3
 *
 * FIN-HISTORIQUE
 */
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;

/**
 * Test des plugins DORIS10 JASON3
 *
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class Jason3DorisPluginTest extends Jason3PluginTest {

    @Autowired
    IPluginService pluginService;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResoler;

    @Before
    public void start() {
        runtimeTenantResoler.forceTenant(DEFAULT_TENANT);
    }

    @Override
    public IGenerateSIPPlugin buildPlugin() throws ModuleException {
        PluginConfiguration pluginConfiguration = this.getPluginConfiguration("Jason3Doris10ProductMetadataPlugin");

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_JASON3_DORIS10_FLAG", "JASON3/DORIS10/DOR10_INVALIDES");
        addPluginTestDef("DA_TC_JASON3_DORIS10_COM", "JASON3/DORIS10/COMMERCIALES_10");
        addPluginTestDef("DA_TC_JASON3_DORIS10_PUB", "JASON3/DORIS10/PUBLIQUES_10");
    }

}
