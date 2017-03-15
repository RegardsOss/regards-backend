package fr.cnes.regards.modules.access.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.access.dao.INavigationContextRepository;
import fr.cnes.regards.modules.access.domain.NavigationContext;

/**
 *
 * The implementation of {@link INavigationContextService}.
 *
 * @author Christophe Mertz
 */
@Service
public class NavigationContextService implements INavigationContextService {

    /**
     * {@link NavigationContext} JPA Repository
     */
    private INavigationContextRepository navigationContextReposiory;

    /**
     * A constructor with the {@link INavigationContextRepository}
     * 
     * @param pNavigationContextRepository
     *            {@link NavigationContext} JPA Repository
     */
    public NavigationContextService(INavigationContextRepository pNavigationContextRepository) {
        super();
        navigationContextReposiory = pNavigationContextRepository;
    }

    @Override
    public NavigationContext create(NavigationContext pNavigationContext) throws ModuleException {
        final StringBuilder msg = new StringBuilder("Impossible to save a navigation context");

        boolean throwError = false;
        if (pNavigationContext == null) {
            msg.append(". The navigation context cannot be null.");
            throwError = true;
        }
        if (!throwError && pNavigationContext.getTinyUrl() == null) {
            msg.append(". The tinyUrll attribute cannot be null.");
            throwError = true;
        }
        if (!throwError && pNavigationContext.getStore() == null) {
            msg.append(". The store attribute cannot be null.");
            throwError = true;
        }
        if (throwError) {
            throw new ModuleException(msg.toString());
        }
        return navigationContextReposiory.save(pNavigationContext);
    }

    @Override
    public NavigationContext update(NavigationContext pNavigationContext) throws EntityNotFoundException {
        if (!navigationContextReposiory.exists(pNavigationContext.getId())) {
            throw new EntityNotFoundException(pNavigationContext.getId(), NavigationContext.class);
        }
        return navigationContextReposiory.save(pNavigationContext);
    }

    @Override
    public void delete(Long pNavCtxId) throws EntityNotFoundException {
        final NavigationContext aNavigationContext = this.load(pNavCtxId);
        navigationContextReposiory.delete(aNavigationContext.getId());
    }

    @Override
    public NavigationContext load(Long pNavCtxId) throws EntityNotFoundException {
        if (!navigationContextReposiory.exists(pNavCtxId)) {

            throw new EntityNotFoundException(pNavCtxId, NavigationContext.class);
        }

        return navigationContextReposiory.findOne(pNavCtxId);
    }

    @Override
    public List<NavigationContext> list() {
        final Iterable<NavigationContext> navContexts = navigationContextReposiory.findAll();
        return Lists.newArrayList(navContexts);
    }

}
