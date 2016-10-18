/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao;

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

    private static String INVALID_JWT = "Invalid JWT";

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

    @Test
    public void createPluginParameter() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            pluginParameterRepository.save(PARAMETER1);
            Assert.assertEquals(1, pluginParameterRepository.count());

            pluginParameterRepository.save(PARAMETER2);
            Assert.assertEquals(2, pluginParameterRepository.count());

            pluginParameterRepository.deleteAll();
            Assert.assertEquals(0, pluginParameterRepository.count());
        } catch (InvalidJwtException | MissingClaimException e) {
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

            pluginParameterRepository.save(PARAMETER1);
            final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETER2);
            Assert.assertEquals(paramJpa.getName(), PARAMETER2.getName());
            Assert.assertEquals(2, pluginParameterRepository.count());

            pluginParameterRepository.save(paramJpa);
            Assert.assertEquals(2, pluginParameterRepository.count());

            final PluginParameter paramFound = pluginParameterRepository.findOne(paramJpa.getId());
            Assert.assertEquals(paramFound.getName(), paramJpa.getName());

            pluginParameterRepository.deleteAll();
            Assert.assertEquals(0, pluginParameterRepository.count());
        } catch (InvalidJwtException | MissingClaimException e) {
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

            pluginParameterRepository.save(PARAMETER1);
            final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETER2);
            Assert.assertEquals(2, pluginParameterRepository.count());

            pluginParameterRepository.delete(paramJpa);
            Assert.assertEquals(1, pluginParameterRepository.count());

            pluginParameterRepository.deleteAll();
            Assert.assertEquals(0, pluginParameterRepository.count());
        } catch (InvalidJwtException | MissingClaimException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit test for the delete of a {@link PluginParameter}
     */
    @Test
    public void setPluginParameter() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            pluginParameterRepository.save(PARAMETER1);
            final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETER2);
            Assert.assertEquals(2, pluginParameterRepository.count());

            pluginParameterRepository.deleteAll();
            Assert.assertEquals(0, pluginParameterRepository.count());
        } catch (InvalidJwtException | MissingClaimException e) {
            Assert.fail(INVALID_JWT);
        }
    }

}
