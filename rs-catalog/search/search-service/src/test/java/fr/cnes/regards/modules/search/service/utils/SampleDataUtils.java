/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.search.service.utils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Collection;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.search.domain.Terms;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Define sample data for tests.
 *
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

    public static final Fragment TEST_FRAGMENT = Fragment.buildDefault();

    // Build some attribute models for all attribute types
    public static final AttributeModel BOOLEAN_ATTRIBUTE_MODEL = new AttributeModelBuilder(BOOLEAN_FIELD,
                                                                                           PropertyType.BOOLEAN,
                                                                                           BOOLEAN_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel INTEGER_ATTRIBUTE_MODEL = new AttributeModelBuilder(INTEGER_FIELD,
                                                                                           PropertyType.INTEGER,
                                                                                           INTEGER_FIELD).setFragment(
        TEST_FRAGMENT).build();

    /**
     * A query with a term "groups"
     */
    public static final String QUERY_WITH_GROUPS = INTEGER_ATTRIBUTE_MODEL.getJsonPath()
                                                   + ":(2 AND 3) OR "
                                                   + Terms.GROUPS
                                                   + ":admin";

    public static final AttributeModel DOUBLE_ATTRIBUTE_MODEL = new AttributeModelBuilder(DOUBLE_FIELD,
                                                                                          PropertyType.DOUBLE,
                                                                                          DOUBLE_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel LONG_ATTRIBUTE_MODEL = new AttributeModelBuilder(LONG_FIELD,
                                                                                        PropertyType.LONG,
                                                                                        LONG_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel STRING_ATTRIBUTE_MODEL = new AttributeModelBuilder(STRING_FIELD,
                                                                                          PropertyType.STRING,
                                                                                          STRING_FIELD).setFragment(
        TEST_FRAGMENT).build();

    /**
     * Dummy OpenSearch request
     */
    public static final String QUERY = INTEGER_ATTRIBUTE_MODEL.getJsonPath()
                                       + ":(2 AND 3) OR "
                                       + STRING_ATTRIBUTE_MODEL.getJsonPath()
                                       + ":hello";

    /**
     * A query with no "groups" term
     */
    public static final String QUERY_WITH_NO_GROUPS = QUERY;

    public static final AttributeModel STRING_ATTRIBUTE_MODEL_1 = new AttributeModelBuilder(STRING_FIELD_1,
                                                                                            PropertyType.STRING,
                                                                                            STRING_FIELD_1).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel LOCAL_DATE_TIME_ATTRIBUTE_MODEL = new AttributeModelBuilder(LOCAL_DATE_TIME_FIELD,
                                                                                                   PropertyType.DATE_ISO8601,
                                                                                                   LOCAL_DATE_TIME_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel INTEGER_RANGE_ATTRIBUTE_MODEL = new AttributeModelBuilder(INTEGER_RANGE_FIELD,
                                                                                                 PropertyType.INTEGER_INTERVAL,
                                                                                                 INTEGER_RANGE_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel DOUBLE_RANGE_ATTRIBUTE_MODEL = new AttributeModelBuilder(DOUBLE_RANGE_FIELD,
                                                                                                PropertyType.DOUBLE_INTERVAL,
                                                                                                DOUBLE_RANGE_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel LONG_RANGE_ATTRIBUTE_MODEL = new AttributeModelBuilder(LONG_RANGE_FIELD,
                                                                                              PropertyType.LONG_INTERVAL,
                                                                                              LONG_RANGE_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel LOCAL_DATE_TIME_RANGE_ATTRIBUTE_MODEL = new AttributeModelBuilder(
        LOCAL_DATE_TIME_RANGE_FIELD,
        PropertyType.DATE_INTERVAL,
        LOCAL_DATE_TIME_RANGE_FIELD).setFragment(TEST_FRAGMENT).build();

    public static final AttributeModel INTEGER_ARRAY_ATTRIBUTE_MODEL = new AttributeModelBuilder(INTEGER_ARRAY_FIELD,
                                                                                                 PropertyType.INTEGER_ARRAY,
                                                                                                 INTEGER_ARRAY_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel DOUBLE_ARRAY_ATTRIBUTE_MODEL = new AttributeModelBuilder(DOUBLE_ARRAY_FIELD,
                                                                                                PropertyType.DOUBLE_ARRAY,
                                                                                                DOUBLE_ARRAY_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel LONG_ARRAY_ATTRIBUTE_MODEL = new AttributeModelBuilder(LONG_ARRAY_FIELD,
                                                                                              PropertyType.LONG_ARRAY,
                                                                                              LONG_ARRAY_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel STRING_ARRAY_ATTRIBUTE_MODEL = new AttributeModelBuilder(STRING_ARRAY_FIELD,
                                                                                                PropertyType.STRING_ARRAY,
                                                                                                STRING_ARRAY_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final AttributeModel LOCAL_DATE_TIME_ARRAY_ATTRIBUTE_MODEL = new AttributeModelBuilder(
        LOCAL_DATE_TIME_ARRAY,
        PropertyType.DATE_ARRAY,
        LOCAL_DATE_TIME_ARRAY).setFragment(TEST_FRAGMENT).build();

    public static final AttributeModel TAGS_ATTRIBUTE_MODEL = new AttributeModelBuilder(TAGS_FIELD,
                                                                                        PropertyType.STRING_ARRAY,
                                                                                        TAGS_FIELD).setFragment(
        TEST_FRAGMENT).build();

    public static final List<AttributeModel> LIST = Lists.newArrayList(BOOLEAN_ATTRIBUTE_MODEL,
                                                                       INTEGER_ATTRIBUTE_MODEL,
                                                                       DOUBLE_ATTRIBUTE_MODEL,
                                                                       LONG_ATTRIBUTE_MODEL,
                                                                       STRING_ATTRIBUTE_MODEL,
                                                                       STRING_ATTRIBUTE_MODEL_1,
                                                                       LOCAL_DATE_TIME_ATTRIBUTE_MODEL,
                                                                       INTEGER_RANGE_ATTRIBUTE_MODEL,
                                                                       DOUBLE_RANGE_ATTRIBUTE_MODEL,
                                                                       LONG_RANGE_ATTRIBUTE_MODEL,
                                                                       LOCAL_DATE_TIME_RANGE_ATTRIBUTE_MODEL,
                                                                       INTEGER_ARRAY_ATTRIBUTE_MODEL,
                                                                       DOUBLE_ARRAY_ATTRIBUTE_MODEL,
                                                                       LONG_ARRAY_ATTRIBUTE_MODEL,
                                                                       STRING_ARRAY_ATTRIBUTE_MODEL,
                                                                       LOCAL_DATE_TIME_ARRAY_ATTRIBUTE_MODEL,
                                                                       TAGS_ATTRIBUTE_MODEL);

    public static final ResponseEntity<List<EntityModel<AttributeModel>>> ATTRIBUTE_MODEL_CLIENT_RESPONSE = ResponseEntity.ok(
        HateoasUtils.wrapList(SampleDataUtils.LIST));

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

    public static final ResponseEntity<PagedModel<EntityModel<AccessGroup>>> USER_CLIENT_RESPONSE = ResponseEntity.ok(
        HateoasUtils.wrapToPagedResources(Lists.newArrayList(ACCESS_GROUP_0, ACCESS_GROUP_1)));

    public static final ResponseEntity<Boolean> PROJECT_USERS_CLIENT_RESPONSE = ResponseEntity.ok(Boolean.FALSE);

    /**
     * A dummy assembler for collections
     */
    public static final PagedResourcesAssembler<Collection> ASSEMBLER_COLLECTION = Mockito.mock(PagedResourcesAssembler.class);

    /**
     * A dummy assembler for dataobjects
     */
    public static final PagedResourcesAssembler<DataObject> ASSEMBLER_DATAOBJECT = Mockito.mock(PagedResourcesAssembler.class);

    /**
     * A dummy assembler for datasets
     */
    public static final PagedResourcesAssembler<Dataset> ASSEMBLER_DATASET = Mockito.mock(PagedResourcesAssembler.class);

    /**
     * A dummy assembler for entities
     */
    @SuppressWarnings("rawtypes")
    public static final PagedResourcesAssembler<AbstractEntity> ASSEMBLER_ABSTRACT_ENTITIES = Mockito.mock(
        PagedResourcesAssembler.class);

    /**
     * A dummy collection
     */
    public static final Collection COLLECTION = new Collection();

    public static final Model DATAOBJET_MODEL = Model.build("dataObjectModel", "dataObjectModel", EntityType.DATA);

    public static final Model DATASET_MODEL = Model.build("dataSetModel", "dataSetModel", EntityType.DATASET);

    /**
     * A dummy dataobject
     */
    public static final DataObject DATAOBJECT = new DataObject(DATAOBJET_MODEL, "tenant", "DO1", "DO1");

    /**
     * A dummy page of dataobjects
     */
    public static final Page<DataObject> PAGE_DATAOBJECT = new PageImpl<>(Lists.newArrayList(DATAOBJECT));

    /**
     * A dummy page of dataobjects
     */
    public static final FacetPage<DataObject> FACET_PAGE_DATAOBJECT = new FacetPage<>(Lists.newArrayList(DATAOBJECT),
                                                                                      Sets.newHashSet());

    /**
     * A dummy dataset
     */
    public static final Dataset DATASET = new Dataset(DATASET_MODEL, "tenant", "DS1", "DS1");

    /**
     * A dummy page of dataobjects
     */
    public static final Page<Dataset> PAGE_DATASET = new PageImpl<>(Lists.newArrayList(DATASET));

    /**
     * A dummy list of facets
     */
    public static final Map<String, FacetType> FACETS = new ImmutableMap.Builder<String, FacetType>().put("integer",
                                                                                                          FacetType.NUMERIC)
                                                                                                     .put("string",
                                                                                                          FacetType.STRING)
                                                                                                     .build();

    /**
     * A dummy list of facets
     */
    public static final List<String> QUERY_FACETS = Arrays.asList("integer", "string");

    /**
     * A mock pageable
     */
    public static final Pageable PAGEABLE = Mockito.mock(Pageable.class);

    /**
     * A dummy paged resources of dataobjects
     */
    public static final PagedModel<EntityModel<DataObject>> PAGED_RESOURCES_DATAOBJECT = PagedModel.of(new ArrayList<>(),
                                                                                                       (PagedModel.PageMetadata) null,
                                                                                                       Link.of("href"));

    /**
     * A dummy paged resources of dataset
     */
    public static final PagedModel<EntityModel<Dataset>> PAGED_RESOURCES_DATASET = PagedModel.of(new ArrayList<>(),
                                                                                                 (PagedModel.PageMetadata) null,
                                                                                                 Link.of("href"));

    /**
     * A criterion string match
     */
    public static final ICriterion SIMPLE_STRING_MATCH_CRITERION = ICriterion.eq("field",
                                                                                 "value",
                                                                                 StringMatchType.KEYWORD);

    /**
     * Define a criterion with a nested criterion of name "target" (this must be detected and properly handled)
     */
    public static final ICriterion CRITERION_WITH_NESTED_TARGET_FIELD = ICriterion.or(ICriterion.eq("target",
                                                                                                    "DATASET",
                                                                                                    StringMatchType.KEYWORD),
                                                                                      ICriterion.eq("field",
                                                                                                    "value",
                                                                                                    StringMatchType.KEYWORD));

    /**
     * Define a criterion with a nested criterion of name "dataset" (this must be detected and properly handled)
     */
    public static final ICriterion CRITERION_WITH_NESTED_DATASET_FIELD = ICriterion.or(ICriterion.eq("dataset",
                                                                                                     "whatever",
                                                                                                     StringMatchType.KEYWORD),
                                                                                       ICriterion.eq("field",
                                                                                                     "value",
                                                                                                     StringMatchType.KEYWORD));

    /**
     * Define a criterion with a nested criterion of name "datasets" (this must be detected and properly handled)
     */
    public static final ICriterion CRITERION_WITH_NESTED_DATASETS_FIELD = ICriterion.or(ICriterion.eq("datasets",
                                                                                                      "whatever",
                                                                                                      StringMatchType.KEYWORD),
                                                                                        ICriterion.eq("field",
                                                                                                      "value",
                                                                                                      StringMatchType.KEYWORD));

    /**
     * A dummy sort
     */
    public static final Sort SORT = Sort.by("date");

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
     * A sample email representing the current user
     */
    public static final String EMAIL = "user@email.com";
}
