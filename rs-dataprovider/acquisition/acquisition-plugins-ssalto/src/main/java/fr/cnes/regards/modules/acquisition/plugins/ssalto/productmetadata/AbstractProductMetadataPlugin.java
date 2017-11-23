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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.finder.AttributeFinder;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginsRepositoryProperties;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * PlugIn generic de creation de metadonnees d'un produit. Cette classe possède une specification pour chaque produit.
 *
 * @author Christophe Mertz
 */
public abstract class AbstractProductMetadataPlugin implements IGenerateSIPPlugin {

    /**
     * Nom du fichier de configuration des plugins
     */
    private static final String CONFIG_FILE_SUFFIX = "_PluginConfiguration.xml";

    private static final String RULE_FILE = "ssalto/domain/plugins/impl/tools/pluginFinderDigesterRules.xml";

    private static final String ATTRIBUTE_ORDER_PROP_FILE = "ssalto/domain/plugins/impl/tools/attributeOrder.properties";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProductMetadataPlugin.class);

    /**
     * map contenant pour chaque attribut la valeur correspondante
     */
    protected Map<String, List<? extends Object>> attributeValueMap;

    /**
     * map qui permet d'ordonner les attributs dans le fichier descripteur
     */
    protected Properties attributeOrderProperties;

    /**
     * proprietes contenant la configuration du plugin, notamment, la liste des finder a utiliser pour chaque attribut.
     */
    protected PluginConfigurationProperties pluginConfProperties;

    protected abstract String getProjectName();

    protected abstract PluginsRepositoryProperties getPluginsRepositoryProperties();

    protected abstract String getProjectProperties();

    /**
     * cree les attributs pour le {@link Product} contenant la liste des {@link AcquisitionFile}, pour le jeu de donnees dataSetName
     */
    @Override
    public SortedMap<Integer, Attribute> createMetadataPlugin(List<AcquisitionFile> acqFiles,
            Optional<String> datasetName) throws ModuleException {
        SortedMap<Integer, Attribute> attributeMap = new TreeMap<>();

        if (!datasetName.isPresent()) {
            ModuleException ex = new ModuleException("The dataset name is required");
            LOGGER.error(ex.getMessage());
            throw ex;
        }

        Map<File, ?> fileMap = buildMapFile(acqFiles);

        loadDataSetConfiguration(datasetName.get());

        // add attribute from attribute finders
        attributeValueMap = new HashMap<>();

        // find all attributeValue and add each one into attributeMap
        // first do the specific attributes not depending from other attribute value
        // or should be available for finders
        doCreateIndependantSpecificAttributes(fileMap, attributeMap);

        if (pluginConfProperties.getFinderList() != null) {
            Collection<AttributeFinder> finderList = pluginConfProperties.getFinderList();
            for (AttributeFinder finder : finderList) {
                finder.setAttributProperties(pluginConfProperties);
                Attribute attribute = finder.buildAttribute(fileMap, attributeValueMap);
                registerAttribute(finder.getName(), attributeMap, attribute);
            }
        }

        // then do specific attributs which can depend on other attribute value
        doCreateDependantSpecificAttributes(fileMap, attributeMap);

        return attributeMap;
    }

