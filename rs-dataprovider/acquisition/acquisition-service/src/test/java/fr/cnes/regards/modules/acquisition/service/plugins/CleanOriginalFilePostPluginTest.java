/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.builder.AcquisitionFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.AcquisitionProcessingChainBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.builder.ProductBuilder;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.FileAcquisitionInformations;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingChainService;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;
import fr.cnes.regards.modules.acquisition.service.IMetaProductService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.IScanDirectoryService;
import fr.cnes.regards.modules.acquisition.service.conf.AcquisitionProcessingChainConfiguration;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AcquisitionProcessingChainConfiguration.class })
@DirtiesContext
@ActiveProfiles({ "test", "disableDataProviderTask" })
public class CleanOriginalFilePostPluginTest extends AcquisitionScanPluginHelper {

    /**
     * Static default tenant
     */
    @Value("${regards.tenant}")
    private String tenant;

    private static final String CHAIN_LABEL = "the chain label";

    private static final String DATASET_NAME = "the dataset name";

    private static final String META_PRODUCT_NAME = "the meta product name";

    private static final String DEFAULT_USER = "John Doe";

    private static final String PATTERN_FILTER = "[A-Z]{4}_MESURE_TC_([0-9]{8}_[0-9]{6}).TXT";

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    /*
     *  @see https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest
     */
    private static final String CHECKUM_ALGO = "SHA-256";

    @Autowired
    private IMetaProductService metaProductService;

    @Autowired
    private IMetaFileService metaFileService;

    @Autowired
    private IScanDirectoryService scandirService;

    @Autowired
    private IAcquisitionProcessingChainService acqProcessChainService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authenticationResolver;

    @Autowired
    private IMetaProductRepository metaProductRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    private IAcquisitionProcessingChainRepository processingChainRepository;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    private IMetaFileRepository metaFileRepository;

    private AcquisitionProcessingChain chain;

    private final URL dataPath = getClass().getResource("/data");

    private MetaProduct metaProduct;

    private MetaFile metaFile;

    @Before
    public void setUp() throws Exception {
        tenantResolver.forceTenant(tenant);

        cleanDb();

        initData();

        Mockito.when(authenticationResolver.getUser()).thenReturn(DEFAULT_USER);
    }

    @After
    public void cleanDb() {
        scanDirectoryRepository.deleteAll();
        productRepository.deleteAll();
        acquisitionFileRepository.deleteAll();
        processingChainRepository.deleteAll();
        metaProductRepository.deleteAll();
        metaFileRepository.deleteAll();
    }

    /**
     * Initialize data for this tests
     * @throws ModuleException an error occurs
     */
    public void initData() throws ModuleException {
        ScanDirectory scanDir = scandirService.save(new ScanDirectory(dataPath.getPath()));

        // Set the last modified date of the file most recent that the last acquisition date  
        File dir = new File(dataPath.getPath());
        for (File ff : dir.listFiles()) {
            ff.setLastModified(1000 * NOW.minusMinutes(70).toEpochSecond());
        }

        metaFile = metaFileService.createOrUpdate(MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withMediaType(MediaType.APPLICATION_JSON_VALUE).withFilePattern(PATTERN_FILTER)
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir).get());

        metaProduct = metaProductService.createOrUpdate(MetaProductBuilder.build(META_PRODUCT_NAME)
                .addMetaFile(metaFile).withChecksumAlgorithm(CHECKUM_ALGO).get());

        chain = acqProcessChainService.createOrUpdate(AcquisitionProcessingChainBuilder.build(CHAIN_LABEL)
                .withDataSet(DATASET_NAME).withMetaProduct(metaProduct).lastActivation(NOW.minusMinutes(75)).get());
    }

    @Test
    public void createAcquittement() throws ModuleException, IOException {
        String folder = "ack_rs_test";
        String extension = ".cmz";
        IPostProcessSipPlugin plugin = pluginService
                .getPlugin(pluginService
                        .getPluginConfiguration("CleanOriginalFilePostPlugin", IPostProcessSipPlugin.class)
                        .getId(), PluginParametersFactory.build()
                                .addDynamicParameter(CleanOriginalFilePostPlugin.CREATE_ACK_PARAM, Boolean.TRUE)
                                .addDynamicParameter(CleanOriginalFilePostPlugin.EXTENSION_ACK_PARAM, extension)
                                .addDynamicParameter(CleanOriginalFilePostPlugin.FOLDER_ACK_PARAM, folder)
                                .getParameters().toArray(new PluginParameter[1]));

        chain.setSession("session-001");
        acqProcessChainService.save(chain);

        Product product = null;
        File tmpFile = File.createTempFile("file-100", ".dat");
        FileAcquisitionInformations fai1 = new FileAcquisitionInformations();
        fai1.setAcquisitionDirectory("/tmp");
        product = createProduct("product-100", true, ProductState.COMPLETED, tmpFile.getName(), fai1);
        Assert.assertNotNull(product);

        plugin.runPlugin(product, chain);

        File acqFolder = new File(fai1.getAcquisitionDirectory(), folder);
        Assert.assertTrue(acqFolder.exists());

        File acqFile = new File(acqFolder, tmpFile.getName() + extension);
        Assert.assertTrue(acqFile.exists());
    }

    @Test
    public void doNotCreateAcquittement() throws ModuleException {
        IPostProcessSipPlugin plugin = pluginService.getPlugin(pluginService
                .getPluginConfiguration("CleanOriginalFilePostPlugin", IPostProcessSipPlugin.class).getId());

        chain.setSession("session-001");
        acqProcessChainService.save(chain);

        Product product = null;
        try {
            File f1 = File.createTempFile("file-100", ".dat");
            FileAcquisitionInformations fai1 = new FileAcquisitionInformations();
            fai1.setAcquisitionDirectory("/tmp");
            product = createProduct("product-100", true, ProductState.COMPLETED, f1.getName(), fai1);
        } catch (IOException e) {
            Assert.fail();
        }
        Assert.assertNotNull(product);

        plugin.runPlugin(product, chain);
    }

    /**
     * {@link Product} creation
     * @param productName the {@link Product} name
     * @param sended <code>true</code> or <code>false</code> if the {@link SIP} is send
     * @param status the {@link ProductState}
     * @param fileName the file name of the {@link Product}
     * @param fileAcqInf the {@link FileAcquisitionInformations}
     * @return the created {@link Product}
     * @throws ModuleException if an error occurs
     */
    private Product createProduct(String productName, boolean sended, ProductState status, String fileName,
            FileAcquisitionInformations fileAcqInf) throws ModuleException {
        Product product = ProductBuilder.build(productName).withStatus(status).withMetaProduct(metaProduct)
                .isSended(sended).withSession(chain.getSession()).get();
        product.addAcquisitionFile(acquisitionFileService
                .save(AcquisitionFileBuilder.build(fileName).withStatus(AcquisitionFileStatus.VALID.toString())
                        .withFileAcquisitionInformations(fileAcqInf).withMetaFile(metaFile).get()));

        return productService.save(product);
    }

    /**
     * {@link AcquisitionFile} creation
     * @param dir the folder that contain the data file
     * @param name the file name
     * @return the {@link AcquisitionFile} created
     */
    protected AcquisitionFile createAcquisition(String dir, String name) {
        File file = new File(getClass().getClassLoader().getResource(dir + "/" + name).getFile());
        AcquisitionFile af = initAcquisitionFile(file, metaFileService.retrieve(metaFile.getId()), CHECKUM_ALGO);
        af.setAcqDate(OffsetDateTime.now());

        return af;
    }
}
