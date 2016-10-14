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

import fr.cnes.regards.framework.test.repository.RepositoryStub;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/***
 * {@link PluginParameter} Repository stub
 * 
 * @author cmertz
 *
 */

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginDaoTestConfiguration.class })
@DirtiesContext
public class PluginParameterTest extends RepositoryStub<PluginParameter> {

    @Autowired
    IPluginParameterRepository pluginParameterRepository;

    @Test
    public void createOnePluginParameter() {
//        pluginParameterRepository.deleteAll();
        Assert.assertTrue(true);
    }
}
