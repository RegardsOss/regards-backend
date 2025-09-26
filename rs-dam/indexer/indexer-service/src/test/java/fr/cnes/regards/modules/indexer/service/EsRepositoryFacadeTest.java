/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author mnguyen0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { EsRepositoryFacade.class })
public class EsRepositoryFacadeTest {

    @MockBean
    private IEsRepository esRepository;

    @MockBean
    private IndexAliasResolver indexAliasResolver;

    @Autowired
    private EsRepositoryFacade facade;

    private MockedStatic<IndexAliasResolver> aliasResolverStatic;

    private static final String TENANT = "project";

    private static final String ALIAS = "project_alias";

    private static final String BUILDING = "project_building";

    @BeforeEach
    public void setup() {
        aliasResolverStatic = Mockito.mockStatic(IndexAliasResolver.class);
        aliasResolverStatic.when(() -> IndexAliasResolver.resolveAliasName(TENANT)).thenReturn(ALIAS);
    }

    @AfterEach
    public void tearDown() {
        if (aliasResolverStatic != null) {
            aliasResolverStatic.close();
        }
    }

    @Test
    public void deleteByQueryOnAliasAndBuildingIndex_withBuildingIndex() {
        // Given
        ICriterion crit = mock(ICriterion.class);
        when(indexAliasResolver.resolveBuildingIndex(TENANT)).thenReturn(Optional.of(BUILDING));
        when(esRepository.deleteByQuery(BUILDING, crit)).thenReturn(5L);
        when(esRepository.deleteByQuery(ALIAS, crit)).thenReturn(7L);

        // When
        long total = facade.deleteByQueryOnAliasAndBuildingIndex(TENANT, crit);

        // Then
        assertThat(total).isEqualTo(12L);
    }

    @Test
    public void deleteByQueryOnAliasAndBuildingIndex_withoutBuildingIndex() {
        // Given
        ICriterion crit = mock(ICriterion.class);
        when(indexAliasResolver.resolveBuildingIndex(TENANT)).thenReturn(Optional.empty());
        when(esRepository.deleteByQuery(ALIAS, crit)).thenReturn(9L);

        // When
        long total = facade.deleteByQueryOnAliasAndBuildingIndex(TENANT, crit);

        // Then
        assertThat(total).isEqualTo(9L);
        verify(esRepository).deleteByQuery(eq(ALIAS), eq(crit));
        verify(esRepository, never()).deleteByQuery(eq(BUILDING), any());
    }

    @Test
    public void forAliasAndBuilding_includesBuildingAndAlias() {
        // Given
        when(indexAliasResolver.resolveBuildingIndex(TENANT)).thenReturn(Optional.of(BUILDING));
        AtomicInteger count = new AtomicInteger();
        Set<String> seen = new HashSet<>();

        // When
        facade.runOnAliasAndBuildingIndex(TENANT, idx -> {
            count.incrementAndGet();
            seen.add(idx);
        });

        // Then
        assertThat(count.get()).isEqualTo(2);
        assertThat(seen).contains(BUILDING, ALIAS);
    }

    @Test
    public void forAliasAndBuilding_aliasOnlyWhenNoBuilding() {

        // Given
        when(indexAliasResolver.resolveBuildingIndex(TENANT)).thenReturn(Optional.empty());
        AtomicInteger step = new AtomicInteger();

        // When
        facade.runOnAliasAndBuildingIndex(TENANT, idx -> {
            step.incrementAndGet();
            assertThat(idx).isEqualTo(ALIAS);
        });

        // Then
        assertThat(step.get()).isEqualTo(1);
    }
}
