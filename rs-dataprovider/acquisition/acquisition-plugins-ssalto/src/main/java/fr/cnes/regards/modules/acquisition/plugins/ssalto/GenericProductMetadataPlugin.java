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
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectDescriptionElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorFile;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.finder.AttributeFinder;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.repository.PluginsRespositoryProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.PluginConfigurationProperties;

/**
 * PlugIn generic de creation de metadonnees d'un produit. Cette classe possède une specification pour chaque produit.
 *
 * @author CS
 * @since 1.2
 */
public abstract class GenericProductMetadataPlugin implements IGenerateSIPPlugin {

    /**
     * Nom du fichier de configuration des plugins
     */
    private static final String CONFIG_FILE_SUFFIX = "_PluginConfiguration.xml";

    private static final String RULE_FILE = "tools/pluginFinderDigesterRules.xml";

    private static final String ATTRIBUTE_ORDER_PROP_FILE = "/ssalto/domain/plugins/impl/tools/attributeOrder.properties";

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericProductMetadataPlugin.class);

    /**
     * map contenant pour chaque attribut la valeur correspondante.
     *
     * @since 1.2
     */
    protected Map<String, List<? extends Object>> attributeValueMap_;

    /**
     * map qui permet d'ordonner les attributs dans le fichier descripteur
     *
     * @since 1.2
     */
    protected java.util.Properties attributeOrderProperties_;

    /**
     * proprietes contenant la configuration du plugin, notamment, la liste des finder a utiliser pour chaque attribut.
     */
    @Autowired
    protected PluginConfigurationProperties pluginConfProperties;

    @Autowired
    protected PluginsRespositoryProperties pluginsRespositoryProperties;

    /**
     * cree le squelette du fichier descripteur contenant les attributs minimums ascendingNode, fileSize, et la liste
     * des object
     *
     * @return un DataObjectDescriptionElement minimum.
     * @param pProductName
     *            , le nom du produit dont on cree les meta donnees
     * @param pFileMap
     *            la liste des fichiers composant le produit
     * @param pDataSetName
     *            le nom du dataSet auquel rattacher l'objet de donnees.
     * @since 1.2
     */
    public DataObjectDescriptionElement createSkeleton(String pProductName, Map<File, ?> pFileMap,
            String pDataSetName) {
        DataObjectDescriptionElement element = new DataObjectDescriptionElement();
        element.setAscendingNode(pDataSetName);
        element.setDataObjectIdentifier(pProductName);
        long size = 0;
        for (File file : pFileMap.keySet()) {
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
     * @param pDataSetName
     * @throws SsaltoDomainException
     *             en cas d'erreur du parsing du fichier de configuration ou de la lecture du fichier d'ordonnancement
     *             des attributs.
     * @since 1.2
     * @FA SIPNG-FA-0535-CN : ajout d'un test sur le fichier de conf
     */
    public void init(String pDataSetName) throws ModuleException {
        // parse the configuration file to set pluginConfProperties
        // Get the path to the digester rules file
        URL ruleFile = getClass().getResource(RULE_FILE);

        if (ruleFile == null) {
            String msg = "unable to load the rule file " + RULE_FILE;
            LOGGER.error(msg);
            throw new ModuleException(msg);
        }

        // Getting conf file from project configured directory
        String pluginConfDirectory = pluginsRespositoryProperties.getPluginConfFilesDir();
        File pluginConfFile = new File(pluginConfDirectory, pDataSetName + CONFIG_FILE_SUFFIX);
        URL confFile = null;

        // If conf file doesn't exists in the project configuration directory, check in the classpath
        if ((pluginConfFile == null) || !pluginConfFile.exists() || !pluginConfFile.canRead()) {
            String msg = "unable to load the conf file " + pluginConfFile.getPath() + ", checking in classpath ...";
            LOGGER.warn(msg);
            confFile = getClass().getResource("tools/" + pDataSetName + CONFIG_FILE_SUFFIX);
            if (confFile == null) {
                msg = "unable to load the conf file " + "tools/" + pDataSetName + CONFIG_FILE_SUFFIX;
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
            String msg = "unable to parse file " + pDataSetName + CONFIG_FILE_SUFFIX + " using rule file " + RULE_FILE;
            LOGGER.error(msg, e);
            throw new ModuleException(e);
        }

        // init the attributeOrderMap_ from the attributeOrderConfigurationFile.
        attributeOrderProperties_ = new Properties();
        try {
            attributeOrderProperties_.load(ATTRIBUTE_ORDER_PROP_FILE);

        } catch (FileException e) {
            String message = "unable to load property file" + ATTRIBUTE_ORDER_PROP_FILE;
            LOGGER.error(message, e);
            throw new ModuleException(message);
        }

    }

    protected abstract String getProjectName();

    /**
     * Ecriture du descripteur
     *
     * @param pTargetFile
     *            Fichier physique dans lequel ecrire
     * @param pDescFile
     *            Objet descripteur
     * @throws IOException
     * @since 1.2
     * @FA SIPNG-FA-0400-CN : ajout de code
     */
    private String writeXmlToString(DescriptorFile pDescFile) throws IOException {

        String xmlString = null;
        // Write the description document to a String
        DocumentImpl descDocumentToWrite = DescriptorFileControler.getDescDocument(pDescFile);
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
     * Methode surchargee
     *
     * @see ssalto.domain.plugins.decl.ICreateProductMetadataPlugin#createMetadataPlugin(String, Map, String, String,
     *      String)
     * @since 1.2
     */
    @Override
    public String createMetadataPlugin(String pProductName, Map<File, ?> pFileMap, String pDatasetName,
            String pDicoName, String pProjectName) throws ModuleException {
        String outputXml = null;
        init(pDatasetName);
        // init descriptor file
        DescriptorFile descFile = new DescriptorFile();
        descFile.setDicoName(pDicoName);
        descFile.setProjectName(pProjectName);

        // add dataObject skeleton bloc
        DataObjectDescriptionElement element = createSkeleton(pProductName, pFileMap, pDatasetName);

        // add attribute from attribute finders
        SortedMap<Integer, Attribute> attributeMap = new TreeMap<>();
        attributeValueMap_ = new HashMap<>();
        // find all attributeValue and add each one into attributeMap
        try {
            // first do the specific attributes not depending from other
            // attribute value
            // or should be available for finders
            doCreateIndependantSpecificAttributes(pFileMap, attributeMap);

            if (pluginConfProperties.getFinderList() != null) {
                Collection<AttributeFinder> finderList = pluginConfProperties.getFinderList();
                for (AttributeFinder finder : finderList) {
                    finder.setAttributProperties(pluginConfProperties);
                    Attribute attribute = finder.buildAttribute(pFileMap, attributeValueMap_);
                    registerAttribute(finder.getName(), attributeMap, attribute);
                }
            }

            // then do specific attributs which can depend on other attribute
            // value
            doCreateDependantSpecificAttributes(pFileMap, attributeMap);

            // now get the attributeMap values to add the attribute ordered into
            // the dataObjectDescriptionElement
            for (Attribute att : attributeMap.values()) {
                element.addAttribute(att);
            }

            descFile.addDescElementToDocument(element);
            // output the descriptorFile on a physical file.

            outputXml = writeXmlToString(descFile);
        } catch (IOException e) {
            // "Error writing xml"
            throw new ModuleException(e);
        } catch (PluginAcquisitionException e1) {
            LOGGER.error("", e1);
            // TODO CMZ
            // "Error building metadata"
            throw new ModuleException(e1.toString());
        }
        return outputXml;
    }

    /**
     * enregistre l'attribut dans la map des attributs
     *
     * @param pAttributeMap
     *            la map des attributs
     * @param pAttribute
     *            l'attribut a enregistrer.
     * @since 1.0
     */
    protected void registerAttribute(String pName, Map<Integer, Attribute> pAttributeMap, Attribute pAttribute) {

        pAttributeMap.put(new Integer(attributeOrderProperties_.getProperty(pName)), pAttribute);
    }

    /**
     * permet d'ajouter d'autres attributs que ceux definit dans le fichier de configuration
     *
     * @param pAttributeMap
     * @since 1.0
     */
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {
        // DO NOTHING
    }

    /**
     * permet d'ajouter d'autres attributs que ceux definit dans le fichier de configuration
     *
     * @param pAttributeMap
     * @since 1.0
     */
    protected void doCreateDependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws ModuleException {
        // DO NOTHING
    }

    public String generateXml(File pFile, String pProjectName, String pDicoName, String pDataSetId)
            throws ModuleException {
        // TODO CMZ à confirmer
        return null;
    }

    // Getters and Setters

    protected Properties getAttributeOrderProperties() {
        return attributeOrderProperties_;
    }

    protected Map<String, List<? extends Object>> getAttributeValueMap() {
        return attributeValueMap_;
    }

    protected PluginConfigurationProperties getProperties() {
        return pluginConfProperties;
    }
}
