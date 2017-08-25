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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.finder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.io.Files;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.calc.ICalculationClass;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression.CompressionFacade;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression.CompressionTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression.exception.CompressionException;

/**
 * classe mere des finder dont le but est de construire une classe - Attribute a partir des informations suivante : -
 * liste des fichiers a traiter, fichier de traduction des valeurs des attributs - map des valeurs des attributs deja
 * trouves.
 * 
 * @author Christophe Mertz
 *
 */
public abstract class AttributeFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeFinder.class);

    /**
     * nom de l'attribut a trouver
     */
    protected String name;

    /**
     * type de valeur de l'attribut a trouver
     */
    protected fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum valueType;

    /**
     * ordre de traitement de l'attribut a trouver
     */
    private int order;

    /**
     * format dans lequel la valeur de l'attribut a ete lue
     */
    protected String formatRead;

    /**
     * format dans lequel la valeur de l'attribut doit etre ecrite dans le fichier descripteur
     */
    protected String formatInXML;

    /**
     * Classe de calcul
     */
    protected ICalculationClass calculationClass;

    /**
     * indique si l'attribut doit renvoyer plusieurs valeurs ou non.
     */
    private Boolean multiValues = Boolean.FALSE;

    /**
     * Properties utile pour l'attribut (filePattern entre autre)
     */
    @Autowired
    protected PluginConfigurationProperties confProperties;

    /**
     * valeur par defaut si la liste des valeurs retournee par le finder est vide
     */
    private String defaultValue;

    private Boolean unzipBefore = Boolean.FALSE;

    private CompressionTypeEnum compression;

    /**
     * List <File> list des répertoires dans lesquels sont décompressés les fichiers
     */
    private final List<File> temporaryUnzippedDirList = new ArrayList<>();

    /**
     * cree un objet Attribute qui va servir a generer l'element XML dans le fichier descripteur
     * 
     * @param pAttributeValueMap
     *            : map de valeurs de l'attribut
     * @param pFileMap
     *            la liste des fichiers a traiter, en clef se trouve le fichier a traiter et en valeur, le fichier sur
     *            l'espace de fourniture.
     * @return
     * @throws PluginAcquisitionException
     *             en cas d'erreur lors de la creation de l'attribute
     */
    @SuppressWarnings("unchecked")
    public Attribute buildAttribute(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {

        LOGGER.debug("START building attribute " + getName());

        Attribute attribute = null;
        try {
            List<Object> valueList = null;

            try {
                // get value list
                valueList = (List<Object>) getValueList(pFileMap, pAttributeValueMap);
            } catch (NumberFormatException e) {
                throw new PluginAcquisitionException(e);
            }

            // add attribut to calculated attribut map
            List<Object> translatedValueList = new ArrayList<>();
            // translate the values
            if (!valueList.isEmpty()) {
                translatedValueList = translateValueList(valueList);
            } else {
                if (defaultValue == null) {
                    String msg = "unable to find a value and no default value has been specified";
                    LOGGER.error(msg);
                    throw new PluginAcquisitionException(msg);
                }
                translatedValueList = new ArrayList<>();
                translatedValueList.add(defaultValue);
            }
            attribute = AttributeFactory.createAttribute(getValueType(), getName(), translatedValueList);
            pAttributeValueMap.put(name, translatedValueList);
        } catch (DomainModelException e) {
            String msg = "unable to create attribute" + getName();
            throw new PluginAcquisitionException(msg, e);
        } finally {
            deleteUnzippedFile();
        }

        LOGGER.debug("END building attribute " + getName());

        return attribute;
    }

    /**
     * traduit la liste des valeurs en applicant les classes de calcul si elles sont definies
     * 
     * @param pValueList
     *            la liste de valeurs a traduire
     * @return List la liste de valeur traduite
     */
    protected List<Object> translateValueList(List<? extends Object> pValueList) {
        List<Object> translatedValueList = new ArrayList<>();
        for (Object value : pValueList) {
            // launch calculation if needed
            if (calculationClass != null) {
                value = calculationClass.calculateValue(value, getValueType(), confProperties);
            }
            translatedValueList.add(value);
        }
        return translatedValueList;
    }

    /**
     * renvoie une liste d'objet dont le type depend du type de valeur de l'attribut.
     * 
     * @see AttributeFactory#createAttribute(AttributeTypeEnum, String, List)
     * @param pFileMap
     *            une liste de SsaltoFile
     * @param pAttributeValueMap
     * @return
     * @throws PluginAcquisitionException
     */
    public abstract List<?> getValueList(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException;

    /**
     * permet de positionner les propriete (filePattern et autre) sur les finder
     * 
     * @param pConfProperties
     */
    public void setAttributProperties(PluginConfigurationProperties pConfProperties) {
        confProperties = pConfProperties;
    }

    /**
     * permet de traduire la valeur lue du format formatRead dans le format formatInXml_ pour pouvoir inserer la valeur
     * dans la classe Attribute la traduction se fait essentiellement entre les valeur de type DATE
     */
    protected String changeFormat(Object pValue) throws PluginAcquisitionException {
        String returnValue = pValue.toString();
        if (valueType.equals(AttributeTypeEnum.TYPE_DATE) || valueType.equals(AttributeTypeEnum.TYPE_DATE_TIME)) {
            // the format must be externally synchronized
            try {
                Date date = (Date) pValue;
                DateFormat outputFormat = new SimpleDateFormat(formatInXML);
                returnValue = outputFormat.format(date);
            } catch (Exception e) {
                String msg = "unable to parse attribute " + getName() + " value " + pValue + " in format " + formatRead;
                LOGGER.error(msg, e);
                throw new PluginAcquisitionException(msg, e);
            }
        } else if (valueType.equals(AttributeTypeEnum.TYPE_INTEGER)) {
            returnValue = pValue.toString();
        } else if (valueType.equals(AttributeTypeEnum.TYPE_REAL)) {
            returnValue = pValue.toString();
        }

        return returnValue;
    }

    /**
     * Parse la value et cree l'objet java en fonction du type de l'attribut
     * 
     * @param pValue
     * @return
     * @throws PluginAcquisitionException
     */
    protected Object valueOf(String pValue) throws PluginAcquisitionException {
        Object parsedValue = null;
        if (valueType.equals(AttributeTypeEnum.TYPE_DATE) || valueType.equals(AttributeTypeEnum.TYPE_DATE_TIME)) {
            DateFormat format = new SimpleDateFormat(formatRead, Locale.US);
            // the format must be externally synchronized
            synchronized (format) {
                try {
                    parsedValue = format.parse(pValue.toString());
                } catch (ParseException e) {
                    LOGGER.error("", e);
                    throw new PluginAcquisitionException(e);
                }
            }
        } else if (valueType.equals(AttributeTypeEnum.TYPE_INTEGER)) {
            parsedValue = Integer.valueOf(pValue);
        } else if (valueType.equals(AttributeTypeEnum.TYPE_REAL)) {
            parsedValue = Double.valueOf(pValue);
        } else if (valueType.equals(AttributeTypeEnum.TYPE_STRING)
                || valueType.equals(AttributeTypeEnum.TYPE_LONG_STRING)) {
            parsedValue = pValue;
        } else if (valueType.equals(AttributeTypeEnum.TYPE_URL)) {
            try {
                parsedValue = new URL(pValue);
            } catch (MalformedURLException e) {
                LOGGER.error("", e);
                throw new PluginAcquisitionException(e);
            }
        }
        // le type CLOB n'est pas utilise
        return parsedValue;
    }

    /**
     * permet de recuperer une liste de java.io.File correspondant aux fichiers qui se trouvent dans l'espace de travail
     * de l'acquisition des ssaltoFile passes en parametre.
     * 
     * @param pSsaltoFileList
     * @return
     */
    protected List<File> buildFileList(Map<File, ?> pFileMap) throws PluginAcquisitionException {
        if (pFileMap.isEmpty()) {
            LOGGER.error("No SsaltoFile file");
        }
        List<File> unzippedFileList = new ArrayList<>();
        // liste des fichiers zip
        List<File> zipFileList = new ArrayList<>();
        for (File physicalFile : pFileMap.keySet()) {
            // if physical file is null, file is not in the workingDirectory
            // so metada cannot be ingested
            if (physicalFile != null) {
                zipFileList.add(physicalFile);
                LOGGER.debug("Add file " + physicalFile.getName());
            } else {
                String msg = "file not found int the acquisition working Directory";
                throw new PluginAcquisitionException(msg);
            }
        }
        if (unzipBefore.booleanValue()) {
            for (File zipFile : zipFileList) {
                // ajoute les fichiers dezippé
                unzippedFileList.addAll(unzip(zipFile));
            }
        } else {
            unzippedFileList = zipFileList;
        }
        // change le filePattern pour utiliser les fichiers dezippe
        if (unzippedFileList.isEmpty()) {
            LOGGER.debug("No file found to be treated by this plugIn");
        }
        return unzippedFileList;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(this.getClass());
        buff.append(" | name : ").append(name);
        buff.append(" | valueType : ").append(valueType);
        buff.append(" | order : ").append(order);
        buff.append(" | formatRead : ").append(formatRead);
        buff.append(" | formatInXml : ").append(formatInXML);
        buff.append(" | calculationClass").append(calculationClass);
        buff.append(" | multiValues : ").append(multiValues);
        return buff.toString();
    }

    /**
     * cette methode dezippe le fichier passe en parametre, et renvoie la liste des fichiers resultants.
     * 
     * @param pFile
     * @return une liste de java.io.File
     */
    private Collection<File> unzip(File pFile) throws PluginAcquisitionException {
        Collection<File> unzippedFileList = new HashSet<>();
        ;
        File temporaryDir = getTemporaryUnzippedDir(pFile);
        try {
            CompressionFacade compressor = new CompressionFacade();
            // prepare le repertoire de sortie
            // sauvegarde le repertoire afin de l'effacer en cas de probleme
            temporaryUnzippedDirList.add(temporaryDir);
            compressor.decompress(compression, pFile, temporaryDir);
        } catch (CompressionException e) {
            LOGGER.error(e.getMessage());
            throw new PluginAcquisitionException(e);
        }

        // TODO CMZ : à controler que c'est OK
        Files.fileTreeTraverser().children(temporaryDir).forEach(a -> unzippedFileList.add(a));

        return unzippedFileList;
    }

    /**
     * Cree le repertoire dans lequel les fichiers sont dezippe
     * 
     * @param pFile
     *            le fichier zip
     * @return le repertoire dans lequel les fichiers sont dezippe
     * @throws PluginAcquisitionException
     */
    private File getTemporaryUnzippedDir(File pFile) throws PluginAcquisitionException {
        File temporaryUnzippedDir = null;

        // recupere le nom du fichier sans l'extension
        String baseDirName = com.google.common.io.Files.getNameWithoutExtension(pFile.getName());
        baseDirName += "_" + Calendar.getInstance().getTimeInMillis();

        // verifie les droits d'ecriture
        if (pFile.getParentFile().canWrite()) {
            // cree le repertoire
            temporaryUnzippedDir = new File(pFile.getParentFile(), baseDirName);
            if (!temporaryUnzippedDir.mkdir()) {
                String message = String.format("Unable to create the directory '%s'",
                                               pFile.getParentFile().getAbsolutePath());
                LOGGER.error(message);
                throw new PluginAcquisitionException(message);
            }
        } else {
            String message = String.format("No access right to '%s'", pFile.getName());
            LOGGER.error(message);
            throw new PluginAcquisitionException(message);
        }
        return temporaryUnzippedDir;
    }

    /**
     * Efface les repertoires ayant servit pour dezipper les fichiers
     * 
     * @throws PluginAcquisitionException
     */
    private void deleteUnzippedFile() throws PluginAcquisitionException {
        if (unzipBefore.booleanValue()) {
            for (Object element : temporaryUnzippedDirList) {
                File dir = (File) element;
                try {
                    java.nio.file.Files.delete(Paths.get(dir.getAbsolutePath()));
                } catch (IOException e) {
                    throw new PluginAcquisitionException(e);
                }

            }
        }
    }

    public void setName(String pName) {
        name = pName;
    }

    public void setValueType(String pType) throws Exception {
        try {
            valueType = AttributeTypeEnum.parse(pType);
        } catch (Exception e) {
            String msg = "unable to parse valueType " + pType;
            LOGGER.error(msg);
            throw e;
        }
    }

    public void setOrder(String pOrder) {
        order = Integer.parseInt(pOrder);
    }

    public void setFormatRead(String pFormatRead) {
        formatRead = pFormatRead;
    }

    public void setFormatInXml(String pFormatInXml) {
        formatInXML = pFormatInXml;
    }

    public void setMultiValues(String pMultiValues) {
        multiValues = new Boolean(pMultiValues);
    }

    public String getFormatInXML() {
        return formatInXML;
    }

    public String getFormatRead() {
        return formatRead;
    }

    public Boolean getMultiValues() {
        return multiValues;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public AttributeTypeEnum getValueType() {
        return valueType;
    }

    public void setCalculationClass(String pCalculationClass) throws Exception {
        calculationClass = (ICalculationClass) Class.forName(pCalculationClass).newInstance();
    }

    public void setDefaultValue(String pDefaultValue) {
        defaultValue = pDefaultValue;
    }

    public void setUnzipBefore(String pUnzipBefore) {
        unzipBefore = Boolean.valueOf(pUnzipBefore);
    }

    public void setCompression(String pCompression) {
        compression = CompressionTypeEnum.parse(pCompression);
    }
}
