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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.FileAcquisitionInformations;
import fr.cnes.regards.modules.acquisition.domain.SsaltoFileStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.SupplyDirectory;
import fr.cnes.regards.modules.acquisition.domain.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginsRespositoryProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.Diff;

/**
 * Cette classe permet de simplifier la creation des tests sur les plugins.<br>
 * Pour l'utiliser, il suffit de creer une classe fille et d'implementer les methodes requises par l'interface.
 *
 * @author Christophe Mertz
 */
// TODO CMZ confirmer que peut être enlevé : extend SipadTst
public abstract class AbstractProductMetadataPluginTest extends AbstractRegardsIT implements IProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProductMetadataPluginTest.class);

    @Autowired
    PluginsRespositoryProperties pluginsRespositoryProperties;

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

    // Nom du projet
    protected String projectName = null;

    // Nom du dictionnaire
    protected String dicoName = null;

    // Nom du dictionnaire des types de base
    protected String dicoBase = null;

    // Liste des tests
    protected List<PluginTestDef> pluginTestDefList = new ArrayList<>();

    /**
     * Default constructor
     */
    public AbstractProductMetadataPluginTest() {
        super();
    }

    /**
     * Initialisation des proprietes
     */
    @Before
    public void setUp() {
        Properties properties;
        String propertyFilePath;

        LOGGER.info("Create context");

        properties = new Properties();
        propertyFilePath = getProjectProperties();

        try (InputStream stream = AbstractProductMetadataPluginTest.class.getClassLoader()
                .getResourceAsStream(propertyFilePath)) {
            properties.load(stream);
        } catch (Exception e) {
            LOGGER.error("Unable to load file " + propertyFilePath, e);
            Assert.fail();
        }

        dicoName = properties.getProperty("dico");
        dicoBase = properties.getProperty("baseType");
        projectName = properties.getProperty("project");

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
     */
    protected void launchTest() {
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
     * Teste l'existence des donnees a tester
     */
    protected void existResources() {
        boolean isFail = false;

        for (PluginTestDef pluginTestDef : pluginTestDefList) {
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

            String pluginsConfDir;
            pluginsConfDir = pluginsRespositoryProperties.getPluginConfFilesDir();
            File pluginConfFile = new File(pluginsConfDir, pluginTestDef.getDataSetName() + CONFIG_FILE_SUFFIX);
            if ((pluginConfFile == null) || !pluginConfFile.exists() || !pluginConfFile.canRead()) {
                LOGGER.warn("NOT FOUND " + pluginConfFile.getPath());
                URL confFile = getClass().getResource("tools/" + pluginTestDef.getDataSetName() + CONFIG_FILE_SUFFIX);
                if (confFile == null) {
                    confFile = getClass()
                            .getResource("impl/tools/" + pluginTestDef.getDataSetName() + CONFIG_FILE_SUFFIX);
                    if (confFile == null) {
                        isFail = true;
                        LOGGER.error("NOT FOUND " + "tools/" + pluginTestDef.getDataSetName() + CONFIG_FILE_SUFFIX);
                    }
                }
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
     */
    protected boolean createMetadataPlugIn(PluginTestDef pluginTestDef, List<String> errorList,
            List<String> sucessList) {
        boolean isCreationOk = true;
        List<AcquisitionFile> fileList = new ArrayList<>();

        LOGGER.debug("Testing METADATA GENERATION FOR DATASET " + pluginTestDef.getDataSetName());

        IGenerateSIPPlugin plugin = buildPlugin();

        if (pluginTestDef.isMultipleFileTest()) {
            for (String fileName : pluginTestDef.getFileNameList()) {
                File file = new File(
                        LOCAL_ARCH_DIR + File.separator + pluginTestDef.getFileDirectory() + File.separator + fileName);
                if (!fileName.equals("") && file.exists()) {
                    fileList.add(initSsaltoFile(file));
                }
            }
            isCreationOk = isCreationOk && createAndValidate(pluginTestDef, plugin, fileList);
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
                fileList = new ArrayList<>();
                if (file.isFile()) {
                    fileList.add(initSsaltoFile(file));
                    LOGGER.info("[" + pluginTestDef.getDataSetName() + "] " + file.getName()
                            + " ... processing metadata");
                    pluginTestDef.setProductName(file.getName());
                    isCreationOk = createAndValidate(pluginTestDef, plugin, fileList);
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
     * @return
     */
    protected AcquisitionFile initSsaltoFile(File aFile) {
        AcquisitionFile ssaltoFile = new AcquisitionFile();
        ssaltoFile.setFileName(aFile.getName());
        
        SupplyDirectory dir = new SupplyDirectory();
        dir.setSupplyDir(aFile.getParent());
        
        FileAcquisitionInformations acqInfos = new FileAcquisitionInformations();
        acqInfos.setSupplyDirectory(dir);
        acqInfos.setWorkingDirectory(aFile.getParent());
        acqInfos.setAcquisitionDirectory(aFile.getParent());
        
        ssaltoFile.setAcquisitionInformations(acqInfos);
        ssaltoFile.setStatus(SsaltoFileStatus.VALID);

        // TODO CMZ à confirmer
        // archivingDirectory
        //        FileArchivingInformations archInfos = new FileArchivingInformations();
        //        LocalPhysicalLocation physLoc = new LocalPhysicalLocation();
        //        PhysicalFile physFile = new PhysicalFile();
        //        physFile.setArchivingDirectory(pFile.getParent());
        //        physFile.setFileName(pFile.getName());
        //        physLoc.setPhysicalFile(physFile);
        //        archInfos.setLocalPhysicalLocation(physLoc);
        //        ssaltoFile.setArchivingInformations(archInfos);
        //        ssaltoFile.setStatus(SsaltoFileStatus.VALID);
        //        LocalArchiveAdapter adapter = new LocalArchiveAdapter();
        //        adapter.setProcessWorkingDirectory(System.getProperty("user.dir") + File.separator);
        //        adapter.setDataFolder(System.getProperty("user.dir") + File.separator);
        return ssaltoFile;
    }

    /**
     *
     * @param pluginTestDef
     * @param pluginGenerateSIP
     * @param fileList
     * @return
     */
    protected boolean createAndValidate(PluginTestDef pluginTestDef, IGenerateSIPPlugin pluginGenerateSIP,
            List<AcquisitionFile> fileList) {
        boolean isValidate = false;
        Map<File, File> fileMap = new HashMap<>();

        for (AcquisitionFile acqFile : fileList) {
            // get the original file from supplyDirectory
            File originalFile = new File(acqFile.getAcquisitionInformations().getAcquisitionDirectory(),
                    acqFile.getFileName());
            // Mise a jour de la date de creation pour les plugins qui l'utilise afin qu'elle corresponde au fichier de
            // référence
            originalFile.setLastModified(1257785981000L);
            if (acqFile.getStatus().equals(SsaltoFileStatus.VALID)) {
                File newFile = new File(acqFile.getAcquisitionInformations().getWorkingDirectory(),
                        acqFile.getFileName());
                fileMap.put(newFile, originalFile);
            }
            // TODO CMZ à confirmer que la condition du if est toujours VRAI
            //            else if ((ssaltoFile.getStatus().equals(SsaltoFileStatus.ACQUIRED))
            //                    || (ssaltoFile.getStatus().equals(SsaltoFileStatus.TO_ARCHIVE))
            //                    || (ssaltoFile.getStatus().equals(SsaltoFileStatus.ARCHIVED))
            //                    || (ssaltoFile.getStatus().equals(SsaltoFileStatus.TAR_CURRENT))
            //                    || (ssaltoFile.getStatus().equals(SsaltoFileStatus.IN_CATALOGUE))) {
            //                File newFile = new File(
            //                        LocalArchive.getInstance().getDataFolder() + "/" + ssaltoFile.getArchivingInformations()
            //                                .getLocalPhysicalLocation().getPhysicalFile().getArchivingDirectory(),
            //                        ssaltoFile.getFileName());
            //                fileMap.put(newFile, originalFile);
            //            }

        }

        if (!fileMap.isEmpty()) {
            try {
                String xml = createMetaData(pluginTestDef, pluginGenerateSIP, fileMap);
                File descFile = new File(DESCRIPTOR_DIRECTORY, desc_product_ + pluginTestDef.getProductName() + ".xml");
                writeDescOnDisk(xml, descFile);

                // TODO CMZ à remettre
                //                if (validate(descFile)) {
                //                    LOGGER.debug("XML is valid with dico " + dicoName);
                //                    isValidate = true;
                //
                //                } else {
                //                    // descFile.renameTo(new File("ERROR_" +
                //                    // descFile.getName()));
                //                    isValidate = false;
                //                }

            } catch (ModuleException e1) {
                // TODO CMZ à remettre
                //                if ((e1.getErrorCode() != null) && e1.getErrorCode().equals("NO_FILE")) {
                //                    LOGGER.error("CONF FILE " + pluginTestDef.getDataSetName() + "_PluginConfiguration.xml NOT FOUND");
                //                }
                LOGGER.error("File in directory does not match the required pattern for dataSet "
                        + pluginTestDef.getDataSetName());
                isValidate = false;

            } catch (Exception e) {
                LOGGER.error("Error occured creating metadata for file", e);
                isValidate = false;
            }
        } else {
            LOGGER.warn("UNABLE TO RUN TEST : NO FILE FOUND FOR DATASET " + pluginTestDef.getDataSetName());
            isValidate = false;
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
     * @param pluginTestDef
     * @param pluginGenerateSIP
     * @param pFileMap
     * @return
     * @throws ModuleException
     */
    protected String createMetaData(PluginTestDef pluginTestDef, IGenerateSIPPlugin pluginGenerateSIP,
            Map<File, File> pFileMap) throws ModuleException {
        String xml;
        if (pluginTestDef.isMultipleFileTest()) {
            xml = pluginGenerateSIP.createMetadataPlugin(pluginTestDef.getProductName(), pFileMap,
                                                         pluginTestDef.getDataSetName(), dicoName, projectName);
        } else {
            // there is only one File in FileList.
            String productName = pFileMap.keySet().iterator().next().getName();
            xml = pluginGenerateSIP.createMetadataPlugin(productName, pFileMap, pluginTestDef.getDataSetName(),
                                                         dicoName, projectName);
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
    // TODO CMZ à remettre
    //    protected boolean validate(File pdescFile) {
    //        boolean isValid = false;
    //        String xmlString = "";
    //        try {
    //            xmlString = FileUtils.readFileToString(pdescFile);
    //        } catch (IOException e1) {
    //            e1.printStackTrace();
    //            Assert.fail();
    //        }
    //        IngestXsdResolver resolver = new IngestXsdResolver();
    //        resolver.addDictionary(new File(System.getProperty("user.dir") + File.separator + DICO_DIR + File.separator,
    //                dicoName));
    //        resolver.addDictionary(new File(System.getProperty("user.dir") + File.separator + DICO_DIR + File.separator,
    //                dicoBase));
    //        XMLValidatorFactory validatorFactory = new XMLValidatorFactory(resolver);
    //        validatorFactory.setXmlSchema(dicoName);
    //        try {
    //            XMLValidation validator = (XMLValidation) validatorFactory.makeObject();
    //            // Validator of the request
    //            validator.validate(xmlString);
    //            isValid = true;
    //        } catch (Exception e) {
    //            LOGGER.error("", e);
    //            isValid = false;
    //        }
    //        if (isValid) {
    //            isValid = compare_to_reference(pdescFile);
    //        }
    //        if (!isValid) {
    //            LOGGER.error("INVALID " + pdescFile.getAbsolutePath());
    //        }
    //        return isValid;
    //    }

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
     * Definit une liste de fichiers a traiter contenus dans le repertoire pFileDirectory et associe au dataset pDataSetName
     *
     * @param dataSetName
     * @param fileDirectory
     */
    protected void addPluginTestDef(String dataSetName, String fileDirectory) {
        pluginTestDefList.add(new PluginTestDef(dataSetName, fileDirectory));
    }

}
