/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.search.domain.SearchType;
import fr.cnes.regards.modules.search.domain.Terms;

/**
 * Define sample data for tests.
 * @author Xavier-Alexandre Brochard
 */
@SuppressWarnings("unchecked")
public class SampleDataUtils {

    public static final String SEPARATOR = ":";

    public static final String BOOLEAN_FIELD = "isTrue";

    public static final String INTEGER_FIELD = "altitude";

    public static final String INTEGER_RANGE_FIELD = "age";

    public static final String DOUBLE_FIELD = "bpm";

    public static final String DOUBLE_RANGE_FIELD = "height";

    public static final String LONG_FIELD = "speed";

    public static final String LONG_RANGE_FIELD = "distance";

    public static final String STRING_FIELD = "title";

    public static final String STRING_FIELD_1 = "author";

    public static final String STRING_RANGE_FIELD = "movie";

    public static final String LOCAL_DATE_TIME_FIELD = "date";

    public static final String LOCAL_DATE_TIME_RANGE_FIELD = "arrival";

    public static final String INTEGER_ARRAY_FIELD = "years";

    public static final String DOUBLE_ARRAY_FIELD = "duration";

    public static final String LONG_ARRAY_FIELD = "boxoffice";

    public static final String STRING_ARRAY_FIELD = "cast";

    public static final String LOCAL_DATE_TIME_ARRAY = "releases";

    public static final String TAGS_FIELD = "tags";

    // Build some attribute models for all attribute types
    public static final AttributeModel BOOLEAN_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(BOOLEAN_FIELD, AttributeType.BOOLEAN, BOOLEAN_FIELD).get();

    public static final AttributeModel INTEGER_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(INTEGER_FIELD, AttributeType.INTEGER, INTEGER_FIELD).get();

    public static final AttributeModel DOUBLE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(DOUBLE_FIELD, AttributeType.DOUBLE, DOUBLE_FIELD).get();

