/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.provider;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.provider.IProjectsProvider;

/**
 * @author svissier
 *
 */
@Component
@Primary
public class ProjectsProviderStub implements IProjectsProvider {

    @Override
    public List<String> retrieveProjectList() {
        final List<String> projectListStub = new ArrayList<String>(2);
        projectListStub.add("PROJECT1");
        projectListStub.add("PROJECT2");
        return projectListStub;
    }

}
