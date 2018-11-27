/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.json.test;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.json.test.domain.ITestEntityRepository;
import fr.cnes.regards.framework.jpa.json.test.domain.JsonbEntity;
import fr.cnes.regards.framework.jpa.json.test.domain.TestEntity;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * WARNING : this test needs default "public" schema on the target database
 * @author Sylvain Vissiere-Guerinet
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JsonbTestConfiguration.class })
public class JsonbTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonbTest.class);

    /**
     * bean provided by spring boot starter data jpa
     */
    @Autowired
    private ITestEntityRepository testEntityRepository;

    @Requirement("REGARDS_DSL_DAM_MOD_060")
    @Purpose("Test ability to persist and retrieve structureless data stored as jsonb into postgres")
    @Test
    public void testPersist() {
        TestEntity te = new TestEntity(new JsonbEntity("name", "content"));
        TestEntity fromSave = testEntityRepository.save(te);
        Optional<TestEntity> fromDBOpt = testEntityRepository.findById(fromSave.getId());
        Assert.assertTrue(fromDBOpt.isPresent());
        Assert.assertEquals(te, fromSave);
        Assert.assertEquals(te, fromDBOpt.get());
    }

}