    public static final AttributeModel LONG_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LONG_FIELD, AttributeType.LONG, LONG_FIELD).get();

    public static final AttributeModel STRING_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(STRING_FIELD, AttributeType.STRING, STRING_FIELD).get();

    public static final AttributeModel STRING_ATTRIBUTE_MODEL_1 = AttributeModelBuilder
            .build(STRING_FIELD_1, AttributeType.STRING, STRING_FIELD_1).get();

    public static final AttributeModel LOCAL_DATE_TIME_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LOCAL_DATE_TIME_FIELD, AttributeType.DATE_ISO8601, LOCAL_DATE_TIME_FIELD).get();

    public static final AttributeModel INTEGER_RANGE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(INTEGER_RANGE_FIELD, AttributeType.INTEGER_INTERVAL, INTEGER_RANGE_FIELD).get();

    public static final AttributeModel DOUBLE_RANGE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(DOUBLE_RANGE_FIELD, AttributeType.DOUBLE_INTERVAL, DOUBLE_RANGE_FIELD).get();

    public static final AttributeModel LONG_RANGE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LONG_RANGE_FIELD, AttributeType.LONG_INTERVAL, LONG_RANGE_FIELD).get();

    public static final AttributeModel LOCAL_DATE_TIME_RANGE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LOCAL_DATE_TIME_RANGE_FIELD, AttributeType.DATE_INTERVAL, LOCAL_DATE_TIME_RANGE_FIELD).get();

    public static final AttributeModel INTEGER_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(INTEGER_ARRAY_FIELD, AttributeType.INTEGER_ARRAY, INTEGER_ARRAY_FIELD).get();

    public static final AttributeModel DOUBLE_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(DOUBLE_ARRAY_FIELD, AttributeType.DOUBLE_ARRAY, DOUBLE_ARRAY_FIELD).get();

    public static final AttributeModel LONG_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LONG_ARRAY_FIELD, AttributeType.LONG_ARRAY, LONG_ARRAY_FIELD).get();

    public static final AttributeModel STRING_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(STRING_ARRAY_FIELD, AttributeType.STRING_ARRAY, STRING_ARRAY_FIELD).get();

    public static final AttributeModel LOCAL_DATE_TIME_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LOCAL_DATE_TIME_ARRAY, AttributeType.DATE_ARRAY, LOCAL_DATE_TIME_ARRAY).get();

    public static final AttributeModel TAGS_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(TAGS_FIELD, AttributeType.STRING_ARRAY, TAGS_FIELD).get();

    public static final List<AttributeModel> LIST = Lists
            .newArrayList(BOOLEAN_ATTRIBUTE_MODEL, INTEGER_ATTRIBUTE_MODEL, DOUBLE_ATTRIBUTE_MODEL,
                          LONG_ATTRIBUTE_MODEL, STRING_ATTRIBUTE_MODEL, STRING_ATTRIBUTE_MODEL_1,
                          LOCAL_DATE_TIME_ATTRIBUTE_MODEL, INTEGER_RANGE_ATTRIBUTE_MODEL, DOUBLE_RANGE_ATTRIBUTE_MODEL,
                          LONG_RANGE_ATTRIBUTE_MODEL, LOCAL_DATE_TIME_RANGE_ATTRIBUTE_MODEL,
                          INTEGER_ARRAY_ATTRIBUTE_MODEL, DOUBLE_ARRAY_ATTRIBUTE_MODEL, LONG_ARRAY_ATTRIBUTE_MODEL,
                          STRING_ARRAY_ATTRIBUTE_MODEL, LOCAL_DATE_TIME_ARRAY_ATTRIBUTE_MODEL, TAGS_ATTRIBUTE_MODEL);

    /**
     * Sample response from the {@link IAttributeModelClient}
     */
    public static final ResponseEntity<List<Resource<AttributeModel>>> ATTRIBUTE_MODEL_CLIENT_RESPONSE = ResponseEntity
            .ok(HateoasUtils.wrapList(SampleDataUtils.LIST));

    /**
     * A dummy access group name
     */
    public static final String ACCESS_GROUP_NAME_0 = "accessGroup0";

    /**
     * A dummy access group name
     */
    public static final String ACCESS_GROUP_NAME_1 = "accessGroup1";

    /**
     * A dummy access group
     */
    public static final AccessGroup ACCESS_GROUP_0 = new AccessGroup(ACCESS_GROUP_NAME_0);

    /**
     * A dummy access group
     */
    public static final AccessGroup ACCESS_GROUP_1 = new AccessGroup(ACCESS_GROUP_NAME_1);

    /**
     * Sample response from the {@link IUserClient}
     */
    public static final ResponseEntity<PagedResources<Resource<AccessGroup>>> USER_CLIENT_RESPONSE = ResponseEntity
            .ok(HateoasUtils.wrapToPagedResources(Lists.newArrayList(ACCESS_GROUP_0, ACCESS_GROUP_1)));

    public static final ResponseEntity<Boolean> PROJECT_USERS_CLIENT_RESPONSE = ResponseEntity.ok(Boolean.FALSE);

    /**
     * Dummy OpenSearch request
     */
    public static final String QUERY = INTEGER_FIELD + ":(2 AND 3) OR " + STRING_FIELD + ":hello";

    /**
     * A query with no "groups" term
     */
    public static final String QUERY_WITH_NO_GROUPS = QUERY;

    /**
     * A query with a term "groups"
     */
    public static final String QUERY_WITH_GROUPS = INTEGER_FIELD + ":(2 AND 3) OR " + Terms.GROUPS + ":admin";

    /**
     * A dummy assembler for collections
     */
    public static final PagedResourcesAssembler<Collection> ASSEMBLER_COLLECTION = Mockito
            .mock(PagedResourcesAssembler.class);

    /**
     * A dummy assembler for dataobjects
     */
    public static final PagedResourcesAssembler<DataObject> ASSEMBLER_DATAOBJECT = Mockito
            .mock(PagedResourcesAssembler.class);

    /**
     * A dummy assembler for datasets
     */
    public static final PagedResourcesAssembler<Dataset> ASSEMBLER_DATASET = Mockito
            .mock(PagedResourcesAssembler.class);

    /**
     * A dummy assembler for documents
     */
    public static final PagedResourcesAssembler<Document> ASSEMBLER_DOCUMENT = Mockito
            .mock(PagedResourcesAssembler.class);

    /**
     * A dummy assembler for entities
     */
    public static final PagedResourcesAssembler<AbstractEntity> ASSEMBLER_ABSTRACT_ENTITIES = Mockito
            .mock(PagedResourcesAssembler.class);

    /**
     * A dummy collection
     */
    public static final Collection COLLECTION = new Collection();

    /**
     * A dummy dataobject
     */
    public static final DataObject DATAOBJECT = new DataObject();

    /**
     * A dummy dataset
     */
    public static final Dataset DATASET = new Dataset();

    /**
     * A dummy document
     */
    public static final Document DOCUMENT = new Document();

    /**
     * A dummy list of facets
     */
    public static final Map<String, FacetType> FACETS = new ImmutableMap.Builder<String, FacetType>()
            .put("integer", FacetType.NUMERIC).put("string", FacetType.STRING).build();

    /**
     * A dummy page of dataobjects
     */
    public static final Page<DataObject> PAGE_DATAOBJECT = new PageImpl<>(Lists.newArrayList(DATAOBJECT));

    /**
     * A dummy page of dataobjects
     */
    public static final Page<Dataset> PAGE_DATASET = new PageImpl<>(Lists.newArrayList(DATASET));

    /**
     * A mock pageable
     */
    public static final Pageable PAGEABLE = Mockito.mock(Pageable.class);

    /**
     * A dummy paged resources of dataobjects
     */
    public static final PagedResources<Resource<DataObject>> PAGED_RESOURCES_DATAOBJECT = new PagedResources<Resource<DataObject>>(
            new ArrayList<>(), null, new Link("href"));

    /**
     * A dummy paged resources of dataset
     */
    public static final PagedResources<Resource<Dataset>> PAGED_RESOURCES_DATASET = new PagedResources<Resource<Dataset>>(
            new ArrayList<>(), null, new Link("href"));

    /**
     * A criterion string match
     */
    public static final ICriterion SIMPLE_STRING_MATCH_CRITERION = ICriterion.eq("field", "value");

    /**
     * Define a criterion with a nested criterion of name "target" (this must be detected and properly handled)
     */
    public static final ICriterion CRITERION_WITH_NESTED_TARGET_FIELD = ICriterion.or(
                                                                                      ICriterion.eq("target",
                                                                                                    SearchType.DATASET
                                                                                                            .toString()),
                                                                                      ICriterion.eq("field", "value"));

    /**
     * Define a criterion with a nested criterion of name "dataset" (this must be detected and properly handled)
     */
    public static final ICriterion CRITERION_WITH_NESTED_DATASET_FIELD = ICriterion
            .or(ICriterion.eq("dataset", "whatever"), ICriterion.eq("field", "value"));

    /**
     * Define a criterion with a nested criterion of name "datasets" (this must be detected and properly handled)
     */
    public static final ICriterion CRITERION_WITH_NESTED_DATASETS_FIELD = ICriterion
            .or(ICriterion.eq("datasets", "whatever"), ICriterion.eq("field", "value"));

    /**
     * A dummy sort
     */
    public static final Sort SORT = new Sort("date");

    /**
     * A dummy tenant
     */
    public static final String TENANT = "tenant";

    /**
     * A dummy urn of a collection
     */
    public static final UniformResourceName URN_COLLECTION = new UniformResourceName();

    /**
     * A dummy urn for a dataobject
     */
    public static final UniformResourceName URN_DATAOBJECT = new UniformResourceName();

    /**
     * A dummy urn for a dataset
     */
    public static final UniformResourceName URN_DATASET = new UniformResourceName();

    /**
     * A dummy urn for a document
     */
    public static final UniformResourceName URN_DOCUMENT = new UniformResourceName();

    /**
     * A sample email representing the current user
     */
    public static final String EMAIL = "user@email.com";

}
