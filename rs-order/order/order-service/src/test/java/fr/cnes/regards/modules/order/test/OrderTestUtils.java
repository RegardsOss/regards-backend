package fr.cnes.regards.modules.order.test;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.OffsetDateTime;
import java.util.Random;

public class OrderTestUtils {

    public static Basket getBasketSingleSelection(String prefix) {
        String orderOwner = randomLabel(prefix);
        Basket basket = new Basket(orderOwner);
        BasketDatasetSelection dsSelection = getDatasetSelection(SearchClientMock.DS1_IP_ID, "DS");
        basket.addDatasetSelection(dsSelection);
        return basket;
    }

    public static Basket getBasketDoubleSelection(String prefix) {
        String orderOwner = randomLabel(prefix);
        Basket basket = new Basket(orderOwner);
        BasketDatasetSelection dsSelection = getDatasetSelection(SearchClientMock.DS1_IP_ID, "DS");
        basket.addDatasetSelection(dsSelection);
        BasketDatasetSelection dsSelection2 = getDatasetSelection(SearchClientMock.DS2_IP_ID, "DS-2");
        basket.addDatasetSelection(dsSelection2);
        return basket;
    }

    private static String randomLabel(String prefix) {
        return prefix + "_" + Long.toHexString(new Random().nextLong());
    }

    public static BasketDatasetSelection getDatasetSelection(UniformResourceName ipid, String label) {
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(ipid.toString());
        dsSelection.setDatasetLabel(label);
        dsSelection.setObjectsCount(3);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name() + "_ref", 0L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name() + "_ref", 0L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name() + "_!ref", 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name() + "_!ref", 12L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 12L);
        dsSelection.addItemsSelection(createDatasetItemSelection(1L, 12, 3, "ALL"));
        return dsSelection;
    }

    public static BasketDatedItemsSelection createDatasetItemSelection(long filesSize,
                                                                       long filesCount,
                                                                       int objectsCount,
                                                                       String query) {
        BasketDatedItemsSelection item = new BasketDatedItemsSelection();
        item.setFileTypeSize(DataType.RAWDATA.name() + "_ref", 0L);
        item.setFileTypeCount(DataType.RAWDATA.name() + "_ref", 0L);
        item.setFileTypeSize(DataType.RAWDATA.name() + "_!ref", filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name() + "_!ref", filesCount);
        item.setFileTypeSize(DataType.RAWDATA.name(), filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name(), filesCount);
        item.setObjectsCount(objectsCount);
        item.setDate(OffsetDateTime.now());
        item.setSelectionRequest(createBasketSelectionRequest(query));
        return item;
    }

    public static BasketSelectionRequest createBasketSelectionRequest(String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        return request;
    }

}
