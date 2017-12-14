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
import java.time.OffsetDateTime;
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
import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;
import fr.cnes.regards.framework.geojson.coordinates.Position;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.finder.AttributeFinder;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginsRepositoryProperties;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.plugins.AbstractGenerateSIPPlugin;
import fr.cnes.regards.modules.entities.domain.geometry.Geometry;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * Abstract class for {@link Plugin} of metadata {@link Product}.
 * 
 * @author Christophe Mertz
 */
public abstract class AbstractProductMetadataPlugin extends AbstractGenerateSIPPlugin implements IGenerateSIPPlugin {

    /**
     * Nom du fichier de configuration des plugins
     */
    private static final String CONFIG_FILE_SUFFIX = "_PluginConfiguration.xml";

    private static final String RULE_FILE = "ssalto/domain/plugins/impl/tools/pluginFinderDigesterRules.xml";

    private static final String ATTRIBUTE_ORDER_PROP_FILE = "ssalto/domain/plugins/impl/tools/attributeOrder.properties";

    protected static final String GEO_COORDINATES = "GEO_COORDINATES";

    protected static final String LONGITUDE_MIN = "LONGITUDE_MIN";

    protected static final String LONGITUDE_MAX = "LONGITUDE_MAX";

    protected static final String LATITUDE_MIN = "LATITUDE_MIN";

    protected static final String LATITUDE_MAX = "LATITUDE_MAX";

    protected static final String RANGE = "RANGE";

    protected static final String TIME_PERIOD = "TIME_PERIOD";

    protected static final String START_DATE = "START_DATE";

    protected static final String STOP_DATE = "STOP_DATE";

