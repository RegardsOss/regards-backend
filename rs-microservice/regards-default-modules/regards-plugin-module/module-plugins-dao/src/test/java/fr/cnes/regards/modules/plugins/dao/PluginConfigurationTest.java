/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.MissingClaimException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/***
 * {@link PluginParameter} unit testing domain persistence
 *
 * @author cmertz
 *
 */

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginDaoTestConfig.class })
@DirtiesContext
public class PluginConfigurationTest extends PluginDaoTestDataUtility {

    /**
     * IPluginConfigurationRepository
     */
    @Autowired
    private IPluginConfigurationRepository pluginConfigurationRepository;

    /**
     * IPluginParameterRepository
     */
    @Autowired
    private IPluginParameterRepository pluginParameterRepository;

    /**
     * IPluginDynamicValueRepository
     */
    @Autowired
    private IPluginDynamicValueRepository pluginDynamicValueRepository;

    /**
     * Security service to generate tokens.
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createPluginConfiguration() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            Assert.assertEquals(0, pluginConfigurationRepository.count());
            final PluginConfiguration jpaConf = pluginConfigurationRepository.save(getPluginConfiguration());
            Assert.assertEquals(1, pluginConfigurationRepository.count());

            Assert.assertEquals(getPluginConfiguration().getLabel(), jpaConf.getLabel());
            Assert.assertEquals(getPluginConfiguration().getVersion(), jpaConf.getVersion());
            Assert.assertEquals(getPluginConfiguration().getPluginId(), jpaConf.getPluginId());

            pluginConfigurationRepository.deleteAll();
            pluginParameterRepository.deleteAll();
            
        } catch (InvalidJwtException | MissingClaimException e) {
            Assert.fail(INVALID_JWT);
        }
    }
    
    private void deleteAllFromRepository() {
        pluginConfigurationRepository.deleteAll();
        pluginParameterRepository.deleteAll();
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createPluginConfigurationWithParameters() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);
            
            deleteAllFromRepository();

            // save 2 plugins parameters
            pluginParameterRepository.save(PARAMETER1);
            Assert.assertEquals(1, pluginParameterRepository.count());

            PARAMETER2.getDynamicsValues().forEach(d -> pluginDynamicValueRepository.save(d));
            pluginParameterRepository.save(PARAMETER2);
            Assert.assertEquals(2, pluginParameterRepository.count());

            // save a plugin configuration
            final PluginConfiguration aPluginConf = pluginConfigurationRepository.save(getPluginConfiguration());

            // add parameters to the plugin configuration
            aPluginConf.setParameters(Arrays.asList(PARAMETER1, PARAMETER2));

            // save the plugin configuration
            pluginConfigurationRepository.save(aPluginConf);

            Assert.assertEquals(1, pluginConfigurationRepository.count());

            deleteAllFromRepository();

        } catch (InvalidJwtException | MissingClaimException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void updatePluginConfigurationWithParameters() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);
            
            deleteAllFromRepository();

            // save 2 plugins parameters
            pluginParameterRepository.save(PARAMETER1);

            PARAMETER2.getDynamicsValues().forEach(d -> pluginDynamicValueRepository.save(d));
            pluginParameterRepository.save(PARAMETER2);

            Assert.assertEquals(2, pluginParameterRepository.count());

            // save a plugin configuration
            final PluginConfiguration jpaConf = pluginConfigurationRepository.save(getPluginConfiguration());

            // add parameters to the plugin configuration
            jpaConf.setParameters(Arrays.asList(PARAMETER1, PARAMETER2));

            // update the plugin configuration
            pluginConfigurationRepository.save(jpaConf);

            Assert.assertEquals(1, pluginConfigurationRepository.count());

            deleteAllFromRepository();
            
        } catch (InvalidJwtException | MissingClaimException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    // /**
    // * Unit test for the update of a {@link PluginParameter}
    // */
    // @Test
    // public void updatePluginParameter() {
    // jwtService.injectToken(PluginDaoTestDataUtility.PROJECT, USERROLE);
    //
    // pluginParameterRepository.save(PARAMETER1);
    // final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETER2);
    // Assert.assertEquals(paramJpa.getName(), PARAMETER2.getName());
    // Assert.assertEquals(2, pluginParameterRepository.count());
    //
    // pluginParameterRepository.save(paramJpa);
    // Assert.assertEquals(2, pluginParameterRepository.count());
    //
    // final PluginParameter paramFound = pluginParameterRepository.findOne(paramJpa.getId());
    // Assert.assertEquals(paramFound.getName(), paramJpa.getName());
    //
    // pluginParameterRepository.deleteAll();
    // Assert.assertEquals(0, pluginParameterRepository.count());
    // }
    //
    // /**
    // * Unit test for the delete of a {@link PluginParameter}
    // */
    //
    // @Test
    // public void deletePluginParameter() {
    // jwtService.injectToken(PluginDaoTestDataUtility.PROJECT, USERROLE);
    //
    // pluginParameterRepository.save(PARAMETER1);
    // final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETER2);
    // Assert.assertEquals(2, pluginParameterRepository.count());
    //
    // pluginParameterRepository.delete(paramJpa);
    // Assert.assertEquals(1, pluginParameterRepository.count());
    //
    // pluginParameterRepository.deleteAll();
    // Assert.assertEquals(0, pluginParameterRepository.count());
    // }

}
