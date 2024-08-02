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
package fr.cnes.regards.modules.configuration.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.modules.configuration.dao.ISearchHistoryRepository;
import fr.cnes.regards.modules.configuration.domain.SearchHistory;
import fr.cnes.regards.modules.configuration.domain.SearchHistoryDto;
import fr.cnes.regards.modules.configuration.domain.SearchHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of ISearchHistory
 *
 * @author Théo Lasserre
 */
@Service
public class SearchHistoryService {

    /**
     * Repository class for search history
     */
    @Autowired
    private final ISearchHistoryRepository searchHistoryRepository;

    /**
     * Mapper class for search history
     */
    @Autowired
    private final SearchHistoryMapper searchHistoryMapper;

    /**
     * DeleteSearchHistoryService
     */
    private final DeleteSearchHistoryService deleteSearchHistoryService;

    /**
     * Constructor
     */
    public SearchHistoryService(ISearchHistoryRepository searchHistoryRepository,
                                SearchHistoryMapper searchHistoryMapper,
                                DeleteSearchHistoryService deleteSearchHistoryService) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.searchHistoryMapper = searchHistoryMapper;
        this.deleteSearchHistoryService = deleteSearchHistoryService;
    }

    /**
     * Retrieve a search history using an account email and a module id
     *
     * @param accountEmail account email
     * @param moduleId     module id
     * @param pageable     pageable
     * @return a search history page
     */
    @RegardsTransactional(readOnly = true)
    public Page<SearchHistoryDto> retrieveSearchHistory(String accountEmail, Long moduleId, Pageable pageable) {
        return searchHistoryRepository.findByAccountEmailAndModuleId(accountEmail, moduleId, pageable)
                                      .map(searchHistoryMapper::convertToSearchHistoryDto);
    }

    /**
     * Retrieve an account search history using account email
     *
     * @param accountEmail account email
     * @param pageable     pageable
     * @return an account search history page
     */
    @RegardsTransactional(readOnly = true)
    public Page<SearchHistoryDto> retrieveAccountSearchHistory(String accountEmail, Pageable pageable) {
        return searchHistoryRepository.findByAccountEmail(accountEmail, pageable)
                                      .map(searchHistoryMapper::convertToSearchHistoryDto);
    }

    /**
     * Retrieve a module search history using module id
     *
     * @param moduleId module id
     * @param pageable pageable
     * @return a module search history page
     */
    @RegardsTransactional(readOnly = true)
    public Page<SearchHistoryDto> retrieveModuleSearchHistory(Long moduleId, Pageable pageable) {
        return searchHistoryRepository.findByModuleId(moduleId, pageable)
                                      .map(searchHistoryMapper::convertToSearchHistoryDto);
    }

    /**
     * Create a new search history element
     *
     * @param searchHistoryDto search history dto
     * @param accountEmail     account email
     * @param moduleId         module id
     * @return the saved search history element
     */
    @RegardsTransactional
    public SearchHistoryDto addSearchHistory(SearchHistoryDto searchHistoryDto, String accountEmail, Long moduleId)
        throws EntityException {
        if (searchHistoryDto == null || accountEmail == null || moduleId == null) {
            throw new EntityInvalidException("Can't create SearchHistory element. SearchHistoryDto, accountId and "
                                             + "moduleId must be set");
        }
        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setConfiguration(searchHistoryDto.getConfiguration());
        searchHistory.setAccountEmail(accountEmail);
        searchHistory.setName(searchHistoryDto.getName());
        searchHistory.setModuleId(moduleId);
        searchHistory = searchHistoryRepository.save(searchHistory);
        SearchHistoryDto createdSearchHistoryDto = searchHistoryMapper.convertToSearchHistoryDto(searchHistory);
        return createdSearchHistoryDto;
    }

    @RegardsTransactional
    public SearchHistoryDto updateSearchHistory(Long id, String searchHistoryConf) throws EntityException {
        if (id == null) {
            throw new EntityInvalidException("SearchHistory element id cannot be null");
        }
        Optional<SearchHistory> searchHistoryOpt = searchHistoryRepository.findById(id);
        if (searchHistoryOpt.isPresent()) {
            SearchHistory searchHistory = searchHistoryOpt.get();
            searchHistory.setConfiguration(searchHistoryConf);
            searchHistory = searchHistoryRepository.save(searchHistory);
            return searchHistoryMapper.convertToSearchHistoryDto(searchHistory);
        } else {
            throw new EntityInvalidException("SearchHistory element not found");
        }
    }

    /**
     * Delete a search history element using its id
     *
     * @param id id of search history element
     */
    @RegardsTransactional
    public void deleteSearchHistory(Long id) throws EntityException {
        if (id == null) {
            throw new EntityInvalidException("SearchHistory element id cannot be null");
        }
        searchHistoryRepository.deleteById(id);
    }

    /**
     * Delete an account search history using its id.
     *
     * @param accountEmail account email
     */
    public void deleteAccountSearchHistory(String accountEmail) {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SearchHistory> searchHistoryPage;
        do {
            searchHistoryPage = deleteSearchHistoryService.deleteAccountSearchHistoryPage(accountEmail, pageable);
        } while (searchHistoryPage.hasNext());
    }

    /**
     * Delete a module search history using its id
     *
     * @param moduleId module id
     */
    public void deleteModuleSearchHistory(Long moduleId) {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SearchHistory> searchHistoryPage;
        do {
            searchHistoryPage = deleteSearchHistoryService.deleteModuleSearchHistoryPage(moduleId, pageable);
        } while (searchHistoryPage.hasNext());
    }
}
