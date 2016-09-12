package fr.cnes.regards.modules.access.service;

import java.util.List;

import fr.cnes.regards.modules.access.domain.NavigationContext;

public interface INavigationContextService {

    NavigationContext load(String pTinyUrl);

    void update(String pTinyUrl, NavigationContext pNavigationContext);

    void delete(String pTinyUrl);

    List<NavigationContext> list();

    NavigationContext create(NavigationContext pNavigationContext);

}
