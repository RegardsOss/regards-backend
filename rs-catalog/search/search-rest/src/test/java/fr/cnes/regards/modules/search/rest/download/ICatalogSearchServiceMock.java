package fr.cnes.regards.modules.search.rest.download;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.elasticsearch.search.aggregations.Aggregation;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.aggregation.QueryableAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.domain.PropertyBound;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.SearchException;

@Service
@Primary
public class ICatalogSearchServiceMock implements ICatalogSearchService {

	@Override
	public <S, R extends IIndexable> FacetPage<R> search(MultiValueMap<String, String> allParams,
			SearchKey<S, R> searchKey, List<String> facets, Pageable pageable)
			throws SearchException, OpenSearchUnknownParameter {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S, R extends IIndexable> FacetPage<R> search(ICriterion criterion, SearchKey<S, R> searchKey,
			List<String> facets, Pageable pageable) throws SearchException, OpenSearchUnknownParameter {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R extends IIndexable> FacetPage<R> search(ICriterion criterion, SearchType searchType, List<String> facets,
			Pageable pageable) throws SearchException, OpenSearchUnknownParameter {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocFilesSummary computeDatasetsSummary(MultiValueMap<String, String> allParams,
			SimpleSearchKey<DataObject> searchKey, UniformResourceName dataset, List<DataType> dataTypes)
			throws SearchException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocFilesSummary computeDatasetsSummary(ICriterion criterion, SimpleSearchKey<DataObject> searchKey,
			UniformResourceName dataset, List<DataType> dataTypes) throws SearchException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocFilesSummary computeDatasetsSummary(ICriterion criterion, SearchType searchType,
			UniformResourceName dataset, List<DataType> dataTypes) throws SearchException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E extends AbstractEntity<?>> E get(UniformResourceName urn)
			throws EntityOperationForbiddenException, EntityNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends IIndexable> List<String> retrieveEnumeratedPropertyValues(MultiValueMap<String, String> allParams,
			SearchKey<T, T> searchKey, String propertyPath, int maxCount, String partialText)
			throws SearchException, OpenSearchUnknownParameter {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends IIndexable> List<String> retrieveEnumeratedPropertyValues(ICriterion criterion,
			SearchKey<T, T> searchKey, String propertyPath, int maxCount, String partialText)
			throws SearchException, OpenSearchUnknownParameter {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> retrieveEnumeratedPropertyValues(ICriterion criterion, SearchType searchType,
			String propertyPath, int maxCount, String partialText) throws SearchException, OpenSearchUnknownParameter {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Aggregation> retrievePropertiesStats(ICriterion criterion, SearchType searchType,
			Collection<QueryableAttribute> attributes) throws SearchException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PropertyBound<?>> retrievePropertiesBounds(Set<String> propertyNames, ICriterion parse, SearchType type)
			throws SearchException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAccess(UniformResourceName urn) {
        UniformResourceName urnExpected = UniformResourceName.fromString(CatalogDownloadControllerIT.AIP_ID_OK);
		return urnExpected.equals(urn);
	}

}
