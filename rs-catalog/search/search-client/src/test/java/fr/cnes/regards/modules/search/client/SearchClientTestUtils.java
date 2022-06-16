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
package fr.cnes.regards.modules.search.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

/**
 * @author Xavier-Alexandre Brochard
 */
public class SearchClientTestUtils {

    public static final String OTHER_USER_EMAIL = "other.user@regards.fr";

    public static final String ADMIN_USER_EMAIL = "admin.user@regards.fr";

    /**
     * The name of an attribute of type date
     */
    public static final String DATE_ATTRIBUTE_NAME = "date";

    /**
     * The name of an attribute of type string
     */
    public static final String STRING_ATTRIBUTE_NAME = "label";

    /**
     * The name of an attribute of type integer
     */
    public static final String INTEGER_ATTRIBUTE_NAME = "integer";

    /**
     * The name of an attribute which does not exist
     */
    public static final String UNEXISTNG_ATTRIBUTE_NAME = "unexisting";

    /**
     * The name of an attribute of type integer
     */
    public static final String EXTRA_ATTRIBUTE_NAME = "extra";

    public static final Fragment TEST_FRAGMENT = Fragment.buildDefault();

    public static final AttributeModel INTEGER_ATTRIBUTE_MODEL = AttributeModelBuilder.build(INTEGER_ATTRIBUTE_NAME,
                                                                                             PropertyType.INTEGER,
                                                                                             INTEGER_ATTRIBUTE_NAME)
                                                                                      .fragment(TEST_FRAGMENT)
                                                                                      .get();

    public static final AttributeModel STRING_ATTRIBUTE_MODEL = AttributeModelBuilder.build(STRING_ATTRIBUTE_NAME,
                                                                                            PropertyType.STRING,
                                                                                            STRING_ATTRIBUTE_NAME)
                                                                                     .get();

    public static final AttributeModel DATE_ATTRIBUTE_MODEL = AttributeModelBuilder.build(DATE_ATTRIBUTE_NAME,
                                                                                          PropertyType.DATE_ISO8601,
                                                                                          DATE_ATTRIBUTE_NAME)
                                                                                   .fragment(TEST_FRAGMENT)
                                                                                   .get();

    public static final AttributeModel EXTRA_ATTRIBUTE_MODEL = AttributeModelBuilder.build(EXTRA_ATTRIBUTE_NAME,
                                                                                           PropertyType.STRING,
                                                                                           EXTRA_ATTRIBUTE_NAME)
                                                                                    .fragment(TEST_FRAGMENT)
                                                                                    .get();

    public static final List<AttributeModel> LIST = Lists.newArrayList(INTEGER_ATTRIBUTE_MODEL,
                                                                       DATE_ATTRIBUTE_MODEL,
                                                                       EXTRA_ATTRIBUTE_MODEL);

    public static final ResponseEntity<List<EntityModel<AttributeModel>>> ATTRIBUTE_MODEL_CLIENT_RESPONSE = ResponseEntity.ok(
        HateoasUtils.wrapList(LIST));

    /**
     * A dummy list of facets
     */
    public static final List<String> FACETS = Lists.newArrayList(INTEGER_ATTRIBUTE_MODEL.getJsonPath(),
                                                                 STRING_ATTRIBUTE_NAME,
                                                                 DATE_ATTRIBUTE_MODEL.getJsonPath(),
                                                                 EXTRA_ATTRIBUTE_MODEL.getJsonPath());

    /**
     * The dummy list of facets as array
     */
    public static final String[] FACETS_AS_ARRAY = FACETS.toArray(new String[FACETS.size()]);

    /**
     * A dummy access group name
     */
    public static final String ACCESS_GROUP_NAME_0 = "accessGroup0";

    /**
     * A dummy access group name
     */
    public static final String ACCESS_GROUP_NAME_1 = "accessGroup1";

    public static final String ACCESS_GROUP_NAME_2 = "accessGroup2";

    /**
     * The previous access group name as a {@link Set}
     */
    public static final Set<String> ACCESS_GROUP_NAMES_AS_SET = Sets.newHashSet(ACCESS_GROUP_NAME_0,
                                                                                ACCESS_GROUP_NAME_1);

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

    public static final AccessGroup ACCESS_GROUP_2 = new AccessGroup(ACCESS_GROUP_NAME_2);

    public static final ResponseEntity<PagedModel<EntityModel<AccessGroup>>> USER_CLIENT_OTHER_RESPONSE = ResponseEntity.ok(
        HateoasUtils.wrapToPagedResources(Lists.newArrayList(ACCESS_GROUP_2)));

    public static final ResponseEntity<PagedModel<EntityModel<AccessGroup>>> PUBLIC_USER_CLIENT_RESPONSE = ResponseEntity.ok(
        HateoasUtils.wrapToPagedResources(Lists.newArrayList()));

    public static final ResponseEntity<PagedModel<EntityModel<AccessGroup>>> USER_CLIENT_EMPTY_RESPONSE = ResponseEntity.ok(
        HateoasUtils.wrapToPagedResources(Lists.newArrayList()));

    /**
     * Sample response from the {@link IProjectUsersClient} isAdmin: false
     */
    public static final ResponseEntity<Boolean> PROJECT_USERS_CLIENT_RESPONSE = ResponseEntity.ok(Boolean.FALSE);

    /**
     * Sample response from the {@link IProjectUsersClient} isAdmin: true
     */
    public static final ResponseEntity<Boolean> PROJECT_USERS_CLIENT_RESPONSE_ADMIN = ResponseEntity.ok(Boolean.TRUE);
}
