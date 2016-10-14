/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao.stubs;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.RepositoryStub;
import fr.cnes.regards.modules.plugins.dao.IPluginParameterRepository;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;

/***
 * {@link PluginParameter} Repository stub
 * 
 * @author cmertz
 *
 */
@Repository
@Primary
@Profile("test")
public class PluginParameterRepositoryStub extends RepositoryStub<PluginParameter> implements IPluginParameterRepository {

    /**
     * A list of plugin parameters
     */
    private static final List<PluginParameter> parameters = PluginParametersFactory.build()
            .addParameter("ACTIVE", "true").addParameter("COEFF", "3").addParameter("SUFFIXE", "chris_test_1")
            .getParameters();

    /**
    *
    */
    public PluginParameterRepositoryStub() {
//        parameters.stream().forEach(s -> entities.add(s));
    }

}
