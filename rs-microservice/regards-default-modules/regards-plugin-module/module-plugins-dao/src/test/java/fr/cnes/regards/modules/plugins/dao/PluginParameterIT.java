/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.plugins.domain.PluginDynamicValue;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/***
 * Unit testing of {@link PluginParameter} persistence.
 *
 * @author Christophe Mertz
 *
 */

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginDaoTestConfig.class })
@DirtiesContext
public class PluginParameterIT extends PluginDaoUtility {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterIT.class);

    /**
     * Constant {@link String} invalid JWT
     */
    private static String INVALID_JWT = "Invalid JWT";

    /**
     * Security service to generate tokens.
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Unit testing for the creation of a {@link PluginParameter}
     */
    @Test
    public void createPluginParameter() {

        try {
            jwtService.injectToken(PROJECT, USERROLE);
            cleanDb();

            final long nPluginParameter = pluginParameterRepository.count();

            pluginParameterRepository.save(A_PARAMETER);
            pluginParameterRepository.save(PARAMETERS2);

            Assert.assertEquals(nPluginParameter + 1 + PARAMETERS2.size(), pluginParameterRepository.count());

            int count = 0;
            for (PluginParameter plgParam : PARAMETERS2) {
                if (plgParam.isDynamic()) {
                    count += plgParam.getDynamicsValues().size();
                }
            }
            Assert.assertEquals(count, pluginDynamicValueRepository.count());
        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit testing for the update of a {@link PluginParameter}
     */
    @Test
    @DirtiesContext
    public void updatePluginParameter() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);
            cleanDb();

            pluginParameterRepository.save(INTERFACEPARAMETERS.get(0));
            final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETERS2.get(0));
            Assert.assertEquals(paramJpa.getName(), PARAMETERS2.get(0).getName());

            pluginParameterRepository.findAll().forEach(p -> LOGGER.info(p.getName()));

            pluginParameterRepository.save(paramJpa);

            final PluginParameter paramFound = pluginParameterRepository.findOne(paramJpa.getId());
            Assert.assertEquals(paramFound.getName(), paramJpa.getName());

        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit testing for the delete of a {@link PluginParameter}
     */
    @Test
    @DirtiesContext
    public void deletePluginParameter() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);
            cleanDb();

            final PluginParameter paramJpa = pluginParameterRepository.save(A_PARAMETER);
            pluginParameterRepository.save(PARAMETERS2);
            Assert.assertEquals(1 + PARAMETERS2.size(), pluginParameterRepository.count());

            pluginParameterRepository.delete(paramJpa);
            Assert.assertEquals(PARAMETERS2.size(), pluginParameterRepository.count());
            
            int count = 0;
            for (PluginParameter plgParam : PARAMETERS2) {
                if (plgParam.isDynamic()) {
                    count += plgParam.getDynamicsValues().size();
                }
            }
            
            Assert.assertEquals(count, pluginDynamicValueRepository.count());
            
            pluginParameterRepository.delete(PARAMETERS2.get(0));
            Assert.assertEquals(PARAMETERS2.size()-1, pluginParameterRepository.count());
            
            Assert.assertEquals(count-PARAMETERS2.get(0).getDynamicsValues().size(), pluginDynamicValueRepository.count());

            pluginDynamicValueRepository.deleteAll();
            
        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }
    
    @Test(expected=DataIntegrityViolationException.class)
    public void deletePluginDynamicVaueError() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);
            cleanDb();

            pluginParameterRepository.save(PARAMETERS2);
            pluginDynamicValueRepository.deleteAll();
            
            Assert.fail();
            
        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit testing about the dynamic values of a {@link PluginParameter}
     */
    @Test
    @DirtiesContext
    public void controlPluginParameterDynamicValues() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);
            cleanDb();

            // first plugin parameter
            final PluginParameter savedParam = pluginParameterRepository.save(A_PARAMETER);
            Assert.assertNotNull(savedParam.getId());
            Assert.assertEquals(1, pluginParameterRepository.count());

            // second plugin parameter with dynamic values
            final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETERS2.get(0));
            Assert.assertNotNull(paramJpa.getId());
            Assert.assertEquals(2, pluginParameterRepository.count());

            // search the second plugin parameter
            final PluginParameter paramFound = pluginParameterRepository.findOne(paramJpa.getId());
            paramFound.getDynamicsValues().stream().forEach(p -> LOGGER.info(p.getValue()));

            // test dynamics values of the second parameter
            Assert.assertEquals(paramJpa.isDynamic(), paramFound.isDynamic());
            Assert.assertEquals(paramJpa.getDynamicsValues().size(), paramFound.getDynamicsValues().size());
            Assert.assertEquals(paramJpa.getName(), paramFound.getName());
            Assert.assertEquals(paramJpa.getValue(), paramFound.getValue());
            Assert.assertEquals(paramJpa.getId(), paramFound.getId());
            Assert.assertEquals(paramJpa.getDynamicsValuesAsString().size(),
                                paramFound.getDynamicsValuesAsString().size());
            paramJpa.getDynamicsValuesAsString().stream()
                    .forEach(s -> paramFound.getDynamicsValuesAsString().contains(s));
            
            PluginDynamicValue aDynamicValue= pluginDynamicValueRepository.findOne(paramJpa.getDynamicsValues().get(0).getId());
            Assert.assertEquals(paramJpa.getDynamicsValues().get(0).getValue(), aDynamicValue.getValue());

        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

}
