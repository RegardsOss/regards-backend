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
package fr.cnes.regards.modules.uiconfiguration.service;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.modules.configuration.dao.ISearchHistoryRepository;
import fr.cnes.regards.modules.configuration.domain.SearchHistory;
import fr.cnes.regards.modules.configuration.domain.SearchHistoryDto;
import fr.cnes.regards.modules.configuration.service.SearchHistoryService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Théo Lasserre
 */
@RegardsTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=searchhistory_it" },
                    locations = { "classpath:application-test.properties" })
public class SearchHistoryServiceIT extends AbstractMultitenantServiceIT {

    @Autowired
    private ISearchHistoryRepository searchHistoryRepository;

    @Autowired
    private SearchHistoryService searchHistoryService;

    @Before
    public void init() {
        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setModuleId(0L);
        searchHistory.setAccountEmail("test@test.fr");
        searchHistory.setName("searchHistoryTest0");
        searchHistory.setConfiguration("configTest0");
        searchHistoryRepository.save(searchHistory);

        searchHistory = new SearchHistory();
        searchHistory.setModuleId(0L);
        searchHistory.setAccountEmail("test@test.fr");
        searchHistory.setName("searchHistoryTest1");
        searchHistory.setConfiguration("configTest1");
        searchHistoryRepository.save(searchHistory);

        searchHistory = new SearchHistory();
        searchHistory.setModuleId(0L);
        searchHistory.setAccountEmail("test@test.fr");
        searchHistory.setName("searchHistoryTest2");
        searchHistory.setConfiguration("configTest2");
        searchHistoryRepository.save(searchHistory);
    }

    @Test
    public void test_retrieve_search_history_page() {
        // When
        Page<SearchHistoryDto> SearchHistoryDtoPage = searchHistoryService.retrieveSearchHistory("test@test.fr",
                                                                                                 0L,
                                                                                                 PageRequest.of(0, 10));
        // Then
        Assert.assertEquals(3, SearchHistoryDtoPage.getContent().size());
    }

    @Test
    public void test_retrieve_search_history_page_by_module_id() {
        // When
        Page<SearchHistoryDto> searchHistoryPage = searchHistoryService.retrieveModuleSearchHistory(0L,
                                                                                                    PageRequest.of(0,
                                                                                                                   10));
        // Then
        Assert.assertEquals(3, searchHistoryPage.getContent().size());
    }

    @Test
    public void test_retrieve_search_history_page_by_account_id() {
        // When
        Page<SearchHistoryDto> searchHistoryPage = searchHistoryService.retrieveAccountSearchHistory("test@test.fr",
                                                                                                     PageRequest.of(0,
                                                                                                                    10));
        // Then
        Assert.assertEquals(3, searchHistoryPage.getContent().size());
    }

    @Test
    public void test_delete_module_search_history() {
        // When
        searchHistoryService.deleteModuleSearchHistory(0L);

        // Then
        Pageable pageable = PageRequest.of(0, 100);
        Page<SearchHistoryDto> SearchHistoryDtoPage = searchHistoryService.retrieveModuleSearchHistory(0L, pageable);
        Assert.assertEquals(0, SearchHistoryDtoPage.getContent().size());
    }

    @Test
    public void test_delete_account_search_history() {
        // When
        searchHistoryService.deleteAccountSearchHistory("test@test.fr");

        // Then
        Pageable pageable = PageRequest.of(0, 100);
        Page<SearchHistoryDto> SearchHistoryDtoPage = searchHistoryService.retrieveAccountSearchHistory("test@test.fr",
                                                                                                        pageable);
        Assert.assertEquals(0, SearchHistoryDtoPage.getContent().size());
    }

    @Test
    public void test_add_search_history_element() throws EntityException {
        // Given
        SearchHistoryDto SearchHistoryDto = new SearchHistoryDto("testCreate1", "testConfig");
        String accountEmail = "testets@tests?fr";
        Long moduleId = 1L;

        // When
        searchHistoryService.addSearchHistory(SearchHistoryDto, accountEmail, moduleId);
        Page<SearchHistoryDto> SearchHistoryDtoPage = searchHistoryService.retrieveSearchHistory(accountEmail,
                                                                                                 moduleId,
                                                                                                 PageRequest.of(0, 10));

        // Then
        Assert.assertEquals(1, SearchHistoryDtoPage.getContent().size());
    }

    @Test
    public void test_delete_search_history() throws EntityException {
        // Given
        SearchHistoryDto SearchHistoryDto = new SearchHistoryDto("testCreate2", "testConfig");
        String accountEmail = "lalaa@tests?fr";
        Long moduleId = 2L;

        // When
        SearchHistoryDto searchHistory = searchHistoryService.addSearchHistory(SearchHistoryDto,
                                                                               accountEmail,
                                                                               moduleId);
        Page<SearchHistoryDto> SearchHistoryDtoPage = searchHistoryService.retrieveSearchHistory(accountEmail,
                                                                                                 moduleId,
                                                                                                 PageRequest.of(0, 10));
        Assert.assertEquals(1, SearchHistoryDtoPage.getContent().size());
        searchHistoryService.deleteSearchHistory(searchHistory.getId());
        SearchHistoryDtoPage = searchHistoryService.retrieveSearchHistory(accountEmail,
                                                                          moduleId,
                                                                          PageRequest.of(0, 10));

        // Then
        Assert.assertEquals(0, SearchHistoryDtoPage.getContent().size());
    }
}
