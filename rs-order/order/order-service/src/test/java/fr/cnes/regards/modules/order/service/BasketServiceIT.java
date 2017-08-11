package fr.cnes.regards.modules.order.service;

import javax.validation.constraints.AssertTrue;

import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import static fr.cnes.regards.modules.order.test.CatalogClientMock.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.order.test.CatalogClientMock;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.search.client.ICatalogClient;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
public class BasketServiceIT {

    @Autowired
    private IBasketService basketService;

    @Autowired
    private IBasketRepository basketRepository;

    @Autowired
    private ICatalogClient catalogClientMocked;

    private static final String USER_EMAIL = "marc.sordi@baltringue.fr";

    @Before
    public void setUp() {
        basketRepository.deleteAll();
    }

    /**
     * BECAUSE OF OffsetDateTime.now() use from BasketService, THIS TEST CLASS MUST DEFINE ONLY ONLY TEST
     */
    @Test
    public void test() {
        Basket basket = basketService.create(USER_EMAIL);

        Assert.assertNotNull(basketService.find(USER_EMAIL));

        // Add a selection on DS1 => 2 documents, 2 RAWDATA files (+ 6 QUICKLOOKS 2 x 3 of each size), 1 Mb each RAW
        // file, 500 b QUICKLOOK SD, 1 kb MD, 500 kb HD
        basketService.addSelection(basket.getId(), DS1_IP_ID.toString(), "");

        basket = basketService.load(basket.getId());
        Assert.assertEquals(1, basket.getDatasetSelections().size());
        BasketDatasetSelection dsSelection = basket.getDatasetSelections().first();
        Assert.assertEquals(DS1_IP_ID.toString(), dsSelection.getDatasetIpid());
        // Default selection type
        Assert.assertEquals(DataTypeSelection.RAWDATA, dsSelection.getDataTypeSelection());
        Assert.assertEquals(2, dsSelection.getFilesCount());
        Assert.assertEquals(2, dsSelection.getObjectsCount());
        Assert.assertEquals(2_000_000l, dsSelection.getFilesSize());

        // Change Data type selection for DS1 => ONLY QUICKLOOKS
        basketService
                .setFileTypes(basket.getId(), DS1_IP_ID.toString(), DataTypeSelection.QUICKLOOKS);
        basket = basketService.load(basket.getId());
        Assert.assertEquals(1, basket.getDatasetSelections().size());
        dsSelection = basket.getDatasetSelections().first();
        Assert.assertEquals(DS1_IP_ID.toString(), dsSelection.getDatasetIpid());
        Assert.assertEquals(6, dsSelection.getFilesCount());
        Assert.assertEquals(2, dsSelection.getObjectsCount());
        Assert.assertEquals(1_003_000l, dsSelection.getFilesSize());

        // Add a selection on DS2 and DS3 with an opensearch request
        basketService.addSelection(basket.getId(), "tags:(" + DS2_IP_ID.toString() + " OR "
                + DS3_IP_ID.toString() + ")");
        basket = basketService.load(basket.getId());
        Assert.assertEquals(3, basket.getDatasetSelections().size());
        for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
            if (dsSel.getDatasetIpid().equals(DS1_IP_ID.toString())) {
                Assert.assertEquals(DataTypeSelection.QUICKLOOKS, dsSel.getDataTypeSelection());
                Assert.assertEquals(6, dsSel.getFilesCount());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(1_003_000l, dsSel.getFilesSize());
            } else if (dsSel.getDatasetIpid().equals(DS2_IP_ID.toString())) {
                Assert.assertEquals(DataTypeSelection.RAWDATA, dsSel.getDataTypeSelection());
                Assert.assertEquals(2, dsSel.getFilesCount());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(2_000_000l, dsSel.getFilesSize());
            } else if (dsSel.getDatasetIpid().equals(DS3_IP_ID.toString())) {
                Assert.assertEquals(DataTypeSelection.RAWDATA, dsSel.getDataTypeSelection());
                Assert.assertEquals(1, dsSel.getFilesCount());
                Assert.assertEquals(1, dsSel.getObjectsCount());
                Assert.assertEquals(1_000_000l, dsSel.getFilesSize());
            } else {
                Assert.fail("Unknown Dataset !!!");
            }
        }

        // Add a selection on all DS (DS1, 2, 3) : for DS1, same results as previous must be returned (so 8 files BUT
        // as ONLY QUICKLOOKS are asked now, only 6 files now)
        basketService.addSelection(basket.getId(), "");

        basket = basketService.load(basket.getId());
        Assert.assertEquals(3, basket.getDatasetSelections().size());
        // Computations on dataset selections must not have been changed (concerns same files as previous)
        for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
            if (dsSel.getDatasetIpid().equals(DS1_IP_ID.toString())) {
                Assert.assertEquals(DataTypeSelection.QUICKLOOKS, dsSel.getDataTypeSelection());
                Assert.assertEquals(6, dsSel.getFilesCount());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(1_003_000l, dsSel.getFilesSize());
                // Must have 2 itemsSelections
                Assert.assertEquals(2, dsSel.getItemsSelections().size());
                // And both must have same values as dataset selection (only date changed and opensearch request)
                for (BasketDatedItemsSelection itemsSel : dsSel.getItemsSelections()) {
                    Assert.assertEquals(dsSel.getFilesCount(), itemsSel.getFilesCount());
                    Assert.assertEquals(dsSel.getFilesSize(), itemsSel.getFilesSize());
                }
            } else if (dsSel.getDatasetIpid().equals(DS2_IP_ID.toString())) {
                Assert.assertEquals(DataTypeSelection.RAWDATA, dsSel.getDataTypeSelection());
                Assert.assertEquals(2, dsSel.getFilesCount());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(2_000_000l, dsSel.getFilesSize());
                // Must have 2 itemsSelections
                Assert.assertEquals(2, dsSel.getItemsSelections().size());
                // And both must have same values as dataset selection (only date changed and opensearch request)
                for (BasketDatedItemsSelection itemsSel : dsSel.getItemsSelections()) {
                    Assert.assertEquals(dsSel.getFilesCount(), itemsSel.getFilesCount());
                    Assert.assertEquals(dsSel.getFilesSize(), itemsSel.getFilesSize());
                }
            } else if (dsSel.getDatasetIpid().equals(DS3_IP_ID.toString())) {
                Assert.assertEquals(DataTypeSelection.RAWDATA, dsSel.getDataTypeSelection());
                Assert.assertEquals(1, dsSel.getFilesCount());
                Assert.assertEquals(1, dsSel.getObjectsCount());
                Assert.assertEquals(1_000_000l, dsSel.getFilesSize());
                // Must have 2 itemsSelections
                Assert.assertEquals(2, dsSel.getItemsSelections().size());
                // And both must have same values as dataset selection (only date changed and opensearch request)
                for (BasketDatedItemsSelection itemsSel : dsSel.getItemsSelections()) {
                    Assert.assertEquals(dsSel.getFilesCount(), itemsSel.getFilesCount());
                    Assert.assertEquals(dsSel.getFilesSize(), itemsSel.getFilesSize());
                }
            } else {
                Assert.fail("Unknown Dataset !!!");
            }
        }

    }
}
