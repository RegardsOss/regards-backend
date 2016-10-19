/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
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
public class PluginParameterTest extends PluginDaoTestDataUtility {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterTest.class);

    /**
     * Constant {@link String} invalid JWT
     */
    private static String INVALID_JWT = "Invalid JWT";

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
     * Unit test for the creation of a {@link PluginParameter}
     */
    @Test
    public void createPluginParameter() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            deleteAllFromRepository();

            pluginParameterRepository.save(PARAMETER1);
            Assert.assertEquals(1, pluginParameterRepository.count());

            pluginParameterRepository.save(PARAMETER2);
            Assert.assertEquals(PARAMETER2.getDynamicsValues().size(), pluginDynamicValueRepository.count());

            Assert.assertEquals(2, pluginParameterRepository.count());

        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit test for the update of a {@link PluginParameter}
     */
    @Test
    public void updatePluginParameter() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            deleteAllFromRepository();

            pluginParameterRepository.save(PARAMETER1);
            final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETER2);
            Assert.assertEquals(paramJpa.getName(), PARAMETER2.getName());

            pluginParameterRepository.findAll().forEach(p -> LOGGER.info(p.getName()));

            Assert.assertEquals(2, pluginParameterRepository.count());

            pluginParameterRepository.save(paramJpa);
            Assert.assertEquals(2, pluginParameterRepository.count());

            final PluginParameter paramFound = pluginParameterRepository.findOne(paramJpa.getId());
            Assert.assertEquals(paramFound.getName(), paramJpa.getName());

        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit test for the delete of a {@link PluginParameter}
     */
    @Test
    public void deletePluginParameter() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            deleteAllFromRepository();

            pluginParameterRepository.save(PARAMETER1);
            final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETER2);
            Assert.assertEquals(2, pluginParameterRepository.count());

            pluginParameterRepository.delete(paramJpa);
            Assert.assertEquals(1, pluginParameterRepository.count());

        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit test about the dynamic values of a {@link PluginParameter}
     */
    @Test
    public void controlPluginParameterDynamicValues() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            deleteAllFromRepository();

            // first pluginb parameter
            pluginParameterRepository.save(PARAMETER1);

            // second plugin parameter with dynamic values
            final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETER2);

            // search the second plugin parameter
            final PluginParameter paramFound = pluginParameterRepository.findOne(paramJpa.getId());
            paramFound.getDynamicsValues().stream().forEach(p -> LOGGER.info(p.getValue()));

            // test dynamics values of the second parameter
            Assert.assertEquals(paramJpa.getIsDynamic(), paramJpa.getIsDynamic());
            Assert.assertEquals(paramJpa.getDynamicsValues().size(), paramFound.getDynamicsValues().size());

        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    private void deleteAllFromRepository() {
        pluginParameterRepository.deleteAll();
        pluginDynamicValueRepository.deleteAll();
        resetId();
    }

}
