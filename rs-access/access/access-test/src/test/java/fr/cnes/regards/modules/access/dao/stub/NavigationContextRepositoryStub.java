/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.dao.stub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.RepositoryStub;
import fr.cnes.regards.modules.access.dao.INavigationContextRepository;
import fr.cnes.regards.modules.access.domain.ConfigParameter;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.domain.Project;
import fr.cnes.regards.modules.access.domain.Theme;
import fr.cnes.regards.modules.access.domain.ThemeType;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Repository
@Primary
@Profile("test")
public class NavigationContextRepositoryStub extends RepositoryStub<NavigationContext>
        implements INavigationContextRepository {

    private static List<NavigationContext> navigationContexts = new ArrayList<>();

    @PostConstruct
    public final void init() {
        List<ConfigParameter> themeParameters = Arrays.asList(new ConfigParameter("param 1 ", "value 1"),
                                                              new ConfigParameter("param 2 ", "value 2"));
        List<ConfigParameter> navCtxtParameters = Arrays
                .asList(new ConfigParameter("param 1 ", "value 1"), new ConfigParameter("param 2 ", "value 2"),
                        new ConfigParameter("param 3 ", "value 3"), new ConfigParameter("param 4 ", "value 4"));

        navigationContexts.add(new NavigationContext("dMLKMLK5454",
                new Project("project1", new Theme(themeParameters, true, ThemeType.ALL)), navCtxtParameters,
                "http:/localhost:port/webapps/url", 95));
        navigationContexts.add(new NavigationContext("AbcD12345",
                new Project("project2", new Theme(themeParameters, true, ThemeType.PORTAL)), navCtxtParameters,
                "http:/localhost:port/webapps/url2", 133));
        navigationContexts.add(new NavigationContext("dj45Kjkdjskjd",
                new Project("project3", new Theme(themeParameters, true, ThemeType.ADMIN)), navCtxtParameters,
                "http:/localhost:port/webapps/url3", 65));

        final Long n = 50L;
        navigationContexts.stream().forEach(nvCtx -> nvCtx.setId(n + 1));

        getEntities().addAll(navigationContexts);

    }

}