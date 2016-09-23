/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.access.domain.ConfigParameter;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.domain.Project;
import fr.cnes.regards.modules.access.domain.Theme;
import fr.cnes.regards.modules.access.domain.ThemeType;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

/**
 * 
 * @author cmertz
 *
 */
@Profile("dev")
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

        navigationContexts.add(new NavigationContext("dMLKMLK5454",
                new Project("project1", new Theme(themeParameters, true, ThemeType.ALL)), navCtxtParameters,
                "http:/localhost:port/webapps/url", 95));
        navigationContexts.add(new NavigationContext("AbcD12345",
                new Project("project2", new Theme(themeParameters, true, ThemeType.PORTAL)), navCtxtParameters,
                "http:/localhost:port/webapps/url2", 133));
        navigationContexts.add(new NavigationContext("dj45Kjkdjskjd",
                new Project("project3", new Theme(themeParameters, true, ThemeType.ADMIN)), navCtxtParameters,
                "http:/localhost:port/webapps/url3", 65));

    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.access.service.INavigationContextService#get(java .lang.String)
     */
    @Override
    public NavigationContext load(String pTinyUrl) throws NoSuchElementException {
        NavigationContext navigationContext = navigationContexts.stream().filter(p -> p.getTinyUrl().equals(pTinyUrl))
                .findFirst().get();
        if (navigationContext == null) {
            throw new NoSuchElementException(pTinyUrl);
        }
        return navigationContext;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.access.service.INavigationContextService#put(java .lang.String)
     */
    @Override
    public void update(String pTinyUrl, NavigationContext pNavigationContext)
            throws OperationNotSupportedException, NoSuchElementException {
        NavigationContext navigationContext = this.load(pTinyUrl);
        if (navigationContext == null) {
            throw new NoSuchElementException(pTinyUrl);
        }
        if (!pNavigationContext.getTinyUrl().equals(pTinyUrl)) {
            throw new OperationNotSupportedException("pTinyUrl and updated navigation context does not match");
        }
        navigationContexts.stream().map(p -> p.equals(pTinyUrl) ? pTinyUrl : p).collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.access.service.INavigationContextService#delete( java.lang.String)
     */
    @Override
    public void delete(String pTinyUrl) throws NoSuchElementException {
        NavigationContext navigationContext = this.load(pTinyUrl);
        navigationContexts.remove(navigationContext);
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
     * @see fr.cnes.regards.modules.access.service.INavigationContextService#create(
     * fr.cnes.regards.modules.access.domain. NavigationContext)
     */
    @Override
    public NavigationContext create(NavigationContext pNavigationContext) throws AlreadyExistingException {
        pNavigationContext.setTinyUrl("coucou");
        navigationContexts.add(pNavigationContext);

        return pNavigationContext;
    }

    /**
     * 
     * @param pTinyUrl
     * @return true if the tinyUrl exists, false otherwise
     */
    public boolean exists(String pTinyUrl) {
        return navigationContexts.stream().filter(p -> p.getTinyUrl().equals(pTinyUrl)).findFirst().isPresent();
    }

}