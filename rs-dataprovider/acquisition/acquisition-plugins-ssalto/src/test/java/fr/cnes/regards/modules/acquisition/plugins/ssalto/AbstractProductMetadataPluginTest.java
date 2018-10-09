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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import com.google.common.base.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectDescriptionElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorFile;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers.DescriptorFileControler;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.Diff;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.xsd.IngestXsdResolver;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.xsd.XMLValidation;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.xsd.XMLValidatorFactory;

/**
 * Cette classe permet de simplifier la creation des tests sur les plugins.<br>
 * Pour l'utiliser, il suffit de creer une classe fille et d'implementer les methodes requises par l'interface.
 *
 * @author Christophe Mertz
 */
@SuppressWarnings("deprecation")
@ActiveProfiles({ "disableDataProviderTask", "noschedule" })
public abstract class AbstractProductMetadataPluginTest extends AbstractRegardsIT
        implements IProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProductMetadataPluginTest.class);

    @Autowired
    IPluginService pluginService;

    // ****************************************************************************************
    // PARAMETRES
    // ****************************************************************************************

    // Prefixe du descripteur de niveau produit
    protected static final String desc_product_ = "DESC_PRODUCT_";

    protected static final String CONFIG_FILE_SUFFIX = "_PluginConfiguration.xml";

    // [IN] repertoire dans lequel trouver les fichiers de donnees
    protected static final String LOCAL_ARCH_DIR = "src/test/resources/income/data";

    // [IN] repertoire dans lequel trouver le dico
    protected static final String DICO_DIR = "src/test/resources/income/dico";

    // [OUT] Repertoire dans lequel ecrire les descripteurs
    protected static final String DESCRIPTOR_DIRECTORY = "target/testing/descriptors";

    // Repertoire dans lequel sont stockees les descripteurs de reference
    protected static final String REFERENCES_DESCRIPTOR_DIRECTORY = "src/test/resources/outcome/descriptors_ref";

    // ****************************************************************************************
    // ATTRIBUT
    // ****************************************************************************************

    // Nom du dictionnaire
    protected String dicoName = null;

    // Nom du dictionnaire des types de base
    protected String dicoBase = null;

    // Nom du projet
    protected String projectName = null;

    // Liste des tests
    protected List<PluginTestDef> pluginTestDefList = new ArrayList<>();

    /**
     * Default constructor
     */
    public AbstractProductMetadataPluginTest() {
        super();
    }

    @Override
    public abstract ISIPGenerationPluginWithMetadataToolbox buildPlugin(String datasetName) throws ModuleException;

    /**
     * Initialisation des proprietes et création du répertoire de travail
     */
    @Before
    public void setUp() {
        LOGGER.info("Create context");

        Properties properties = new Properties();
        String propertyFilePath = getProjectProperties();

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(propertyFilePath)) {
            properties.load(stream);
        } catch (Exception e) {
            LOGGER.error("Unable to load file " + propertyFilePath, e);
            Assert.fail();
        }

        dicoName = properties.getProperty("dico");
        dicoBase = properties.getProperty("baseType");

        try {
            Files.createDirectories(Paths.get(DESCRIPTOR_DIRECTORY));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail();
        }
    }

    /**
     * Permet de tester un plugin a la fois
     */
    @Requirement("REGARDS_DSL_ING_SSALTO_070")
    @Purpose("A plugin can generate a SIP for all SSALTO's products")
    @Test
    public void createMetadataPlugin_solo() {
        try {
            initTestSoloList();
            existResources();
            launchTest();
        } catch (Exception e) {
            LOGGER.error("", e);
            Assert.fail();
        }
    }

    /**
     * Permet de tester plusieurs plugins
     */
    @Requirement("REGARDS_DSL_ING_SSALTO_070")
    @Purpose("A plugin can generate a SIP for all SSALTO's products")
    @Test
    public void createMetadataPlugin_all() {
        try {
            initTestList();
            existResources();
            launchTest();
        } catch (Exception e) {
            LOGGER.error("", e);
            Assert.fail();
        }
    }

    /**
     * Lance le ou les tests
     * @throws ModuleException
     */
    protected void launchTest() throws ModuleException {
        List<String> errorList = new ArrayList<String>();
        List<String> successList = new ArrayList<String>();
        boolean isTestOk = true;

        for (PluginTestDef pluginTestDef : pluginTestDefList) {
            if (!createMetadataPlugIn(pluginTestDef, errorList, successList)) {
                isTestOk = false;
            }
        }

        // liste les fichiers en erreur
        for (String errorProductName : errorList) {
            LOGGER.error(errorProductName);
        }
        // liste les fichier OK
        for (String successProductName : successList) {
            LOGGER.info(successProductName);
        }
        Assert.assertTrue(isTestOk);
    }

    /**
     * Controle l'existence des donnees a tester
     */
    protected void existResources() {
        boolean isFail = false;

        for (PluginTestDef pluginTestDef : pluginTestDefList) {
            // Test if the data file exists
            for (Iterator<String> fileIter = pluginTestDef.getFileNameList().iterator(); fileIter.hasNext()
                    && !isFail;) {
                String fileName = fileIter.next();
                File file = new File(
                        LOCAL_ARCH_DIR + File.separator + pluginTestDef.getFileDirectory() + File.separator + fileName);
                if (!file.exists()) {
                    LOGGER.error("NOT FOUND " + file.getAbsolutePath());
                    isFail = true;
                }
            }

            // Test if the dataset plugin configuration file exists
            File pluginConfFile = new File("plugins/configurations",
                    pluginTestDef.getDataSetName() + CONFIG_FILE_SUFFIX);

            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(pluginConfFile.getPath())) {
                // Try to read the InputStream
                stream.available();
            } catch (IOException e) {
                LOGGER.error("NOT FOUND " + pluginConfFile.getPath());
                isFail = true;
            }

        }
        Assert.assertFalse(isFail);
    }

    /**
     * Creer les descripteurs
     *
     * @param pluginTestDef
     * @param errorList
     * @param sucessList
     * @return
     * @throws ModuleException
     */
    protected boolean createMetadataPlugIn(PluginTestDef pluginTestDef, List<String> errorList, List<String> sucessList)
            throws ModuleException {
        boolean isCreationOk = true;
        List<AcquisitionFile> acqFiles = new ArrayList<>();

        LOGGER.debug("Testing METADATA GENERATION FOR DATASET " + pluginTestDef.getDataSetName());

        ISIPGenerationPluginWithMetadataToolbox plugin = buildPlugin(pluginTestDef.getDataSetName());

        if (pluginTestDef.isMultipleFileTest()) {
            for (String fileName : pluginTestDef.getFileNameList()) {
                File file = new File(
                        LOCAL_ARCH_DIR + File.separator + pluginTestDef.getFileDirectory() + File.separator + fileName);
                if (!fileName.equals("") && file.exists()) {
                    acqFiles.add(initAcquisitionFile(file, pluginTestDef.getProductName()));
                }
            }
            isCreationOk = isCreationOk && createAndValidate(pluginTestDef, plugin, acqFiles);
            if (!isCreationOk) {
                errorList.add("product " + pluginTestDef.getProductName() + " FAILED");
            } else {
                sucessList.add("product " + pluginTestDef.getProductName() + " SUCCESS");
            }
        } else {
            List<File> fileNameList = new ArrayList<>();
            // Check file name list
            if (pluginTestDef.getFileNameList().isEmpty()) {
                // Compute directory
                File testDirectory = new File(LOCAL_ARCH_DIR + File.separator + pluginTestDef.getFileDirectory());
                if (testDirectory.listFiles() == null) {
                    LOGGER.error("Directory doesn't exist : " + LOCAL_ARCH_DIR + File.separator
                            + pluginTestDef.getFileDirectory());
                }
                fileNameList = Arrays.asList(testDirectory.listFiles());
            } else {
                // Compute only specified file(s)
                fileNameList = new ArrayList<File>();
                for (String fileName : pluginTestDef.getFileNameList()) {
                    fileNameList.add(new File(LOCAL_ARCH_DIR + File.separator + pluginTestDef.getFileDirectory()
                            + File.separator + fileName));
                }
            }
            boolean allsucceed = true;
            for (File file : fileNameList) {
                acqFiles = new ArrayList<>();
                if (file.isFile()) {
                    pluginTestDef.setProductName(file.getName());
                    acqFiles.add(initAcquisitionFile(file, pluginTestDef.getProductName()));
                    LOGGER.info("[" + pluginTestDef.getDataSetName() + "] " + file.getName()
                            + " ... processing metadata");
                    isCreationOk = createAndValidate(pluginTestDef, plugin, acqFiles);
                    if (!isCreationOk) {
                        errorList.add("[" + pluginTestDef.getDataSetName() + "] " + file.getName()
                                + " ... ERROR - metadata");
                    } else {
                        sucessList.add("[" + pluginTestDef.getDataSetName() + "] " + file.getName()
                                + " ... SUCCESS - metadata created and validated");
                    }
                    allsucceed = allsucceed && isCreationOk;
                }
            }
            isCreationOk = allsucceed;
        }
        return isCreationOk;
    }

    /**
     * Initialise un fichier a partir duquel est cree un descripteur
     *
     * @param aFile
     * @param productName
     * @return an {@link AcquisitionFile}
     */
    protected AcquisitionFile initAcquisitionFile(File aFile, String productName) {

        Product product = new Product();
        product.setProductName(productName);
        product.setState(ProductState.ACQUIRING);

        AcquisitionFile acqFile = new AcquisitionFile();
        acqFile.setFilePath(aFile.toPath());
        acqFile.setState(AcquisitionFileState.ACQUIRED);
        acqFile.setProduct(product);

        return acqFile;
    }

    /**
     *
     * @param pluginTestDef
     * @param pluginGenerateSIP
     * @param acqFiles
     * @return
     */
    protected boolean createAndValidate(PluginTestDef pluginTestDef,
            ISIPGenerationPluginWithMetadataToolbox pluginGenerateSIP, List<AcquisitionFile> acqFiles) {
        boolean isValidate = false;

        for (AcquisitionFile acqFile : acqFiles) {
            // get the original file from supplyDirectory
            File originalFile = acqFile.getFilePath().toAbsolutePath().toFile();

            // Mise a jour de la date de creation pour les plugins qui l'utilise afin qu'elle corresponde au fichier de
            // référence
            OffsetDateTime odt = OffsetDateTime.of(2009, 11, 9, 16, 59, 41, 0, ZoneOffset.UTC);
            originalFile.setLastModified(1000 * odt.toEpochSecond());
        }

        if (!acqFiles.isEmpty()) {
            try {
                // Get metadata
                String xml = createMetaData(acqFiles, pluginTestDef, pluginGenerateSIP);

                // Create the file descriptor
                File descFile = new File(DESCRIPTOR_DIRECTORY, desc_product_ + pluginTestDef.getProductName() + ".xml");
                writeDescOnDisk(xml, descFile);

                if (validate(descFile)) {
                    LOGGER.debug("XML is valid with dico " + dicoName);
                    isValidate = true;
                }

            } catch (ModuleException e1) {
                LOGGER.error("File in directory does not match the required pattern for dataSet "
                        + pluginTestDef.getDataSetName());
            } catch (Exception e) {
                LOGGER.error("Error occured creating metadata for file", e);
            }
        } else {
            LOGGER.warn("UNABLE TO RUN TEST : NO FILE FOUND FOR DATASET " + pluginTestDef.getDataSetName());
        }
        return isValidate;
    }

    /**
     * Ecriture du fichier sur disque dans <code>DESCRIPTOR_DIRECTORY</code>
     *
     * @param xml
     * @param descFile
     * @throws IOException
     */
    protected void writeDescOnDisk(String xml, File descFile) throws IOException {
        FileWriter writer = new FileWriter(descFile);
        try {
            writer.write(xml);
            LOGGER.debug(descFile.getAbsolutePath() + " has been created");
        } catch (Exception e) {
            LOGGER.error("", e);
            Assert.fail();
        } finally {
            writer.close();
        }
    }

    /**
     * Appel du plugin pour recuperer le descripteur
     *
     * @param acqFiles
     * @param pluginTestDef
     * @param pluginGenerateSIP
     *
     * @return
     *
     * @throws ModuleException
     */
    protected String createMetaData(List<AcquisitionFile> acqFiles, PluginTestDef pluginTestDef,
            ISIPGenerationPluginWithMetadataToolbox pluginGenerateSIP) throws ModuleException {

        SortedMap<Integer, Attribute> attrMaps = pluginGenerateSIP.createMetadataPlugin(acqFiles);

        String xml;
        try {
            Map<File, ?> fileMap = buildMapFile(acqFiles);
            xml = generateXmlDescriptor(acqFiles.get(0).getProduct().getProductName(), fileMap,
                                        pluginTestDef.getDataSetName(), attrMaps);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(e.getMessage());

        }
        LOGGER.debug(xml);

        return xml;
    }

    /**
     * Validation du descripteur retourne par le plugin
     *
     * @param descFile
     * @return
     */
    protected boolean validate(File descFile) {
        boolean isValid = false;
        String xmlString = "";
        try {
            xmlString = FileUtils.readFileToString(descFile);
        } catch (IOException e1) {
            e1.printStackTrace();
            Assert.fail();
        }
        IngestXsdResolver resolver = new IngestXsdResolver();
        resolver.addDictionary(new File(System.getProperty("user.dir") + File.separator + DICO_DIR + File.separator,
                dicoName));
        resolver.addDictionary(new File(System.getProperty("user.dir") + File.separator + DICO_DIR + File.separator,
                dicoBase));
        XMLValidatorFactory validatorFactory = new XMLValidatorFactory(resolver);
        validatorFactory.setXmlSchema(dicoName);

        try {
            XMLValidation validator = (XMLValidation) validatorFactory.makeObject();
            // Validator of the request
            validator.validate(xmlString);
            isValid = true;
        } catch (Exception e) {
            LOGGER.error("", e);
            isValid = false;
        }

        if (isValid) {
            isValid = compare_to_reference(descFile);
        }

        if (!isValid) {
            LOGGER.error("INVALID " + descFile.getAbsolutePath());
        }

        return isValid;
    }

    protected boolean compare_to_reference(File descFile) {
        boolean isEqual = false;

        // recupere fichier de reference
        File refFile = new File(REFERENCES_DESCRIPTOR_DIRECTORY, descFile.getName());
        if (refFile.exists()) {
            isEqual = compareFiles(descFile, refFile);
        } else {
            LOGGER.warn("No reference file " + refFile.getAbsolutePath());
        }
        return isEqual;
    }

    protected Boolean compareFiles(File firstFile, File secondFile) {
        boolean sameFiles = Boolean.FALSE;

        if (firstFile.exists() && firstFile.canRead() && secondFile.exists() && secondFile.canRead()) {
            Diff diff = new Diff();
            try {
                sameFiles = diff.doDiff(firstFile, secondFile);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return sameFiles;
    }

    /**
     * Definit un fichier a traiter contenu dans le repertoire pFileDirectory et associe au dataset pDataSetName
     *
     * @param dataSetName
     * @param fileDirectory
     * @param fileName
     */
    protected void addPluginTestDef(String dataSetName, String fileDirectory, String fileName) {
        pluginTestDefList.add(new PluginTestDef(dataSetName, fileDirectory, fileName));
    }

    protected void addPluginTestDef(String dataSetName, String fileDirectory, List<String> fileList,
            String productName) {
        pluginTestDefList.add(new PluginTestDef(dataSetName, fileDirectory, fileList, productName));
    }

    /**
     * Definit une liste de fichiers a traiter contenus dans le repertoire fileDirectory et associe au dataset
     * dataSetName
     *
     * @param dataSetName
     * @param fileDirectory
     */
    protected void addPluginTestDef(String dataSetName, String fileDirectory) {
        pluginTestDefList.add(new PluginTestDef(dataSetName, fileDirectory));
    }

    /**
     * Get an existing configuration if exists otherwise creates it
     *
     * @return an existing {@link PluginConfiguration}
     *
     * @throws ModuleException if an error occurs
     */
    protected PluginConfiguration getPluginConfiguration(String pluginId, Optional<List<PluginParameter>> parameters)
            throws ModuleException {

        // Get the PluginMetadata
        List<PluginMetaData> metaDatas = pluginService.getPluginsByType(ISIPGenerationPluginWithMetadataToolbox.class);
        // Select right metadata
        java.util.Optional<PluginMetaData> metadata = metaDatas.stream().filter(m -> m.getPluginId().equals(pluginId))
                .findFirst();
        if (!metadata.isPresent()) {
            Assert.fail("Missing plugin metadata for plugin id " + pluginId);
        }

        PluginConfiguration pluginConfiguration = new PluginConfiguration(metadata.get(),
                "Automatic plugin configuration for plugin id : " + pluginId + " (" + UUID.randomUUID().toString()
                        + ")");
        pluginConfiguration.setPluginId(pluginId);
        if (parameters.isPresent()) {
            pluginConfiguration.setParameters(parameters.get());
        }

        return pluginService.savePluginConfiguration(pluginConfiguration);
    }

    private Map<File, ?> buildMapFile(List<AcquisitionFile> acqFiles) {
        Map<File, File> fileMap = new HashMap<>();

        for (AcquisitionFile acqFile : acqFiles) {
            File originalFile = acqFile.getFilePath().toAbsolutePath().toFile();
            if (AcquisitionFileState.ACQUIRED.equals(acqFile.getState())) {
                File newFile = acqFile.getFilePath().toFile();
                fileMap.put(newFile, originalFile);
            }
        }
        return fileMap;
    }

    /**
     * Initialisation des proprietes
     */
    private void loadProperties() {
        Properties properties;
        String propertyFilePath = null;

        LOGGER.info("Create context");

        properties = new Properties();
        propertyFilePath = getProjectProperties();

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(propertyFilePath)) {
            properties.load(stream);
        } catch (IOException e) {
            LOGGER.error("Unable to load file " + propertyFilePath, e);
        }

        dicoName = properties.getProperty("dico");
        dicoBase = properties.getProperty("baseType");
        projectName = properties.getProperty("project");
    }

    private String generateXmlDescriptor(String productName, Map<File, ?> fileMap, String datasetName,
            SortedMap<Integer, Attribute> attributeMap) throws IOException, DescriptorException {
        // add dataObject skeleton bloc
        DataObjectDescriptionElement element = createSkeleton(productName, fileMap, datasetName);

        // now get the attributeMap values to add the attribute ordered into
        // the dataObjectDescriptionElement
        for (Attribute att : attributeMap.values()) {
            element.addAttribute(att);
        }

        loadProperties();

        // init descriptor file
        DescriptorFile descFile = new DescriptorFile();
        descFile.setDicoName(this.dicoName);
        descFile.setProjectName(this.projectName);
        descFile.addDescElementToDocument(element);

        // output the descriptorFile on a physical file
        return writeXmlToString(descFile);
    }

    /**
     * cree le squelette du fichier descripteur contenant les attributs minimums ascendingNode, fileSize, et la liste
     * des object
     *
     * @return un DataObjectDescriptionElement minimum.
     * @param productName
     *            , le nom du produit dont on cree les meta donnees
     * @param fileMap
     *            la liste des fichiers composant le produit
     * @param dataSetName
     *            le nom du dataSet auquel rattacher l'objet de donnees.
     */
    private DataObjectDescriptionElement createSkeleton(String productName, Map<File, ?> fileMap, String dataSetName) {
        DataObjectDescriptionElement element = new DataObjectDescriptionElement();
        element.setAscendingNode(dataSetName);
        element.setDataObjectIdentifier(productName);

        long size = 0;
        for (File file : fileMap.keySet()) {
            size = size + file.length();
            element.addDataStorageObjectIdentifier(file.getName());
        }

        // la taille doit etre au minimum de 1
        long displayedFileSize = Math.max(1, size / 1024);
        element.setFileSize(String.valueOf(displayedFileSize));
        return element;
    }

    /**
     * Ecriture du descripteur
     *
     * @param descFile
     *            Objet descripteur
     *
     * @throws IOException
     */
    private String writeXmlToString(DescriptorFile descFile) throws IOException {

        String xmlString = null;
        // Write the description document to a String
        DocumentImpl descDocumentToWrite = DescriptorFileControler.getDescDocument(descFile);
        if (descDocumentToWrite != null) {
            LOGGER.info("***** Computing FILE xml descriptor");
            StringWriter out = new StringWriter();
            // write the update document to the disk
            OutputFormat format = new OutputFormat(descDocumentToWrite, "UTF-8", true);
            format.setLineWidth(0);
            XMLSerializer output = new XMLSerializer(out, format);
            output.serialize(descDocumentToWrite);
            out.flush();
            out.close();
            xmlString = out.getBuffer().toString();
        } else {
            LOGGER.info("***** DO NOT compute FILE xml descriptor");
        }
        return xmlString;
    }
}
