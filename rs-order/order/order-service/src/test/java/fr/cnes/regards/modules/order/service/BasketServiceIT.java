package fr.cnes.regards.modules.order.service;

import java.sql.Date;
import java.text.SimpleDateFormat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import static fr.cnes.regards.modules.order.test.SearchClientMock.*;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

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
    private IOrderService orderService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IEmailClient emailClient;

    private static final String USER_EMAIL = "marc.sordi@baltringue.fr";

    @Before
    public void setUp() {
        basketRepository.deleteAll();
        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
        Mockito.when(authResolver.getUser()).thenReturn(USER_EMAIL);

        Project project = new Project();
        project.setHost("regardsHost");
        Mockito.when(projectsClient.retrieveProject(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new Resource<>(project), HttpStatus.OK));
    }

    /**
     * BECAUSE OF OffsetDateTime.now() used by BasketService, THIS TEST CLASS MUST DEFINE ONLY ONE TEST
     */
    @Test
    @Requirement("REGARDS_DSL_STO_CMD_100")
    public void test() throws EmptyBasketException, EmptySelectionException {
        Basket basket = basketService.findOrCreate(USER_EMAIL);

        Assert.assertNotNull(basketService.find(USER_EMAIL));

        // Add a selection on DS1 => 2 documents, 2 RAWDATA files + 6 QUICKLOOKS 2 x 3 of each size, 1 Mb each RAW
        // file, 500 b QUICKLOOK SD, 1 kb MD, 500 kb HD
        basketService.addSelection(basket.getId(), DS1_IP_ID.toString(), "");

        basket = basketService.load(basket.getId());
        Assert.assertEquals(1, basket.getDatasetSelections().size());
        BasketDatasetSelection dsSelection = basket.getDatasetSelections().first();
        Assert.assertEquals(DS1_IP_ID.toString(), dsSelection.getDatasetIpid());
        Assert.assertEquals(8, dsSelection.getFilesCount());
        Assert.assertEquals(2, dsSelection.getObjectsCount());
        Assert.assertEquals(3_003_000l, dsSelection.getFilesSize());

        // Add a selection on DS2 and DS3 with an opensearch request
        basketService
                .addSelection(basket.getId(), "tags:(" + DS2_IP_ID.toString() + " OR " + DS3_IP_ID.toString() + ")");
        basket = basketService.load(basket.getId());
        Assert.assertEquals(3, basket.getDatasetSelections().size());
        for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
            // No change on DS1
            if (dsSel.getDatasetIpid().equals(DS1_IP_ID.toString())) {
                Assert.assertEquals(8, dsSel.getFilesCount());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(3_003_000l, dsSel.getFilesSize());
            } else if (dsSel.getDatasetIpid().equals(DS2_IP_ID.toString())) {
                Assert.assertEquals(8, dsSel.getFilesCount());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(2_020_202l, dsSel.getFilesSize());
            } else if (dsSel.getDatasetIpid().equals(DS3_IP_ID.toString())) {
                Assert.assertEquals(4, dsSel.getFilesCount());
                Assert.assertEquals(1, dsSel.getObjectsCount());
                Assert.assertEquals(1_010_101l, dsSel.getFilesSize());
            } else {
                Assert.fail("Unknown Dataset !!!");
            }
        }

        // Add a selection on all DS (DS1, 2, 3) : for DS1, same results as previous must be returned
        basketService.addSelection(basket.getId(), "");

        basket = basketService.load(basket.getId());
        Assert.assertEquals(3, basket.getDatasetSelections().size());
        // Computations on dataset selections must not have been changed (concerns same files as previous)
        for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
            if (dsSel.getDatasetIpid().equals(DS1_IP_ID.toString())) {
                Assert.assertEquals(8, dsSel.getFilesCount());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(3_003_000l, dsSel.getFilesSize());
                // Must have 2 itemsSelections
                Assert.assertEquals(2, dsSel.getItemsSelections().size());
                // And both must have same values as dataset selection (only date changed and opensearch request)
                for (BasketDatedItemsSelection itemsSel : dsSel.getItemsSelections()) {
                    Assert.assertEquals(dsSel.getFilesCount(), itemsSel.getFilesCount());
                    Assert.assertEquals(dsSel.getFilesSize(), itemsSel.getFilesSize());
                }
            } else if (dsSel.getDatasetIpid().equals(DS2_IP_ID.toString())) {
                Assert.assertEquals(8, dsSel.getFilesCount());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(2_020_202l, dsSel.getFilesSize());
                // Must have 2 itemsSelections
                Assert.assertEquals(2, dsSel.getItemsSelections().size());
                // And both must have same values as dataset selection (only date changed and opensearch request)
                for (BasketDatedItemsSelection itemsSel : dsSel.getItemsSelections()) {
                    Assert.assertEquals(dsSel.getFilesCount(), itemsSel.getFilesCount());
                    Assert.assertEquals(dsSel.getFilesSize(), itemsSel.getFilesSize());
                }
            } else if (dsSel.getDatasetIpid().equals(DS3_IP_ID.toString())) {
                Assert.assertEquals(4, dsSel.getFilesCount());
                Assert.assertEquals(1, dsSel.getObjectsCount());
                Assert.assertEquals(1_010_101l, dsSel.getFilesSize());
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

        Mockito.when(emailClient.sendEmail(Mockito.any())).thenAnswer(invocation -> {
            mailMessage = (SimpleMailMessage) invocation.getArguments()[0];
            return new ResponseEntity<>(HttpStatus.CREATED);
        });

        Order order = orderService.createOrder(basket, "http://perdu.com");

        // Email sending test
        Assert.assertNotNull(mailMessage);
        Assert.assertEquals(order.getOwner(), mailMessage.getTo()[0]);
        // Check that email text has been interpreted before being sent
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        Assert.assertTrue(mailMessage.getText().contains(sdf.format(Date.from(order.getExpirationDate().toInstant()))));
        Assert.assertFalse(mailMessage.getText().contains("${expiration_date}"));
        Assert.assertFalse(mailMessage.getText().contains("${metalink_download_url}"));
        Assert.assertFalse(mailMessage.getText().contains("${regards_downloader_url}"));
        Assert.assertFalse(mailMessage.getText().contains("${orders_url}"));
        // Reset emailMessage
        mailMessage = null;

        // manage periodic email notifications
        orderService.sendPeriodicNotifications();
    }

    static SimpleMailMessage mailMessage;
}
