/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.rest;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.configuration.dao.ISearchHistoryRepository;
import fr.cnes.regards.modules.configuration.domain.SearchHistory;
import fr.cnes.regards.modules.configuration.domain.SearchHistoryDto;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Class InstanceLayoutControllerIT
 * <p>
 * IT Tests for REST Controller
 *
 * @author Théo Lasserre
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class SearchHistoryControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SearchHistoryControllerIT.class);

    @Autowired
    private ISearchHistoryRepository searchHistoryRepository;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

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
    public void test_retrieve_search_history() {
        performDefaultGet(SearchHistoryController.SEARCH_HISTORY_PATH,
                          customizer().expectStatusOk()
                                      .expectToHaveSize(JSON_PATH_CONTENT, 3)
                                      .addParameter("accountEmail", "test@test.fr")
                                      .addParameter("moduleId", "0"),
                          "There should be the 3 initial search history elements for the given module & user");
    }

    @Test
    public void test_create_search_history_element() {
        SearchHistoryDto searchHistoryDto = new SearchHistoryDto("testSearchHistoryElement", "testConfiguration");
        String accountEmail = "test@test.test";
        String moduleId = "1";
        performDefaultPost(SearchHistoryController.SEARCH_HISTORY_PATH,
                           searchHistoryDto,
                           customizer().expectStatusOk()
                                       .addParameter("accountEmail", accountEmail)
                                       .addParameter("moduleId", moduleId),
                           "New search history element should be correctly saved");
        performDefaultGet(SearchHistoryController.SEARCH_HISTORY_PATH,
                          customizer().expectStatusOk()
                                      .expectToHaveSize(JSON_PATH_CONTENT, 1)
                                      .addParameter("accountEmail", accountEmail)
                                      .addParameter("moduleId", moduleId),
                          "New search history element should be correctly retrieved");
    }

    @Test
    public void test_delete_search_history_element() {
        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setModuleId(0L);
        searchHistory.setAccountEmail("test@test.fr");
        searchHistory.setName("searchHistoryTest3");
        searchHistory.setConfiguration("configTest3");
        searchHistory = searchHistoryRepository.save(searchHistory);

        performDefaultGet(SearchHistoryController.SEARCH_HISTORY_PATH,
                          customizer().expectStatusOk()
                                      .expectToHaveSize(JSON_PATH_CONTENT, 4)
                                      .addParameter("accountEmail", "test@test.fr")
                                      .addParameter("moduleId", "0"),
                          "Search history element should be correctly retrieved");
        performDefaultDelete(SearchHistoryController.SEARCH_HISTORY_PATH + "/{searchHistoryId}",
                             customizer().expectStatusOk(),
                             "Search history element should be correctly deleted",
                             searchHistory.getId());
        performDefaultGet(SearchHistoryController.SEARCH_HISTORY_PATH,
                          customizer().expectStatusOk()
                                      .expectToHaveSize(JSON_PATH_CONTENT, 3)
                                      .addParameter("accountEmail", "test@test.fr")
                                      .addParameter("moduleId", "0"),
                          "Search history element should be correctly retrieved");
    }
}
