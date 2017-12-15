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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.exception.DescriptorException;

/**
 * represente un fichier descripteur
 * 
 * @author Christophe Mertz
 */
public class DescriptorFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptorFile.class);

    private String dicoName;

    private String projectName;

    /**
     * le nom du fichier descripteur
     */
    private String fileName;

    /**
     * Une {@link Map} contenant les elements du document.
     * Cette map est constituée de la manière suivante :
     * <li>en clef : une {@link String}
     * <li>en valeur : une {@link List} de {@link EntityDescriptorElement}
     */
    private final HashMap<String, List<EntityDescriptorElement>> descElementMap;

    private final HashMap<String, List<EntityDescriptorElement>> updateElementMap;

    private final List<String> deleteElementList;

    public DescriptorFile() {
        super();
        descElementMap = new HashMap<>();
        updateElementMap = new HashMap<>();
        deleteElementList = new ArrayList<>();
    }

    public DescriptorFile(String dicoName, String projectName) {
        super();
        this.dicoName = dicoName;
        this.projectName = projectName;
//        this.fileName = null;

        descElementMap = new HashMap<>();
        updateElementMap = new HashMap<>();
        deleteElementList = new ArrayList<>();
}

    public HashMap<String, List<EntityDescriptorElement>> getDescElementMap() {
        return descElementMap;
    }

    public HashMap<String, List<EntityDescriptorElement>> getUpdateElementMap() {
        return updateElementMap;
    }

    public List<String> getDeleteElementList() {
        return deleteElementList;
    }

    /**
     * Cette methode permet d'ajouter un element de type description dans le document.
     * 
     * @param pElement
     * @throws DescriptorExceptionControler
     *             si l'element existe deja
     */
    public synchronized void addDescElementToDocument(EntityDescriptorElement pElement) throws DescriptorException {
        List<EntityDescriptorElement> entityDescriptorList = descElementMap.get(pElement.getEntityId());
        if (entityDescriptorList == null) {
            entityDescriptorList = new ArrayList<>();
            descElementMap.put(pElement.getEntityId(), entityDescriptorList);
        }
        if (entityDescriptorList.contains(pElement)) {
            String msg = String.format("Descriptor bloc found in file '%s' for entity '%s'", fileName,
                                       pElement.getEntityId());
            throw new DescriptorException(msg);
        }

        entityDescriptorList.add(pElement);

        LOGGER.debug("***** Element added to desc element map");
    }

    /**
     * Cette methode permet d'ajouter un element de type update dans le document.
     * 
     * @param pElement
     * @throws DescriptorExceptionControler
     *             si l'element existe deja
     */
    public synchronized void addUpdateElementToDocument(EntityDescriptorElement pElement) throws ModuleException {
        List<EntityDescriptorElement> entityDescriptorList = updateElementMap.get(pElement.getEntityId());
        if (entityDescriptorList == null) {
            entityDescriptorList = new ArrayList<>();
            updateElementMap.put(pElement.getEntityId(), entityDescriptorList);
        }
        if (entityDescriptorList.contains(pElement)) {
            String msg = String.format("Descriptor bloc found in file '%s' for entity '%s'", fileName,
                                       pElement.getEntityId());
            throw new DescriptorException(msg);
        }

        entityDescriptorList.add(pElement);

        LOGGER.info("***** Element added to update element map : update bloc id " + pElement.getEntityId());
    }

    /**
     * Cette methode permet d'ajouter un element de type delete dans le document. Remarque : la liste des elements de
     * type delete est utiliser lors du merge pour ne pas ajouter d'element pour lequel on a demander la suppression par
     * la suite.
     * 
     * @param pElement
     * @throws DescriptorExceptionControler
     */
    public synchronized void addDelElementToDocument(EntityDeletionDescriptorElement pElement)
            throws DescriptorException {
        List<String> elementList = pElement.getDataStorageObjectList();

        // For each element of the deletion descriptor element
        for (String entityId : elementList) {
            if (deleteElementList.contains(entityId)) {
                String msg = String.format("Descriptor bloc found in file '%s' for entity '%s'", fileName,
                                           pElement.getEntityId());
                throw new DescriptorException(msg);
            }

            deleteElementList.add(entityId);

            LOGGER.info("***** Element added to delete element list : delete bloc id " + pElement.getEntityId());
        }
    }

    public void setDicoName(String name) {
        dicoName = name;
    }

    public String getDicoName() {
        return dicoName;
    }

    public void setProjectName(String name) {
        projectName = name;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String name) {
        fileName = name;
    }
}
