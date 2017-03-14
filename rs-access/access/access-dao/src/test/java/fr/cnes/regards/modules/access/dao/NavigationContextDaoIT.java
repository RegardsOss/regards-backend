/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.modules.access.dao.project.INavigationContextRepository;

/**
 * @author Christophe Mertz
 *
 */
public class NavigationContextDaoIT extends NavigationContextUtility {

    @Autowired
    private INavigationContextRepository navigationContextRepository;

    @Test
    public void save() {
        injectToken("test1");

//        navigationContextRepository.save(getContexts().get(0));
//        Assert.assertEquals(1, navigationContextRepository.count());

    }

}
