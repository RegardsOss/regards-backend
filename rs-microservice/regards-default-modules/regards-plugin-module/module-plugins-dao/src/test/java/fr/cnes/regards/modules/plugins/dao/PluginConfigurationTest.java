/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

/***
 * Unit testing of {@link PluginConfiguration} persistence.
 *
 * @author Christophe Mertz
 *
 */

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginDaoTestConfig.class })
@DirtiesContext
public class PluginConfigurationTest extends PluginDaoUtility {

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

            final PluginConfiguration jpaConf = pluginConfigurationRepository
                    .save(getPluginConfigurationWithParameters());
            Assert.assertEquals(1, pluginConfigurationRepository.count());

            Assert.assertEquals(getPluginConfigurationWithParameters().getLabel(), jpaConf.getLabel());
            Assert.assertEquals(getPluginConfigurationWithParameters().getVersion(), jpaConf.getVersion());
            Assert.assertEquals(getPluginConfigurationWithParameters().getPluginId(), jpaConf.getPluginId());
            Assert.assertEquals(getPluginConfigurationWithParameters().isActive(), jpaConf.isActive());
            Assert.assertEquals(getPluginConfigurationWithParameters().getPluginClassName(),
                                jpaConf.getPluginClassName());
            Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                                pluginParameterRepository.count());
            Assert.assertEquals(getPluginConfigurationWithParameters().getPriorityOrder(), jpaConf.getPriorityOrder());
            getPluginConfigurationWithParameters().getParameters()
                    .forEach(p -> Assert
                            .assertEquals(getPluginConfigurationWithParameters().getParameterConfiguration(p.getName()),
                                          jpaConf.getParameterConfiguration(p.getName())));
        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createAndFindPluginConfigurationWithParameters() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            // save a plugin configuration
            final PluginConfiguration aPluginConf = pluginConfigurationRepository
                    .save(getPluginConfigurationWithParameters());
            Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                                pluginParameterRepository.count());
            Assert.assertEquals(1, pluginConfigurationRepository.count());

            // find it
            final PluginConfiguration jpaConf = pluginConfigurationRepository.findOne(aPluginConf.getId());

            // compare the initial conf with the results of the search
            Assert.assertEquals(aPluginConf.getLabel(), jpaConf.getLabel());
            Assert.assertEquals(aPluginConf.getVersion(), jpaConf.getVersion());
            Assert.assertEquals(aPluginConf.getPluginId(), jpaConf.getPluginId());
            Assert.assertEquals(aPluginConf.isActive(), jpaConf.isActive());
            Assert.assertEquals(aPluginConf.getPluginClassName(), jpaConf.getPluginClassName());
            Assert.assertEquals(aPluginConf.getParameters().size(), pluginParameterRepository.count());
            Assert.assertEquals(aPluginConf.getPriorityOrder(), jpaConf.getPriorityOrder());
            aPluginConf.getParameters()
                    .forEach(p -> Assert.assertEquals(aPluginConf.getParameterConfiguration(p.getName()),
                                                      jpaConf.getParameterConfiguration(p.getName())));
        } catch (JwtException e) {
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

            // save a plugin configuration
            final PluginConfiguration aPluginConf = pluginConfigurationRepository
                    .save(getPluginConfigurationWithParameters());

            // set two new parameters to the plugin configuration
            aPluginConf.setParameters(Arrays.asList(PARAMETER2));

            // update the plugin configuration
            final PluginConfiguration jpaConf = pluginConfigurationRepository.save(aPluginConf);

            Assert.assertEquals(1, pluginConfigurationRepository.count());

            // compare the initial conf with the results of the search
            Assert.assertEquals(aPluginConf.getLabel(), jpaConf.getLabel());
            Assert.assertEquals(aPluginConf.getVersion(), jpaConf.getVersion());
            Assert.assertEquals(aPluginConf.getPluginId(), jpaConf.getPluginId());
            Assert.assertEquals(aPluginConf.isActive(), jpaConf.isActive());
            Assert.assertEquals(aPluginConf.getPluginClassName(), jpaConf.getPluginClassName());
            Assert.assertEquals(aPluginConf.getPriorityOrder(), jpaConf.getPriorityOrder());
            aPluginConf.getParameters()
                    .forEach(p -> Assert.assertEquals(aPluginConf.getParameterConfiguration(p.getName()),
                                                      jpaConf.getParameterConfiguration(p.getName())));

            INTERFACEPARAMETERS.forEach(p -> pluginParameterRepository.delete(p));
            Assert.assertEquals(aPluginConf.getParameters().size(), pluginParameterRepository.count());

        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    @Before
    public void deleteAllFromRepository() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);
        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
        pluginConfigurationRepository.deleteAll();
        pluginParameterRepository.findAll().forEach(p -> pluginParameterRepository.delete(p));
        resetId();
    }

}
