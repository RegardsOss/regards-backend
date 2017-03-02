/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.crawler.dao.IEsRepository;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;

/**
 * {@link IIndexService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class IndexService implements IIndexService {

    /**
     * Elasticsearch repository
     */
    private final IEsRepository repository;

    /**
     * Get current tenant at runtime and allows tenant forcing
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    public IndexService(final IEsRepository repository) {
        this.repository = repository;
    }

    @Override
    public <T> Page<T> search(final Class<T> pClass, final int pPageSize, final ICriterion criterion) {
        final String index = runtimeTenantResolver.getTenant();
        return repository.search(index, pClass, pPageSize, criterion);
    }

    @Override
    public <T> Page<T> search(final Class<T> pClass, final Pageable pPageRequest, final ICriterion criterion) {
        final String index = runtimeTenantResolver.getTenant();
        return repository.search(index, pClass, pPageRequest, criterion);
    }
}