//    public SortedMap<Integer, Attribute> createMetaDataPlugin(List<AcquisitionFile> acqFiles) throws ModuleException {
//        return new TreeMap<>();
//    }

    @Override
    public SIP runPlugin(List<AcquisitionFile> acqFiles, Optional<String> datasetIpId) throws ModuleException {
        // TODO CMZ createMetaDataPlugin à compléter

        return null;
    }

    /**
     * parse le fichier de configuration pour remplir les properties, et initialise la map de l'ordre des attributs.
     *
     * @param dataSetName
     * @throws ModuleException
     *             en cas d'erreur du parsing du fichier de configuration ou de la lecture du fichier d'ordonnancement
     *             des attributs.
     */
    protected void loadDataSetConfiguration(String dataSetName) throws ModuleException {
        // Get the path to the digester rules file
        URL ruleFile = getClass().getClassLoader().getResource(RULE_FILE);
        if (ruleFile == null) {
            String msg = "unable to load the rule file " + RULE_FILE;
            LOGGER.error(msg);
            throw new ModuleException(msg);
        }

        // Getting conf file from project configured directory
        String pluginConfDirectory = getPluginsRepositoryProperties().getPluginConfFilesDir();
        // TODO CMZ à virer : String pluginConfDirectory = pluginsRepositoryProperties.getPluginConfFilesDir();
        File pluginConfFile = new File(pluginConfDirectory, dataSetName + CONFIG_FILE_SUFFIX);
        URL confFile = null;

        if (!pluginConfFile.exists() || !pluginConfFile.canRead()) {
            // If conf file doesn't exists in the project configuration directory, check in the classpath

            String msg = "unable to load the conf file " + pluginConfFile.getPath() + ", checking in classpath ...";
            LOGGER.warn(msg);

            confFile = getClass().getResource("tools/" + dataSetName + CONFIG_FILE_SUFFIX);
            if (confFile == null) {
                msg = "unable to load the conf file " + "tools/" + dataSetName + CONFIG_FILE_SUFFIX;
                LOGGER.error(msg);
                throw new ModuleException(msg);
            }
        } else {
            try {
                confFile = pluginConfFile.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new ModuleException(e.getMessage());
            }
        }

        // The first action is to test if the property file exists
        try (InputStream in = confFile.openStream()) {
            // create instance of digester that can handle the digester rule file
            Digester digester = DigesterLoader.createDigester(ruleFile);
            // Process the input file.
            pluginConfProperties = (PluginConfigurationProperties) digester.parse(in);
            pluginConfProperties.setProject(getProjectName());
        } catch (IOException | SAXException e) {
            String msg = "unable to parse file " + dataSetName + CONFIG_FILE_SUFFIX + " using rule file " + RULE_FILE;
            LOGGER.error(msg, e);
            throw new ModuleException(e);
        }

        // init the attributeOrderMap_ from the attributeOrderConfigurationFile
        attributeOrderProperties = new Properties();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(ATTRIBUTE_ORDER_PROP_FILE)) {
            attributeOrderProperties.load(stream);
        } catch (IOException e) {
            String message = "unable to load property file" + ATTRIBUTE_ORDER_PROP_FILE;
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(message);
        }

    }

    /**
     * enregistre l'attribut dans la map des attributs
     *
     * @param attributeMap
     *            la map des attributs
     * @param attribute
     *            l'attribut a enregistrer.
     */
    protected void registerAttribute(String attrName, Map<Integer, Attribute> attributeMap, Attribute attribute) {
        attributeMap.put(Integer.valueOf(attributeOrderProperties.getProperty(attrName)), attribute);
    }

    /**
     * permet d'ajouter d'autres attributs que ceux definit dans le fichier de configuration
     *
     * @param attributeMap
     */
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws PluginAcquisitionException { // NOSONAR
    }

    /**
     * permet d'ajouter d'autres attributs que ceux definit dans le fichier de configuration
     *
     * @param attributeMap
     */
    protected void doCreateDependantSpecificAttributes(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws ModuleException { // NOSONAR
    }

    protected Properties getAttributeOrderProperties() {
        return attributeOrderProperties;
    }

    protected Map<String, List<? extends Object>> getAttributeValueMap() {
        return attributeValueMap;
    }

    protected PluginConfigurationProperties getProperties() {
        return pluginConfProperties;
    }

    private Map<File, ?> buildMapFile(List<AcquisitionFile> acqFiles) {
        Map<File, File> fileMap = new HashMap<>();

        for (AcquisitionFile acqFile : acqFiles) {
            // get the original file from supplyDirectory
            File originalFile = new File(acqFile.getAcquisitionInformations().getAcquisitionDirectory(),
                    acqFile.getFileName());

            if (acqFile.getStatus().equals(AcquisitionFileStatus.VALID)) {
                File newFile = new File(acqFile.getAcquisitionInformations().getWorkingDirectory(),
                        acqFile.getFileName());
                fileMap.put(newFile, originalFile);
            }

            // TODO CMZ à confirmer que la condition du if est toujours VRAI
            //            else if ((ssaltoFile.getStatus().equals(AcquisitionFileStatus.ACQUIRED))
            //                    || (ssaltoFile.getStatus().equals(AcquisitionFileStatus.TO_ARCHIVE))
            //                    || (ssaltoFile.getStatus().equals(AcquisitionFileStatus.ARCHIVED))
            //                    || (ssaltoFile.getStatus().equals(AcquisitionFileStatus.TAR_CURRENT))
            //                    || (ssaltoFile.getStatus().equals(AcquisitionFileStatus.IN_CATALOGUE))) {
            //                File newFile = new File(
            //                        LocalArchive.getInstance().getDataFolder() + "/" + ssaltoFile.getArchivingInformations()
            //                                .getLocalPhysicalLocation().getPhysicalFile().getArchivingDirectory(),
            //                        ssaltoFile.getFileName());
            //                fileMap.put(newFile, originalFile);
            //            }
        }

        return fileMap;
    }

}
