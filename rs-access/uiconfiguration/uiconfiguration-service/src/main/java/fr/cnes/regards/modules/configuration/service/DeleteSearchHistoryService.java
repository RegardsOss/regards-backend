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
import fr.cnes.regards.modules.configuration.dao.ISearchHistoryRepository;
import fr.cnes.regards.modules.configuration.domain.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Implementation of ISearchHistory
 *
 * @author Théo Lasserre
 */
@Service
public class DeleteSearchHistoryService {

    /**
     * Repository class for search history
     */
    private final ISearchHistoryRepository searchHistoryRepository;

    /**
     * Constructor
     */
    public DeleteSearchHistoryService(ISearchHistoryRepository searchHistoryRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
    }

    /**
     * Delete an account search history page
     *
     * @param accountEmail account email
     * @param pageable     pageable
     * @return an account search history page
     */
    @RegardsTransactional
    public Page<SearchHistory> deleteAccountSearchHistoryPage(String accountEmail, Pageable pageable) {
        Page<SearchHistory> SearchHistoryDtoPage = searchHistoryRepository.findByAccountEmail(accountEmail, pageable);
        searchHistoryRepository.deleteByIdIn(SearchHistoryDtoPage.stream()
                                                                 .map(SearchHistory::getId)
                                                                 .collect(Collectors.toList()));
        return SearchHistoryDtoPage;
    }

    /**
     * Delete a module search history page
     *
     * @param moduleId module id
     * @param pageable pageable
     * @return a module search history page
     */
    @RegardsTransactional
    public Page<SearchHistory> deleteModuleSearchHistoryPage(Long moduleId, Pageable pageable) {
        Page<SearchHistory> SearchHistoryDtoPage = searchHistoryRepository.findByModuleId(moduleId, pageable);
        Collection<Long> idsToDelete = SearchHistoryDtoPage.stream()
                                                           .map(SearchHistory::getId)
                                                           .collect(Collectors.toList());
        searchHistoryRepository.deleteByIdIn(idsToDelete);
        return SearchHistoryDtoPage;
    }
}