    protected static final String MISSION = "mission";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProductMetadataPlugin.class);

    /**
     * {@link Map} contenant pour chaque attribut la valeur correspondante
     */
    protected Map<String, List<? extends Object>> attributeValueMap;

    /**
     * {@link Map} qui permet d'ordonner les attributs dans le fichier descripteur
     */
    protected Properties attributeOrderProperties;

    /**
     * proprietes contenant la configuration du plugin, notamment, la liste des finder a utiliser pour chaque attribut
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
                registerAttribute(attributeMap, finder.getName(), attribute);
            }
        }

        // then do specific attributes which can depend on other attribute value
        doCreateDependantSpecificAttributes(fileMap, attributeMap);

        // log the calculated attributes
        LOGGER.info("[{}] {} attributes calcultated for {} AcquisitionFile", datasetName.get(), attributeMap.size(),
                    acqFiles.size());
        if (LOGGER.isDebugEnabled()) {
            for (Attribute att : attributeMap.values()) {
                LOGGER.debug(att.toString());
            }
        }

        return attributeMap;
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
     * @param attributeMap la map des attributs
     * @param attrName le nom de l'attribute
     * @param attribute l'attribute à enregister
     */
    protected void registerAttribute(Map<Integer, Attribute> attributeMap, String attrName, Attribute attribute) {
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
            File originalFile = new File(acqFile.getAcquisitionInformations().getAcquisitionDirectory(),
                    acqFile.getFileName());

            if (acqFile.getStatus().equals(AcquisitionFileStatus.VALID)) {
                File newFile = acqFile.getFile();
                fileMap.put(newFile, originalFile);
            }

        }

        return fileMap;
    }

    @Override
    protected void addAttributesTopSip(SIPBuilder sipBuilder, SortedMap<Integer, Attribute> mapAttrs)
            throws AcquisitionException {

        sipBuilder.getPDIBuilder().setFacility("CNES");
        sipBuilder.addDescriptiveInformation(Strings.toLowerCase(MISSION), getProjectName());

        for (Attribute att : mapAttrs.values()) {

            if (att.getMetaAttribute().getValueType().equals(AttributeTypeEnum.TYPE_STRING)
                    && att.getClass().equals(CompositeAttribute.class)) {
                // CompositeAttribute
                addSip(sipBuilder, (CompositeAttribute) att);
            } else {
                addSip(sipBuilder, att);
            }

        }
    }

    /**
     * Add descriptive information to the {@link SIPBuilder} for a {@link CompositeAttribute}
     * @param sipBuilder the current {@link SIPBuilder}
     * @param compAttr a {@link CompositeAttribute} 
     */
    private void addSip(SIPBuilder sipBuilder, CompositeAttribute compAttr) {
        LOGGER.debug("build SIP : add composite attribute [{}]", compAttr.getName());
        if (compAttr.getName().equals(GEO_COORDINATES)) {
            addSipGeo(sipBuilder, compAttr);
        } else if (compAttr.getName().endsWith(RANGE)) {
            addSipRange(sipBuilder, compAttr);

        } else if (compAttr.getName().endsWith(TIME_PERIOD)) {
            addSipTime(sipBuilder, compAttr);

        } else {
            for (Attribute attr : compAttr.getAttributeList()) {
                LOGGER.debug("attribute {}", attr.getMetaAttribute().getName());
                addSip(sipBuilder, attr);
            }
        }

    }

    /**
     * Add descriptive information of a range values to the {@link SIPBuilder}
     * @param sipBuilder the current {@link SIPBuilder}
     * @param compAttr a {@link CompositeAttribute} 
     */
    private void addSipRange(SIPBuilder sipBuilder, CompositeAttribute compAttr) {
        LOGGER.debug("build SIP : add Range [{}]", compAttr.getName());
        JsonObject jsonRange = new JsonObject();

        for (Attribute attr : compAttr.getAttributeList()) {
            LOGGER.debug("build SIP : attribute [{}]", attr.getMetaAttribute().getName());
            Object objValue = attr.getValueList().get(0);
            jsonRange.addProperty(Strings.toLowerCase(attr.getMetaAttribute().getName()), objValue.toString());
        }
        sipBuilder.addDescriptiveInformation(Strings.toLowerCase(compAttr.getName()), jsonRange);
    }

    /**
     * Add descriptive information of time period to the {@link SIPBuilder}
     * @param sipBuilder the current {@link SIPBuilder}
     * @param compAttr a {@link CompositeAttribute}
     */
    private void addSipTime(SIPBuilder sipBuilder, CompositeAttribute compAttr) {
        LOGGER.debug("build SIP : add Time [{}]", compAttr.getName());
        JsonObject jsonStartStop = new JsonObject();

        for (Attribute attr : compAttr.getAttributeList()) {
            LOGGER.debug("build SIP : attribute [{}]", attr.getMetaAttribute().getName());
            OffsetDateTime ofDate = (OffsetDateTime) attr.getValueList().get(0);
            jsonStartStop.addProperty(Strings.toLowerCase(attr.getMetaAttribute().getName()), ofDate.toString());
        }

        sipBuilder.addDescriptiveInformation(Strings.toLowerCase(compAttr.getName()), jsonStartStop);
    }

    /**
     * Add a {@link Geometry} information to the {@link SIPBuilder}
     * @param sipBuilder the current {@link SIPBuilder}
     * @param compAttr a {@link CompositeAttribute}
     */
    private void addSipGeo(SIPBuilder sipBuilder, CompositeAttribute compAttr) {
        LOGGER.debug("build SIP : add Geo [{}]", compAttr.getName());

        Double latMin = null;
        Double lonMin = null;
        Double latMax = null;
        Double lonMax = null;

        LOGGER.debug("build SIP : add Geo [{}]", compAttr.getName());

        for (Attribute attr : compAttr.getAttributeList()) {
            LOGGER.debug("build SIP : attribute [{}]", attr.getMetaAttribute().getName());
            if (attr.getMetaAttribute().getName().equals(LATITUDE_MIN)) {
                latMin = (Double) attr.getValueList().get(0);
            } else if (attr.getMetaAttribute().getName().equals(LATITUDE_MAX)) {
                latMax = (Double) attr.getValueList().get(0);
            } else if (attr.getMetaAttribute().getName().equals(LONGITUDE_MIN)) {
                lonMin = (Double) attr.getValueList().get(0);
            } else if (attr.getMetaAttribute().getName().equals(LONGITUDE_MAX)) {
                lonMax = (Double) attr.getValueList().get(0);
            }
        }

        if (latMin != null && latMax != null && lonMin != null && lonMax != null) {
            Positions pos = new Positions();

            Position pos0 = IGeometry.position(lonMin, latMin);
            pos.add(pos0);
            pos.add(IGeometry.position(lonMax, latMin));
            pos.add(IGeometry.position(lonMax, latMax));
            pos.add(IGeometry.position(lonMin, latMax));
            pos.add(pos0);

            PolygonPositions pp = new PolygonPositions();
            pp.add(pos);

            // Add Geometry to SIP
            sipBuilder.setGeometry(IGeometry.polygon(pp));
        }
    }

    /**
     * Add descriptive information to the {@link SIPBuilder} 
     * @param sipBuilder the current {@link SIPBuilder}
     * @param attr a {@link Attribute}
     */
    private void addSip(SIPBuilder sipBuilder, Attribute attr) {
        LOGGER.debug("build SIP : add attribute [{}]", attr.getMetaAttribute().getName());
        if (attr.getValueList().size() == 1) {

            sipBuilder.addDescriptiveInformation(Strings.toLowerCase(attr.getAttributeKey()),
                                                 attr.getValueList().get(0));
        } else {
            sipBuilder.addDescriptiveInformation(Strings.toLowerCase(attr.getAttributeKey()), attr.getValueList());
        }
    }

    @Override
    protected void addDataObjectsToSip(SIPBuilder sipBuilder, List<AcquisitionFile> acqFiles)
            throws AcquisitionException {
        for (AcquisitionFile af : acqFiles) {
            try {
                sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, af.getFile().toURI().toURL(),
                                                                        af.getChecksumAlgorithm(), af.getChecksum());
                sipBuilder.getContentInformationBuilder().setSyntax("Mime name", "Mime Description",
                                                                    af.getMetaFile().getMediaType());
                sipBuilder.addContentInformation();
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                throw new AcquisitionException(e.getMessage());
            }
        }

    }

}
