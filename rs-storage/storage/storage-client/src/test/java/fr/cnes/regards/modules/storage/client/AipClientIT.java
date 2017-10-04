package fr.cnes.regards.modules.storage.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.EventType;

@TestPropertySource("classpath:test.properties")
public class AipClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AipClientIT.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private final static String workspace = "target/workspace";

    private IAipClient client;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Value("${server.address}")
    private String serverAddress;

    @BeforeClass
    public static void initAll() throws IOException {
        if (Paths.get(workspace).toFile().exists()) {
            FileUtils.deleteDirectory(Paths.get(workspace).toFile());
        }
        Files.createDirectory(Paths.get(workspace));
    }

    @Before
    public void init() {
        client = FeignClientBuilder.build(new TokenClientProvider<>(IAipClient.class,
                "http://" + serverAddress + ":" + getPort(), feignSecurityManager));
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        FeignSecurityManager.asSystem();
    }

    @Test
    public void testCreateAIP() {
        AIPBuilder builder = new AIPBuilder(EntityType.DATASET, "AIP:DATASET:test-client-aip-1", "clientAipTest");
        builder.addEvent(EventType.SUBMISSION.toString(), "Creation", OffsetDateTime.now());
        AIP aip = builder.build();
        Set<AIP> aips = Sets.newHashSet(aip);
        client.createAIP(aips);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
