package fr.cnes.regards.modules.order.service;

import javax.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EmptyBasketException;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.domain.summary.FilesSummary;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.search.client.ICatalogClient;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class BasketService implements IBasketService {

    @Autowired
    private IBasketRepository repos;

    @Autowired
    private ICatalogClient catalogClient;

    @Override
    public Basket findOrCreate(String user) {
        Basket basket = repos.findByEmail(user);
        return (basket == null) ? repos.save(new Basket(user)) : basket;
    }

    @Override
    public Basket find(String email) throws EmptyBasketException {
        Basket basket = repos.findByEmail(email);
        if (basket == null) {
            throw new EmptyBasketException();
        }
        return basket;
    }


    @Override
    public void deleteIfExists(String user) {
        Basket basket = repos.findByEmail(user);
        if (basket != null) {
            repos.delete(basket.getId());
        }
    }


    @Override
    public Basket load(Long id) {
        return repos.findOneById(id);
    }

    @Override
    public Basket addSelection(Long basketId, String datasetIpId, String openSearchRequest) {
        Basket basket = repos.findOneById(basketId);
        if (basket == null) {
            throw new EntityNotFoundException("Basket with id " + basketId + " doesn't exist");
        }
        // Add current date on search request
        OffsetDateTime now = OffsetDateTime.now();
        String nowStr = OffsetDateTimeAdapter.format(now);
        if (!openSearchRequest.isEmpty()) {
            openSearchRequest = "(" + openSearchRequest + ") AND ";
        }
        openSearchRequest += "creationDate:[* TO " + nowStr + "]";
        // Compute summary for this selection
        Map<String, String> queryMap = new ImmutableMap.Builder<String, String>().put("q", openSearchRequest).build();
        DocFilesSummary summary = catalogClient
                .computeDatasetsSummary(queryMap, datasetIpId, DataTypeSelection.ALL.getFileTypes()).getBody();
        // Create a map to find more easiely a basket dataset selection from dataset IpId
        Map<String, BasketDatasetSelection> basketDsSelectionMap = basket.getDatasetSelections().stream()
                .collect(Collectors.toMap(BasketDatasetSelection::getDatasetIpid, Function.identity()));
        // Parsing results
        for (Map.Entry<String, DocFilesSubSummary> entry : summary.getSubSummariesMap().entrySet()) {
            BasketDatasetSelection datasetSelection = basketDsSelectionMap.get(entry.getKey());
            // Manage basket dataset selection
            if (datasetSelection == null) {
                Dataset dataset = catalogClient.getDataset(UniformResourceName.fromString(entry.getKey())).getBody()
                        .getContent();
                datasetSelection = new BasketDatasetSelection();
                datasetSelection.setDatasetIpid(entry.getKey());
                datasetSelection.setDatasetLabel(dataset.getLabel());
                datasetSelection.setOpenSearchRequest("(" + openSearchRequest + ")");
                basket.getDatasetSelections().add(datasetSelection);
            } else { // update dataset opensearch request
                datasetSelection.setOpenSearchRequest(
                        datasetSelection.getOpenSearchRequest() + " OR (" + openSearchRequest + ")");
            }
            // Create dated items selection
            BasketDatedItemsSelection itemsSelection = createItemsSelection(openSearchRequest, now, entry.getValue());

            // Add items selection to dataset selection
            datasetSelection.getItemsSelections().add(itemsSelection);
            // Update DatasetSelection (summary)
            computeSummaryAndUpdateDatasetSelection(datasetSelection);

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
        for (BasketDatasetSelection dsSelection : basket.getDatasetSelections()) {
            if (dsSelection.getId().equals(datasetId)) {
                for (Iterator<BasketDatedItemsSelection> i = dsSelection.getItemsSelections().iterator(); i.hasNext(); ) {
                    if (i.next().getDate().equals(itemsSelectionDate)) {
                        i.remove();
                        break;
                    }
                }
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
        // a "OR" between previous one and nes item selection
        Map<String, String> dsQueryMap = new ImmutableMap.Builder<String, String>()
                .put("q", datasetSelection.getOpenSearchRequest()).build();
        DocFilesSummary curDsSelectionSummary = catalogClient
                .computeDatasetsSummary(dsQueryMap, datasetSelection.getDatasetIpid(),
                                        DataTypeSelection.ALL.getFileTypes()).getBody();
        // Take into account only asked datasetIpId (sub-)summary
        DocFilesSubSummary curDsSelectionSubSummary = curDsSelectionSummary.getSubSummariesMap()
                .get(datasetSelection.getDatasetIpid());
        datasetSelection.setObjectsCount((int) curDsSelectionSubSummary.getDocumentsCount());
        datasetSelection.setFilesCount((int) curDsSelectionSubSummary.getFilesCount());
        datasetSelection.setFilesSize(curDsSelectionSubSummary.getFilesSize());
    }
}
