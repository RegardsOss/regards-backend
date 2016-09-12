package fr.cnes.regards.modules.access.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.access.domain.ConfigParameter;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.domain.Project;
import fr.cnes.regards.modules.access.domain.Theme;
import fr.cnes.regards.modules.access.domain.ThemeType;

@Service
public class NavigationContextServiceStub implements INavigationContextService {

    private static List<NavigationContext> navigationContexts = new ArrayList<>();

    @PostConstruct
    public void init() {
        List<ConfigParameter> themeParameters = Arrays.asList(new ConfigParameter("param 1 ", "value 1"),
                                                              new ConfigParameter("param 2 ", "value 2"));
        List<ConfigParameter> navCtxtParameters = Arrays
                .asList(new ConfigParameter("param 1 ", "value 1"), new ConfigParameter("param 2 ", "value 2"),
                        new ConfigParameter("param 3 ", "value 3"), new ConfigParameter("param 4 ", "value 4"));

        navigationContexts
        .add(new NavigationContext(new Project("project1", new Theme(themeParameters, true, ThemeType.ALL)),
                                   navCtxtParameters, "http:/localhost:port/webapps/url", 95));
        navigationContexts
        .add(new NavigationContext(new Project("project2", new Theme(themeParameters, true, ThemeType.PORTAL)),
                                   navCtxtParameters, "http:/localhost:port/webapps/url2", 133));
        navigationContexts
        .add(new NavigationContext(new Project("project3", new Theme(themeParameters, true, ThemeType.ADMIN)),
                                   navCtxtParameters, "http:/localhost:port/webapps/url3", 65));

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.access.service.INavigationContextService#get(java.lang.String)
     */
    @Override
    public NavigationContext load(String pTinyUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.access.service.INavigationContextService#put(java.lang.String)
     */
    @Override
    public void update(String pTinyUrl, NavigationContext pNavigationContext) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.access.service.INavigationContextService#delete(java.lang.String)
     */
    @Override
    public void delete(String pTinyUrl) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.access.service.INavigationContextService#list()
     */
    @Override
    public List<NavigationContext> list() {
        return navigationContexts;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.access.service.INavigationContextService#create(fr.cnes.regards.modules.access.domain.
     * NavigationContext)
     */
    @Override
    public NavigationContext create(NavigationContext pNavigationContext) {
        // TODO Auto-generated method stub
        return null;
    }

}