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
package fr.cnes.regards.modules.order.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.domain.summary.FilesSummary;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineClient;
import fr.cnes.regards.modules.search.domain.ComplexSearchRequest;
import fr.cnes.regards.modules.search.domain.SearchRequest;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;

/**
 * @author oroussel
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class BasketService implements IBasketService {

    @Autowired
    private IBasketRepository repos;

    @Autowired
    private IComplexSearchClient complexSearchClient;

    @Autowired
    private ILegacySearchEngineClient searchClient;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Override
    public Basket findOrCreate(String user) {
        Basket basket = repos.findByOwner(user);
        return (basket == null) ? repos.save(new Basket(user)) : basket;
    }

    @Override
    public Basket find(String user) throws EmptyBasketException {
        Basket basket = repos.findByOwner(user);
        if (basket == null) {
            throw new EmptyBasketException();
        }
        return basket;
    }

    @Override
    public void deleteIfExists(String user) {
        Basket basket = repos.findByOwner(user);
        if (basket != null) {
            repos.delete(basket.getId());
        }
    }

    @Override
    public Basket load(Long id) {
        return repos.findOneById(id);
    }

    @Override
    public Basket addSelection(Long basketId, BasketSelectionRequest selectionRequest) throws EmptySelectionException {
        Basket basket = repos.findOneById(basketId);
        if (basket == null) {
            throw new EntityNotFoundException("Basket with id " + basketId + " doesn't exist");
        }

        try {
            FeignSecurityManager.asUser(authResolver.getUser(), authResolver.getRole());
            // Retrieve summary for all datasets matching the search request from the new selection to add.
            DocFilesSummary summary = complexSearchClient
                    .computeDatasetsSummary(buildSearchRequest(selectionRequest, 0, 1)).getBody();
            // If global summary contains no files => EmptySelection
            if (summary.getFilesCount() == 0l) {
                throw new EmptySelectionException();
            }
            // Create a map to find more easiely a basket dataset selection from dataset IpId
            Map<String, BasketDatasetSelection> basketDsSelectionMap = basket.getDatasetSelections().stream()
                    .collect(Collectors.toMap(BasketDatasetSelection::getDatasetIpid, Function.identity()));
            // Parsing results
            for (Map.Entry<String, DocFilesSubSummary> entry : summary.getSubSummariesMap().entrySet()) {
                // For each dataset of the returned sumary, update the already existing BasketDatasetSelection of the user basket.
                // Or, if no one exists yet, create a new one.
                String datasetIpId = entry.getKey();
                DocFilesSubSummary subSummary = entry.getValue();
                if (subSummary.getFilesCount() == 0l) {
                    // No results for the current dataset.
                    continue;
                }
                // Try to retrieve current dataset selection
                BasketDatasetSelection datasetSelection = basketDsSelectionMap.get(datasetIpId);
                // Manage basket dataset selection
                if (datasetSelection == null) {
                    // There is no existing BasketDatasetSelection for the current datasetIpId so create a new one
                    // Retrieve global information about the new dataset
                    EntityFeature feature = searchClient.getDataset(UniformResourceName.fromString(datasetIpId),
                                                                    SearchEngineMappings.getJsonHeaders())
                            .getBody().getContent();
                    datasetSelection = new BasketDatasetSelection();
                    datasetSelection.setDatasetIpid(datasetIpId);
                    datasetSelection.setDatasetLabel(feature.getLabel());
                    // Add the newly created dataset to the current basket.
                    basket.getDatasetSelections().add(datasetSelection);
                }
                // Create item selection for each dataset
                BasketDatedItemsSelection itemsSelection = createItemsSelection(selectionRequest, subSummary);

                // Add items selection to dataset selection
                datasetSelection.getItemsSelections().add(itemsSelection);

                // Update DatasetSelection (summary)
                computeSummaryAndUpdateDatasetSelection(datasetSelection);
            }
        } finally {
            FeignSecurityManager.reset();
        }
        return repos.save(basket);
    }

    @Override
    public Basket removeDatasetSelection(Basket basket, Long datasetId) {
        for (Iterator<BasketDatasetSelection> i = basket.getDatasetSelections().iterator(); i.hasNext();) {
            if (i.next().getId().equals(datasetId)) {
                i.remove();
                repos.save(basket);
                break;
            }
        }
        return basket;
    }

    @Override
    public Basket removeDatedItemsSelection(Basket basket, Long datasetId, OffsetDateTime itemsSelectionDate) {
        for (Iterator<BasketDatasetSelection> j = basket.getDatasetSelections().iterator(); j.hasNext();) {
            BasketDatasetSelection dsSelection = j.next();
            if (dsSelection.getId().equals(datasetId)) {
                // Search for item selections to remove
                for (Iterator<BasketDatedItemsSelection> i = dsSelection.getItemsSelections().iterator(); i
                        .hasNext();) {
                    if (i.next().getSelectionRequest().getSelectionDate().equals(itemsSelectionDate)) {
                        i.remove();
                        break;
                    }
                }
                // Must recompute dataset opensearch request from its associated dated items selections
                if (dsSelection.getItemsSelections().isEmpty()) {
                    // must delete dsSelection (no more dated items selections => no more datasetSelection)
                    j.remove();
                }
                computeSummaryAndUpdateDatasetSelection(dsSelection);
                repos.save(basket);
                break;
            }
        }
        return basket;
    }

    /**
     * Create dated items selection
     * @param openSearchRequest opensearch request from which this selection is created
     * @param now date of selection
     * @param subSummary dataset  selection (sub-)summary
     * @return dated items selection (what else ?)
     */
    private BasketDatedItemsSelection createItemsSelection(BasketSelectionRequest selectionRequest,
            DocFilesSubSummary subSummary) {
        // Create a new basket dated items selection
        BasketDatedItemsSelection itemsSelection = new BasketDatedItemsSelection();
        itemsSelection.setSelectionRequest(selectionRequest);
        itemsSelection.setObjectsCount((int) subSummary.getDocumentsCount());
        int filesCount = 0;
        long filesSize = 0;
        for (DataType fileType : DataTypeSelection.ALL.getFileTypes()) {
            FilesSummary filesSummary = subSummary.getFileTypesSummaryMap().get(fileType.toString());
            filesCount += filesSummary.getFilesCount();
            filesSize += filesSummary.getFilesSize();
        }
        itemsSelection.setFilesCount(filesCount);
        itemsSelection.setFilesSize(filesSize);
        itemsSelection.setDate(selectionRequest.getSelectionDate());
        return itemsSelection;
    }

    /**
     * (Re-)compute summary on a dataset selection.
     * (Re-) run search on given dataset with all combined items requests.
     */
    private void computeSummaryAndUpdateDatasetSelection(BasketDatasetSelection datasetSelection) {

        DocFilesSummary curDsSelectionSummary = complexSearchClient
                .computeDatasetsSummary(buildSearchRequest(datasetSelection, 0, 1)).getBody();
        // Take into account only asked datasetIpId (sub-)summary
        DocFilesSubSummary curDsSelectionSubSummary = curDsSelectionSummary.getSubSummariesMap()
                .get(datasetSelection.getDatasetIpid());
        // Occurs only in tests
        if (curDsSelectionSubSummary == null) {
            datasetSelection.setObjectsCount(0);
            datasetSelection.setFilesCount(0);
            datasetSelection.setFilesSize(0);
        } else {
            datasetSelection.setObjectsCount((int) curDsSelectionSubSummary.getDocumentsCount());
            datasetSelection.setFilesCount((int) curDsSelectionSubSummary.getFilesCount());
            datasetSelection.setFilesSize(curDsSelectionSubSummary.getFilesSize());
        }
    }

    private static SearchRequest buildSearchRequest(String engineType, String datasetUrn,
            MultiValueMap<String, String> searchParameters, Collection<String> entityIds,
            OffsetDateTime searchDateLimit) {
        SearchRequest request;
        if ((entityIds != null) && !entityIds.isEmpty()) {
            if ((searchParameters != null) && !searchParameters.isEmpty()) {
                request = new SearchRequest(engineType, datasetUrn, searchParameters, entityIds, null, searchDateLimit);
            } else {
                request = new SearchRequest(engineType, datasetUrn, searchParameters, null, entityIds, searchDateLimit);
            }
        } else {
            request = new SearchRequest(engineType, datasetUrn, searchParameters, null, null, searchDateLimit);
        }

        return request;

    }

    public static ComplexSearchRequest buildSearchRequest(BasketSelectionRequest bascketSelectionRequest, int page,
            int size) {
        ComplexSearchRequest complexReq = new ComplexSearchRequest(DataTypeSelection.ALL.getFileTypes(), page, size);
        complexReq.getRequests()
                .add(buildSearchRequest(bascketSelectionRequest.getEngineType(),
                                        bascketSelectionRequest.getDatasetUrn(),
                                        bascketSelectionRequest.getSearchParameters(),
                                        bascketSelectionRequest.getIpIds(),
                                        bascketSelectionRequest.getSelectionDate()));
        return complexReq;

    }

    public static ComplexSearchRequest buildSearchRequest(BasketDatasetSelection datasetSelection, int page, int size) {
        ComplexSearchRequest request = new ComplexSearchRequest(DataTypeSelection.ALL.getFileTypes(), page, size);
        datasetSelection.getItemsSelections().forEach(selectionItem -> {
            request.getRequests()
                    .add(buildSearchRequest(selectionItem.getSelectionRequest().getEngineType(),
                                            selectionItem.getSelectionRequest().getDatasetUrn(),
                                            selectionItem.getSelectionRequest().getSearchParameters(),
                                            selectionItem.getSelectionRequest().getIpIds(),
                                            selectionItem.getSelectionRequest().getSelectionDate()));
        });
        return request;
    }
}
