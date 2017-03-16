/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.crawler.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;

import fr.cnes.regards.modules.crawler.dao.IEsRepository;
import fr.cnes.regards.modules.crawler.domain.IIndexable;
import fr.cnes.regards.modules.crawler.domain.SearchKey;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.facet.FacetType;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.models.domain.EntityType;

@Service
public class IndexerService implements IIndexerService {

    private final IEsRepository repository;

    private static final BiMap<EntityType, Class<? extends AbstractEntity>> TYPE_MAP = EnumHashBiMap
            .create(EntityType.class);
    static {
        TYPE_MAP.put(EntityType.COLLECTION, fr.cnes.regards.modules.entities.domain.Collection.class);
        TYPE_MAP.put(EntityType.DATASET, Dataset.class);
        TYPE_MAP.put(EntityType.DATA, DataObject.class);
        TYPE_MAP.put(EntityType.DOCUMENT, Document.class);
    }

    public IndexerService(IEsRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean createIndex(String pIndex) {
        if (!repository.indexExists(pIndex)) {
            return repository.createIndex(pIndex);
        }
        return true;
    }

    @Override
    public boolean deleteIndex(String pIndex) {
        if (repository.indexExists(pIndex)) {
            return repository.deleteIndex(pIndex);
        }
        return true;
    }

    @Override
    public boolean indexExists(String pIndex) {
        return repository.indexExists(pIndex);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IIndexable> T get(UniformResourceName urn) {
        Class<? extends IIndexable> clazz;
        switch (urn.getEntityType()) {
            case COLLECTION:
                clazz = fr.cnes.regards.modules.entities.domain.Collection.class;
                break;
            case DATA:
                clazz = DataObject.class;
                break;
            case DATASET:
                clazz = Dataset.class;
                break;
            case DOCUMENT:
                clazz = Document.class;
                break;
            default:
                throw new IllegalArgumentException();
        }
        return (T) repository.get(urn.getTenant(), urn.getEntityType().toString(), urn.toString(), clazz);
    }

    @Override
    public boolean saveEntity(String pIndex, IIndexable pEntity) {
        return repository.save(pIndex, pEntity);
    }

    @Override
    public void refresh(String pIndex) {
        repository.refresh(pIndex);
    }

    @Override
    public int saveBulkEntities(String pIndex, IIndexable... pEntities) {
        return repository.saveBulk(pIndex, pEntities);
    }

    @Override
    public int saveBulkEntities(String pIndex, Collection<? extends IIndexable> pEntities) {
        return repository.saveBulk(pIndex, pEntities);
    }

    @Override
    public <T> Page<T> search(SearchKey<T> searchKey, Pageable pPageRequest, ICriterion pCriterion,
            Map<String, FacetType> pFacetsMap, LinkedHashMap<String, Boolean> pAscSortMap) {
        return repository.search(searchKey, pPageRequest, pCriterion, pFacetsMap, pAscSortMap);
    }

    @Override
    public <T extends IIndexable> Page<T> searchAndReturnJoinedEntities(SearchKey<T> searchKey, Pageable pageRequest,
            ICriterion pCriterion) {
        // Create a new SearchKey to search on asked type but to only retrieve tags of found results
        SearchKey<String[]> tagSearchKey = new SearchKey<>(searchKey.getSearchIndex(), searchKey.getSearchType(),
                String[].class);
        // Predicate to filter each tag : it must be a valid URN and this URN must concern wanted result type
        Predicate<String> askedTypePredicate = tag -> UniformResourceName.isValidUrn(tag)
                && (TYPE_MAP.get(UniformResourceName.fromString(tag).getEntityType()) == searchKey.getResultClass());
        // Function to get Entity from its ipId (URN) (from Elasticsearch)
        Function<String, T> toAskedEntityFct = tag -> repository
                .get(searchKey.getSearchIndex(), TYPE_MAP.inverse().get(searchKey.getResultClass()).toString(), tag,
                     searchKey.getResultClass());
        List<T> objects = repository.search(tagSearchKey, pCriterion, "tags", askedTypePredicate, toAskedEntityFct);
        int total = objects.size();
        if (!objects.isEmpty()) {
            objects = objects.subList(pageRequest.getOffset(),
                                      Math.min(pageRequest.getOffset() + pageRequest.getPageSize(), objects.size()));
        }
        return new PageImpl<>(objects, pageRequest, total);
    }

    @Override
    public boolean deleteEntity(String pIndex, IIndexable pEntity) {
        return repository.delete(pIndex, pEntity);
    }

    @Override
    public <T> Page<T> multiFieldsSearch(SearchKey<T> pSearchKey, Pageable pPageRequest, Object pValue,
            String... pFields) {
        return repository.multiFieldsSearch(pSearchKey, pPageRequest, pValue, pFields);
    }
}