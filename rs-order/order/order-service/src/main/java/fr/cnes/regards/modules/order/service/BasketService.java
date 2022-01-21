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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
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
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.order.service.processing.OrderProcessingService;
import fr.cnes.regards.modules.processing.client.IProcessingRestClient;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;
import fr.cnes.regards.modules.processing.order.OrderProcessInfoMapper;
import fr.cnes.regards.modules.processing.order.SizeLimit;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineClient;
import fr.cnes.regards.modules.search.domain.ComplexSearchRequest;
import fr.cnes.regards.modules.search.domain.SearchRequest;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

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

    @Autowired
    private IProcessingRestClient processingClient;

    @Override
    public Basket findOrCreate(String user) {
        Basket basket = repos.findByOwner(user);
        return basket == null ? repos.save(new Basket(user)) : basket;
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
            repos.deleteById(basket.getId());
        }
    }

    @Override
    public Basket load(Long id) {
        return repos.findOneById(id);
    }

    @Override
    public Basket addSelection(Long basketId, BasketSelectionRequest selectionRequest) throws EmptySelectionException, TooManyItemsSelectedInBasketException {
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

                // check if a process is associated to the dataset
                // if not, add the selection without check
                // if yes, get the associated process and check if the updated number of items to process is
                // less that the limit defined.
                if(datasetSelection.getProcessDatasetDescription() != null) {
                    checkLimitElementsInOrder(datasetSelection.getProcessDatasetDescription().getProcessBusinessId().toString(),
                            datasetSelection);
                }

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

    @Override
    public Basket attachProcessing(
            Basket basket,
            Long datasetId,
            @Nullable ProcessDatasetDescription desc
    ) throws TooManyItemsSelectedInBasketException {
        // find dataset selection with selected id
        BasketDatasetSelection basketSelection = basket.getDatasetSelections().stream()
                .filter(ds -> ds.getId().equals(datasetId))
                .findFirst().orElseThrow(() -> new EntityNotFoundException("Basket selection with id " + datasetId + " doesn't exist"));
        // check the number of items in the basket only if a process is associated to the dataset
        if (desc != null) {
            checkLimitElementsInOrder(desc.getProcessBusinessId().toString(), basketSelection);
        }
        // attach the process to the dataset if no error has occurred
        attachProcessToDatasetSelectionAndSaveBasket(basket, basketSelection, desc);
        return basket;
    }

    private void checkLimitElementsInOrder(String processBusinessId,
                                           BasketDatasetSelection basketSelection) throws TooManyItemsSelectedInBasketException {
        // retrieve the processing to get configuration parameters
        ResponseEntity<PProcessDTO> processResponse = processingClient.findByUuid(processBusinessId);
        if (processResponse.getBody() != null && processResponse.getStatusCode() == HttpStatus.OK) {
            OrderProcessInfoMapper orderProcessInfoMapper = new OrderProcessInfoMapper();
            OrderProcessInfo processInfo =
                    orderProcessInfoMapper.fromMap(processResponse.getBody().getProcessInfo()).getOrElseThrow(() -> new OrderProcessingService.UnparsableProcessInfoException(
                            String.format("Unparsable description info from process plugin with id %s", processBusinessId)));

            // limit the number of items ordered only if forbidSplitInSuborders is active
            if(processInfo.getForbidSplitInSuborders()) {
                // check if the number of items in the basket does not exceed maximum number of items configured
                // by the process. The numberOfElementsToCheck is init to 0 by default in case there is no limit
                // configured in the process
                long numberOfElementsToCheck = 0L;
                SortedSet<BasketDatedItemsSelection> selectedItemsInOrder = basketSelection.getItemsSelections();
                SizeLimit sizeLimit = processInfo.getSizeLimit();
                switch (sizeLimit.getType()) {
                    case FEATURES:
                        numberOfElementsToCheck = selectedItemsInOrder.stream().map(BasketDatedItemsSelection::getObjectsCount).reduce(0, Integer::sum);
                        break;
                    case FILES:
                        numberOfElementsToCheck =
                                selectedItemsInOrder.stream().map(item -> item.getFileTypesCount().values().stream().reduce(0L, Long::sum)).reduce(0L, Long::sum);
                        break;
                    case BYTES:
                        numberOfElementsToCheck =
                                selectedItemsInOrder.stream().map(item -> item.getFileTypesSizes().values().stream().reduce(0L, Long::sum)).reduce(0L, Long::sum);
                        break;
                    default:
                        break;
                }
                if (numberOfElementsToCheck > processInfo.getSizeLimit().getLimit()) {
                    throw new TooManyItemsSelectedInBasketException(String.format("The number of selected \"%s\" in " +
                                                                                          "the basket [%d] exceeds the maximum number of \"%s\" allowed [%d]. Please, decrease the " +
                                                                                          "number of selected items or contact the administrator for more information.",
                                                                                  sizeLimit.getType(), numberOfElementsToCheck, sizeLimit.getType(), processInfo.getSizeLimit().getLimit()));
                }
            }
        }
    }

    @Override
    public Basket duplicate(Long id, String owner) {
        Basket oldBasket = load(id);
        Basket newBasket = new Basket();
        BeanUtils.copyProperties(oldBasket, newBasket, "id", "owner");
        newBasket.setOwner(owner);
        oldBasket.getDatasetSelections().forEach(basketDatasetSelection -> {
            BasketDatasetSelection newBasketDatasetSelection = new BasketDatasetSelection();
            BeanUtils.copyProperties(basketDatasetSelection, newBasketDatasetSelection, "id");
            basketDatasetSelection.getItemsSelections().forEach(basketDatedItemsSelection -> {
                BasketDatedItemsSelection newBasketDatedItemsSelection = new BasketDatedItemsSelection();
                BeanUtils.copyProperties(basketDatedItemsSelection, newBasketDatedItemsSelection);
                newBasketDatasetSelection.addItemsSelection(newBasketDatedItemsSelection);
            });
            newBasket.addDatasetSelection(newBasketDatasetSelection);
        });
        return repos.save(newBasket);
    }

    @Override
    public Basket transferOwnerShip(String fromOwner, String toOwner) {
        Basket basket = repos.findByOwner(fromOwner);
        basket.setOwner(toOwner);
        return repos.save(basket);
    }

    private Basket attachProcessToDatasetSelectionAndSaveBasket(
            Basket basket,
            BasketDatasetSelection ds,
            ProcessDatasetDescription desc
    ) {
        ds.setProcessDatasetDescription(desc);
        Basket modified = repos.save(basket);
        return modified;
    }

    /**
     * Create dated items selection
     * @param selectionRequest opensearch request from which this selection is created
     * @param subSummary dataset  selection (sub-)summary
     * @return dated items selection (what else ?)
     */
    private BasketDatedItemsSelection createItemsSelection(BasketSelectionRequest selectionRequest,
            DocFilesSubSummary subSummary) {
        // Create a new basket dated items selection
        BasketDatedItemsSelection itemsSelection = new BasketDatedItemsSelection();
        itemsSelection.setSelectionRequest(selectionRequest);
        itemsSelection.setObjectsCount((int) subSummary.getDocumentsCount());
        DataTypeSelection.ALL.getFileTypes().stream()
            .map(DataType::toString)
            .flatMap(ft -> Stream.of(ft, ft+"_ref", ft+"_!ref"))
            .forEach(ft -> {
                FilesSummary fs = subSummary.getFileTypesSummaryMap().get(ft);
                itemsSelection.setFileTypeCount(ft, fs.getFilesCount());
                itemsSelection.setFileTypeSize(ft, fs.getFilesSize());
            });
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
        } else {
            datasetSelection.setObjectsCount((int) curDsSelectionSubSummary.getDocumentsCount());
            curDsSelectionSubSummary.getFileTypesSummaryMap()
                .forEach((fileType, fs) -> {
                    datasetSelection.setFileTypeCount(fileType, fs.getFilesCount());
                    datasetSelection.setFileTypeSize(fileType, fs.getFilesSize());
                });
        }
    }

    public static ComplexSearchRequest buildSearchRequest(BasketSelectionRequest bascketSelectionRequest, int page,
            int size) {
        ComplexSearchRequest complexReq = new ComplexSearchRequest(DataTypeSelection.ALL.getFileTypes(), page, size);
        complexReq.getRequests()
                .add(new SearchRequest(bascketSelectionRequest.getEngineType(), bascketSelectionRequest.getDatasetUrn(),
                        bascketSelectionRequest.getSearchParameters(), bascketSelectionRequest.getEntityIdsToInclude(),
                        bascketSelectionRequest.getEntityIdsToExclude(), bascketSelectionRequest.getSelectionDate()));
        return complexReq;

    }

    public static ComplexSearchRequest buildSearchRequest(BasketDatasetSelection datasetSelection, int page, int size) {
        ComplexSearchRequest request = new ComplexSearchRequest(DataTypeSelection.ALL.getFileTypes(), page, size);
        datasetSelection.getItemsSelections().forEach(selectionItem -> {
            // If selected dataset is not defined in the item selection request, use the one from the datasetSelection.
            String datasetUrn = Optional.ofNullable(selectionItem.getSelectionRequest().getDatasetUrn())
                    .orElse(datasetSelection.getDatasetIpid());
            request.getRequests()
                    .add(new SearchRequest(selectionItem.getSelectionRequest().getEngineType(), datasetUrn,
                            selectionItem.getSelectionRequest().getSearchParameters(),
                            selectionItem.getSelectionRequest().getEntityIdsToInclude(),
                            selectionItem.getSelectionRequest().getEntityIdsToExclude(),
                            selectionItem.getSelectionRequest().getSelectionDate()));
        });
        return request;
    }

}
