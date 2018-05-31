package fr.cnes.regards.modules.order.service;

import javax.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.domain.summary.FilesSummary;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.search.client.ISearchClient;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class BasketService implements IBasketService {

    @Autowired
    private IBasketRepository repos;

    @Autowired
    private ISearchClient searchClient;

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
    public Basket addSelection(Long basketId, String datasetIpId, String inOpenSearchRequest)
            throws EmptySelectionException {
        Basket basket = repos.findOneById(basketId);
        if (basket == null) {
            throw new EntityNotFoundException("Basket with id " + basketId + " doesn't exist");
        }
        // Add current date on search request
        OffsetDateTime now = OffsetDateTime.now();
        String nowStr = OffsetDateTimeAdapter.format(now);
        String openSearchRequest = Strings.nullToEmpty(inOpenSearchRequest);
        if (!openSearchRequest.isEmpty()) {
            openSearchRequest = "(" + openSearchRequest + ") AND ";
        }
        openSearchRequest += "creationDate:[* TO " + nowStr + "]";
        // Compute summary for this selection
        Map<String, String> queryMap = new ImmutableMap.Builder<String, String>().put("q", openSearchRequest).build();
        try {
            FeignSecurityManager.asUser(authResolver.getUser(), authResolver.getRole());
            DocFilesSummary summary = searchClient
                    .computeDatasetsSummary(queryMap, datasetIpId, DataTypeSelection.ALL.getFileTypes()).getBody();
            // If global summary contains no files => EmptySelection
            if (summary.getFilesCount() == 0l) {
                throw new EmptySelectionException();
            }
            // Create a map to find more easiely a basket dataset selection from dataset IpId
            Map<String, BasketDatasetSelection> basketDsSelectionMap = basket.getDatasetSelections().stream()
                    .collect(Collectors.toMap(BasketDatasetSelection::getDatasetIpid, Function.identity()));
            // Parsing results
            for (Map.Entry<String, DocFilesSubSummary> entry : summary.getSubSummariesMap().entrySet()) {
                DocFilesSubSummary subSummary = entry.getValue();
                if (subSummary.getFilesCount() == 0l) {
                    continue;
                }
                // Try to retrieve current dataset selection
                BasketDatasetSelection datasetSelection = basketDsSelectionMap.get(entry.getKey());
                // Manage basket dataset selection
                if (datasetSelection == null) {
                    Dataset dataset = searchClient.getDataset(UniformResourceName.fromString(entry.getKey())).getBody()
                            .getContent();
                    datasetSelection = new BasketDatasetSelection();
                    datasetSelection.setDatasetIpid(entry.getKey());
                    datasetSelection.setDatasetLabel(dataset.getLabel());
                    datasetSelection.setOpenSearchRequest("(" + openSearchRequest + ")");
                    basket.getDatasetSelections().add(datasetSelection);
                } else { // update dataset opensearch request
                    datasetSelection.setOpenSearchRequest(
                            "(" + datasetSelection.getOpenSearchRequest() + " OR (" + openSearchRequest + "))");
                }
                // Create dated items selection
                BasketDatedItemsSelection itemsSelection = createItemsSelection(openSearchRequest, now, subSummary);

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
        for (Iterator<BasketDatasetSelection> i = basket.getDatasetSelections().iterator(); i.hasNext(); ) {
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
        for (Iterator<BasketDatasetSelection> j = basket.getDatasetSelections().iterator(); j.hasNext(); ) {
            BasketDatasetSelection dsSelection = j.next();
            if (dsSelection.getId().equals(datasetId)) {
                // Search for item selections to remove
                for (Iterator<BasketDatedItemsSelection> i = dsSelection.getItemsSelections().iterator(); i
                        .hasNext(); ) {
                    if (i.next().getDate().equals(itemsSelectionDate)) {
                        i.remove();
                        break;
                    }
                }
                // Must recompute dataset opensearch request from its associated dated items selections
                switch (dsSelection.getItemsSelections().size()) {
                    case 0:
                        // must delete dsSelection (no more dated items selections => no more datasetSelection)
                        j.remove();
                        break;
                    case 1: // only one dated items selection :
                        dsSelection.setOpenSearchRequest(
                                "(" + dsSelection.getItemsSelections().iterator().next().getOpenSearchRequest() + ")");
                        break;
                    default: // more than one dated items selections
                        dsSelection.setOpenSearchRequest(dsSelection.getItemsSelections().stream()
                                                                 .map(BasketDatedItemsSelection::getOpenSearchRequest)
                                                                 .collect(Collectors.joining(") OR (", "((", "))")));
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
    private BasketDatedItemsSelection createItemsSelection(String openSearchRequest, OffsetDateTime now,
            DocFilesSubSummary subSummary) {
        // Create a new basket dated items selection
        BasketDatedItemsSelection itemsSelection = new BasketDatedItemsSelection();
        itemsSelection.setDate(now);
        itemsSelection.setOpenSearchRequest(openSearchRequest);
        itemsSelection.setObjectsCount((int) subSummary.getDocumentsCount());
        int filesCount = 0;
        long filesSize = 0;
        for (String fileType : DataTypeSelection.ALL.getFileTypes()) {
            FilesSummary filesSummary = subSummary.getFileTypesSummaryMap().get(fileType);
            filesCount += filesSummary.getFilesCount();
            filesSize += filesSummary.getFilesSize();
        }
        itemsSelection.setFilesCount(filesCount);
        itemsSelection.setFilesSize(filesSize);
        return itemsSelection;
    }

    /**
     * (Re-)compute summary on a dataset selection
     */
    private void computeSummaryAndUpdateDatasetSelection(BasketDatasetSelection datasetSelection) {
        // Compute summary on dataset selection, need to recompute because of fileTypes differences between
        // previous call to computeDatasetSummary or because of opensearch request on dataset selection that is
        // a "OR" between previous one and new item selection
        Map<String, String> dsQueryMap = new ImmutableMap.Builder<String, String>()
                .put("q", datasetSelection.getOpenSearchRequest()).build();
        DocFilesSummary curDsSelectionSummary = searchClient
                .computeDatasetsSummary(dsQueryMap, datasetSelection.getDatasetIpid(),
                                        DataTypeSelection.ALL.getFileTypes()).getBody();
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
}
