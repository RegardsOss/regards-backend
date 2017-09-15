package fr.cnes.regards.modules.order.rest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.DigestUtils;

import com.netflix.discovery.converters.Auto;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.storage.client.IAipClient;

/**
 * @author oroussel
 */
@TestPropertySource(locations = "classpath:test.properties")
public class OrderDataFileControllerIT extends AbstractRegardsIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private IOrderDataFileRepository dataFileRepository;

    @Autowired
    private IAipClient aipClient;

    private static final String USER = "raphael@mechoui.fr";

    public static final UniformResourceName DS1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
                                                                                "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA,
                                                                                "ORDER", UUID.randomUUID(), 1);

    @Configuration
    static class Conf {

        @Bean
        public ICatalogClient catalogClient() {
            return Mockito.mock(ICatalogClient.class);
        }

        @Bean
        public IAipClient aipClient() {
            IAipClient mock = Mockito.mock(IAipClient.class);
            Mockito.when(mock.downloadFile(Matchers.anyString(), Mockito.eq("FILE1")))
                    .thenReturn(this.getClass().getResourceAsStream("/files/file1.txt"));
            return mock;
        }
    }

    @Before
    public void init() {
        tenantResolver.forceTenant(DEFAULT_TENANT);

        orderRepository.deleteAll();
        dataFileRepository.deleteAll();
    }

    @Test
    public void testDownloadFile() throws URISyntaxException, IOException {
        System.out.println(aipClient.downloadFile("", "FILE1"));

        Order order = new Order();
        order.setEmail(USER);
        order.setCreationDate(OffsetDateTime.now());

        // One dataset task
        DatasetTask ds1Task = new DatasetTask();
        ds1Task.setDatasetIpid(DS1_IP_ID.toString());
        ds1Task.setDatasetLabel("DS1");

        order.addDatasetOrderTask(ds1Task);

        FilesTask files1Task = new FilesTask();
        OrderDataFile dataFile1 = new OrderDataFile();
        dataFile1.setUrl("file:///test/files/file1.txt");
        dataFile1.setName("file1.txt");
        dataFile1.setIpId(DO1_IP_ID);
        dataFile1.setState(FileState.ONLINE);
        dataFile1.setChecksum("FILE1");
        files1Task.addFile(dataFile1);
    }
}
