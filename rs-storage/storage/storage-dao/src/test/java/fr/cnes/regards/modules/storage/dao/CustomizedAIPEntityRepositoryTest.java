/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.dao;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;

/**
 * SQL syntax tester
 *
 * @author Marc SORDI
 *
 */
@Ignore
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=nativeq" })
public class CustomizedAIPEntityRepositoryTest extends AbstractMultitenantServiceTest {

    @Autowired
    private ICustomizedAIPEntityRepository customRepo;

    @Test
    public void findAllTest() {
        customRepo
                .findAll(AIPQueryGenerator.searchAIPContainingAllTags(null, null, null, null, null, "PID", null, null,
                                                                      null),
                         PageRequest.of(0, 1));
    }

}
