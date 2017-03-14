/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.cnes.regards.modules.access.domain.project.NavigationContext;
import fr.cnes.regards.modules.access.domain.project.Project;
import fr.cnes.regards.modules.access.domain.project.Theme;
import fr.cnes.regards.modules.access.domain.project.ThemeType;

/***
 * Constants and datas for unit testing of NavigationContext's Service.
 * 
 * @author Christophe Mertz
 *
 */
public class NavigationContextUtility {

    

    protected NavigationContext navCtx1 = new NavigationContext("/hello/toulouse",
            new Project("p1", new Theme(null, false, ThemeType.USER)), null, "route1", 330);

    protected NavigationContext navCtx2 = new NavigationContext("/hello/paris",
            new Project("p2", new Theme(null, false, ThemeType.USER)), null, "route2", 331);

    protected NavigationContext navCtx3 = new NavigationContext("/hello/london",
            new Project("p3", new Theme(null, false, ThemeType.USER)), null, "route3", 332);

    protected List<NavigationContext> contexts = new ArrayList<>();
    
    protected List<NavigationContext> getContexts() {
        contexts.clear();
        contexts.add(navCtx1);
        contexts.add(navCtx2);
        contexts.add(navCtx3);
        return contexts;
    }

    protected Iterable<NavigationContext> getContextsIterale() {
        final Iterable<NavigationContext> iter = new Iterable<NavigationContext>() {

            @Override
            public Iterator<NavigationContext> iterator() {
                return getContexts().iterator();
            }
        };
        return iter;
    }
}
