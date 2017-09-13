/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.entities.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.dao.domain.TestEntity;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.models.dao.IModelRepository;
import fr.cnes.regards.modules.models.domain.Model;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource("classpath:application-test.properties")
@Ignore
public class AbstractEntityDaoTest extends AbstractDaoTransactionalTest {

    private static final String TAG_TO_SEARCH = "tag";

    private static final String TAG_TEST = "TEST";

    private static final String TAG_LAST = "LAST";

    @Autowired
    private IAbstractEntityRepository<AbstractEntity> entityRepository;

    @Autowired
    private IModelRepository modelRepository;

    @Test
    public void testfindByTagsValue() {

        final Model model = Model.build("name", "desc", EntityType.COLLECTION);
        modelRepository.save(model);

        TestEntity entity1 = new TestEntity(model, getUrn(), "entity1");
        final Set<String> entity1Tags = new HashSet<>();
        entity1Tags.add(TAG_TO_SEARCH);
        entity1Tags.add(TAG_TEST);
        entity1Tags.add(TAG_LAST);
        entity1.setTags(entity1Tags);
        entity1 = entityRepository.save(entity1);

        TestEntity entity2 = new TestEntity(model, getUrn(), "entity2");
        final Set<String> entity2Tags = new HashSet<>();
        entity2Tags.add(TAG_TEST);
        entity2Tags.add(TAG_LAST);
        entity2Tags.add(TAG_TO_SEARCH);
        entity2.setTags(entity2Tags);
        entity2 = entityRepository.save(entity2);

        TestEntity entity3 = new TestEntity(model, getUrn(), "entity3");
        final Set<String> entity3Tags = new HashSet<>();
        entity3Tags.add(TAG_TEST);
        entity3Tags.add(TAG_TO_SEARCH);
        entity3Tags.add(TAG_LAST);
        entity3.setTags(entity3Tags);
        entity3 = entityRepository.save(entity3);

        TestEntity entity4 = new TestEntity(model, getUrn(), "entity4");
        final Set<String> entity4Tags = new HashSet<>();
        entity4Tags.add(TAG_TEST);
        entity4Tags.add(TAG_LAST);
        entity4.setTags(entity4Tags);
        entity4 = entityRepository.save(entity4);

        final List<AbstractEntity> result = entityRepository.findByTags(TAG_TO_SEARCH);
        Assert.assertTrue(result.contains(entity1));
        Assert.assertTrue(result.contains(entity2));
        Assert.assertTrue(result.contains(entity3));
        Assert.assertFalse(result.contains(entity4));

    }

    @Test
    public void testFindByIpIdIn() {
        final Model model = Model.build("name", "desc", EntityType.COLLECTION);
        modelRepository.save(model);

        TestEntity entity1 = new TestEntity(model, getUrn(), "entity1");
        entity1 = entityRepository.save(entity1);

        TestEntity entity2 = new TestEntity(model, getUrn(), "entity2");
        entity2 = entityRepository.save(entity2);

        TestEntity entity3 = new TestEntity(model, getUrn(), "entity3");
        entity3 = entityRepository.save(entity3);

        TestEntity entity4 = new TestEntity(model, getUrn(), "entity4");
        entity4 = entityRepository.save(entity4);

        final Set<UniformResourceName> ipIds = new HashSet<>();
        ipIds.add(entity1.getIpId());
        ipIds.add(entity3.getIpId());
        ipIds.add(entity2.getIpId());

        final List<AbstractEntity> result = entityRepository.findByIpIdIn(ipIds);
        Assert.assertTrue(result.contains(entity1));
        Assert.assertTrue(result.contains(entity2));
        Assert.assertTrue(result.contains(entity3));
        Assert.assertFalse(result.contains(entity4));
    }

    @Test
    public void testFindByIpIdInEmptySet() {
        final Model model = Model.build("name", "desc", EntityType.COLLECTION);
        modelRepository.save(model);

        TestEntity entity1 = new TestEntity(model, getUrn(), "entity1");
        entity1 = entityRepository.save(entity1);

        TestEntity entity2 = new TestEntity(model, getUrn(), "entity2");
        entity2 = entityRepository.save(entity2);

        TestEntity entity3 = new TestEntity(model, getUrn(), "entity3");
        entity3 = entityRepository.save(entity3);

        TestEntity entity4 = new TestEntity(model, getUrn(), "entity4");
        entity4 = entityRepository.save(entity4);

        final Set<UniformResourceName> ipIds = new HashSet<>();

        final List<AbstractEntity> result = entityRepository.findByIpIdIn(ipIds);
        Assert.assertEquals(0, result.size());
    }

    private UniformResourceName getUrn() {
        return new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, "PROJECT", UUID.randomUUID(), 1);
    }

}
