package fr.cnes.regards.modules.access.service;

import java.util.List;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.access.dao.INavigationContextRepository;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

/**
 *
 * The implementation of {@link INavigationContextService}.
 *
 * @author Christophe Mertz
 */
@Service
public class NavigationContextService implements INavigationContextService {

    INavigationContextRepository navigationContextReposiory;

    /**
     * A constructor with the {@link INavigationContextRepository}
     * 
     * @param pNavigationContextRepository
     */
    public NavigationContextService(INavigationContextRepository pNavigationContextRepository) {
        super();
        navigationContextReposiory = pNavigationContextRepository;
    }

    @Override
    public NavigationContext create(NavigationContext pNavigationContext) throws AlreadyExistingException {
        // TODO CMZ tester que tout est OK avant de sauver
        return navigationContextReposiory.save(pNavigationContext);
    }

    @Override
    public void update(NavigationContext pNavigationContext) throws EntityNotFoundException {
        if (!navigationContextReposiory.exists(pNavigationContext.getId())) {
            throw new EntityNotFoundException(
                    String.format("Error while updating the navigation context <%s>.", pNavigationContext.getId()),
                    NavigationContext.class);
        }
        navigationContextReposiory.save(pNavigationContext);
    }

    @Override
    public void delete(Long pNavCtxId) throws EntityNotFoundException {
        NavigationContext aNavigationContext = this.load(pNavCtxId);
        navigationContextReposiory.delete(aNavigationContext.getId());
    }

    @Override
    public NavigationContext load(Long pNavCtxId) throws EntityNotFoundException {
        final NavigationContext navContexts = navigationContextReposiory.findOne(pNavCtxId);

        if (navContexts == null) {
            throw new EntityNotFoundException(
                    String.format("Error while getting the navigation context with tiny URL <%l>.", pNavCtxId),
                    NavigationContext.class);
        }

        return navContexts;
    }

    @Override
    public List<NavigationContext> list() {
        final Iterable<NavigationContext> navContexts = navigationContextReposiory.findAll();
        return IterableUtils.toList(navContexts);
    }

}
