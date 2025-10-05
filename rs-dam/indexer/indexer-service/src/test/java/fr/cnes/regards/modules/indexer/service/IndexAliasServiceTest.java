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

import fr.cnes.regards.modules.indexer.dao.IEsIndexAliasRepository;
import fr.cnes.regards.modules.indexer.domain.EsIndexAlias;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@Import({ IndexAliasServiceTest.Cfg.class, IndexAliasService.class })
public class IndexAliasServiceTest {

    @TestConfiguration
    @EnableCaching
    static class Cfg {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("esIndexAliases");
        }
    }

    @Autowired
    private IndexAliasService indexAliasService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private IEsIndexAliasRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        var cache = cacheManager.getCache("esIndexAliases");
        if (cache != null) {
            cache.clear();
        }
        clearInvocations(repository);
    }

    @Test
    public void getByAliasIsCachedBetweenCalls() {
        // given
        var alias = new EsIndexAlias("alias_name", "idx_name");
        when(repository.findByAlias("alias_name")).thenReturn(Optional.of(alias));

        // when
        var first = indexAliasService.getByAlias("alias_name");
        var second = indexAliasService.getByAlias("alias_name");

        // then
        assertThat(first).isSameAs(alias);
        assertThat(second).isSameAs(alias);
        verify(repository, times(1)).findByAlias("alias_name");
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void updateAliasWithSameIndexWithCache() {
        // given
        var existing = new EsIndexAlias("alias_name", "idx_name");
        when(repository.findByAlias("alias_name")).thenReturn(Optional.of(existing));

        // when
        var res = indexAliasService.saveOrUpdate("alias_name", "idx_name");

        // then
        assertThat(res).isSameAs(existing);
        verify(repository, times(1)).findByAlias("alias_name");
        verify(repository, never()).save(ArgumentMatchers.any());
        verifyNoMoreInteractions(repository);
        var cached = indexAliasService.getByAlias("alias_name");
        assertThat(cached).isSameAs(existing);
        verify(repository, times(1)).findByAlias("alias_name"); // toujours 1 seule fois
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void updateAliasWithDifferentIndexWithCache() {
        // given
        var existing = new EsIndexAlias("alias_name", "idx_old");

        when(repository.findByAlias("alias_U")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenAnswer(inv -> {
            existing.setCurrent("idx_new");
            return existing;
        });

        // when
        var result = indexAliasService.saveOrUpdate("alias_name", "idx_new");

        // then
        assertThat(result.getCurrent()).isEqualTo("idx_new");
        verify(repository, times(1)).findByAlias("alias_name");
        verify(repository, times(1)).save(existing);

        clearInvocations(repository);
        var cached = indexAliasService.getByAlias("alias_name");
        assertThat(cached.getCurrent()).isEqualTo("idx_new");
        verify(repository, never()).findByAlias(anyString());
    }

}
