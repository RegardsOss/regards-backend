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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectDescriptionElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorFile;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers.DescriptorFileControler;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.finder.AttributeFinder;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.properties.PluginsRepositoryProperties;

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
    public DataObjectDescriptionElement createSkeleton(String productName, Map<File, ?> fileMap, String dataSetName) {
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
     * parse le fichier de configuration pour remplir les properties, et initialise la map de l'ordre des attributs.
     *
     * @param dataSetName
     * @throws ModuleException
     *             en cas d'erreur du parsing du fichier de configuration ou de la lecture du fichier d'ordonnancement
     *             des attributs.
     */
    public void loadDataSetConfiguration(String dataSetName) throws ModuleException {
        // Get the path to the digester rules file
        URL ruleFile = AbstractProductMetadataPlugin.class.getClassLoader().getResource(RULE_FILE);
        if (ruleFile == null) {
            String msg = "unable to load the rule file " + RULE_FILE;
            LOGGER.error(msg);
            throw new ModuleException(msg);
        }

        // Getting conf file from project configured directory
        String pluginConfDirectory = getPluginsRepositoryProperties().getPluginConfFilesDir();
        //        String pluginConfDirectory = pluginsRepositoryProperties.getPluginConfFilesDir();
        File pluginConfFile = new File(pluginConfDirectory, dataSetName + CONFIG_FILE_SUFFIX);
        URL confFile = null;

        // If conf file doesn't exists in the project configuration directory, check in the classpath
        if ((pluginConfFile == null) || !pluginConfFile.exists() || !pluginConfFile.canRead()) {

            if (pluginConfFile != null) {
                String msg = "unable to load the conf file " + pluginConfFile.getPath() + ", checking in classpath ...";
                LOGGER.warn(msg);
            }

            confFile = getClass().getResource("tools/" + dataSetName + CONFIG_FILE_SUFFIX);
            if (confFile == null) {
                String msg = "unable to load the conf file " + "tools/" + dataSetName + CONFIG_FILE_SUFFIX;
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

            // create instance of digester that can handle the digester rule
            // file
            Digester digester = DigesterLoader.createDigester(ruleFile);
            // Process the input file.
            pluginConfProperties = (PluginConfigurationProperties) digester.parse(in);
            pluginConfProperties.setProject(getProjectName());
        } catch (Exception e) {
            String msg = "unable to parse file " + dataSetName + CONFIG_FILE_SUFFIX + " using rule file " + RULE_FILE;
            LOGGER.error(msg, e);
            throw new ModuleException(e);
        }

        // init the attributeOrderMap_ from the attributeOrderConfigurationFile
        attributeOrderProperties = new Properties();
        try (InputStream stream = AbstractProductMetadataPlugin.class.getClassLoader()
                .getResourceAsStream(ATTRIBUTE_ORDER_PROP_FILE)) {
            attributeOrderProperties.load(stream);
        } catch (Exception e) {
            String message = "unable to load property file" + ATTRIBUTE_ORDER_PROP_FILE;
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(message);
        }

    }

    protected abstract String getProjectName();

    protected abstract PluginsRepositoryProperties getPluginsRepositoryProperties();

    /**
     * Ecriture du descripteur
     *
     * @param pTargetFile
     *            Fichier physique dans lequel ecrire
     * @param descFile
     *            Objet descripteur
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

    /**
     * cree les meta donnees pour le produit pProductName, les fichier pFileList et le jeux de donnees pDataSetName
     */
    @Override
    public String createMetadataPlugin(String productName, Map<File, ?> fileMap, String datasetName, String dicoName,
            String projectName) throws ModuleException {
        String outputXml = null;
        SortedMap<Integer, Attribute> attributeMap = new TreeMap<>();

        loadDataSetConfiguration(datasetName);

        // add dataObject skeleton bloc
        DataObjectDescriptionElement element = createSkeleton(productName, fileMap, datasetName);

        // add attribute from attribute finders
        attributeValueMap = new HashMap<>();

        // find all attributeValue and add each one into attributeMap
        try {
            // first do the specific attributes not depending from other
            // attribute value
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

            // now get the attributeMap values to add the attribute ordered into
            // the dataObjectDescriptionElement
            for (Attribute att : attributeMap.values()) {
                element.addAttribute(att);
            }

            // init descriptor file
            DescriptorFile descFile = new DescriptorFile();
            descFile.setDicoName(dicoName);
            descFile.setProjectName(projectName);
            descFile.addDescElementToDocument(element);

            // output the descriptorFile on a physical file
            outputXml = writeXmlToString(descFile);

        } catch (IOException e) {
            // "Error writing xml"
            throw new ModuleException(e);
        } catch (PluginAcquisitionException e1) {
            LOGGER.error(e1.getMessage(), e1);
            throw new ModuleException(e1.toString());
        }
        return outputXml;
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

        attributeMap.put(new Integer(attributeOrderProperties.getProperty(attrName)), attribute);
    }

    /**
     * permet d'ajouter d'autres attributs que ceux definit dans le fichier de configuration
     *
     * @param attributeMap
     */
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws PluginAcquisitionException {
        // DO NOTHING
    }

    /**
     * permet d'ajouter d'autres attributs que ceux definit dans le fichier de configuration
     *
     * @param attributeMap
     */
    protected void doCreateDependantSpecificAttributes(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws ModuleException {
        // DO NOTHING
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
}
