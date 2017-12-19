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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.xerces.dom.DocumentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DescriptorFile;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDescriptorElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDescriptorElement.ElementType;

/**
 * represente un fichier descripteur
 * 
 * @author Christophe Mertz
 */
public class DescriptorFileControler implements IDescriptorFileControler<DescriptorFile> {

    public static final String DESCRIPTOR_PREFIX = "desc_";

    public static final String UPDATE_DESCRIPTOR_PREFIX = "update_";

    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptorFileControler.class);

    protected static final String DESCRIPTOR_ROOT_ELEMENT = "SIPAD_DATA";

    protected static final String XMLNS = "http://cnes.fr/dico_SIPAD";

    protected static final String XML_XMLNS = "xmlns";

    protected static final String XMLNS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    protected static final String XML_XMLNS_XSI = "xmlns:xsi";

    protected static final String XSI = "http://cnes.fr/dico_SIPAD";

    protected static final String XML_XSI = "xsi:schemaLocation";

    protected static final String DESCRIPTOR_PROJECT_NAME = "PROJECT_NAME";

    /**
     * nombre maximum d'element a ecrire dans un fichier descripteur
     */
    private static final int MAX_ELEMENTS_IN_DESC_FILE = 1000;

    public DescriptorFileControler() {
        super();
    }

    /**
     * 
     * @return un objet document representant la structure du fichier descripteur
     */
    public static synchronized DocumentImpl getDescDocument(DescriptorFile pDescriptorFile) {
        // Build document
        DocumentImpl newDoc = null;
        if (pDescriptorFile.getDescElementMap().size() > 0) {
            newDoc = new DocumentImpl();

            org.w3c.dom.Element rootElement = newDoc.createElement(DESCRIPTOR_ROOT_ELEMENT);
            rootElement.setAttribute(XML_XMLNS, XMLNS);
            rootElement.setAttribute(XML_XMLNS_XSI, XMLNS_XSI);
            rootElement.setAttribute(XML_XSI, XSI + " " + pDescriptorFile.getDicoName());
            rootElement.setAttribute(DESCRIPTOR_PROJECT_NAME, pDescriptorFile.getProjectName());
            newDoc.appendChild(rootElement);

            // Loop through the blocks to add all blocks
            // data storage objects
            for (List<EntityDescriptorElement> elementList : pDescriptorFile.getDescElementMap().values()) {
                for (EntityDescriptorElement element : elementList) {
                    rootElement.appendChild(SsaltoControlers.getControler(element).getElement(element, newDoc));
                }
            }
        }
        return newDoc;
    }

    /**
     * construit le document a partir de la liste de EntityDescriptorElement passe en parametre
     * 
     * @param elementList
     * @return
     */
    public static synchronized DocumentImpl buildDocument(DescriptorFile pDescriptorFile,
            List<EntityDescriptorElement> elementList) {
        // Build document
        DocumentImpl newDoc = null;
        if (elementList.size() > 0) {
            newDoc = new DocumentImpl();

            org.w3c.dom.Element rootElement = newDoc.createElement(DESCRIPTOR_ROOT_ELEMENT);
            rootElement.setAttribute(XML_XMLNS, XMLNS);
            rootElement.setAttribute(XML_XMLNS_XSI, XMLNS_XSI);
            rootElement.setAttribute(XML_XSI, XSI + " " + pDescriptorFile.getDicoName());
            rootElement.setAttribute(DESCRIPTOR_PROJECT_NAME, pDescriptorFile.getProjectName());
            newDoc.appendChild(rootElement);

            // Loop through the blocks to add all blocks
            // data storage objects
            for (EntityDescriptorElement element : elementList) {
                rootElement.appendChild(SsaltoControlers.getControler(element).getElement(element, newDoc));
            }
        }
        return newDoc;
    }

    /**
     * retourne une liste de DocumentImpl qui contiennent tous les elements de type description u DescriptorFile. Le
     * nombre d'element que peut contenir un fichier est positionne a
     * 
     * @see DescriptorFileControler#MAX_ELEMENTS_IN_DESC_FILE
     * 
     * @return une liste de <code>DocumentImpl</code>
     */
    public static synchronized List<DocumentImpl> getDescDocumentList(DescriptorFile pDescriptorFile) {
        ArrayList<DocumentImpl> descDocumentList = new ArrayList<>();

        // Loop through the blocks to add all blocks data storage objects
        Collection<List<EntityDescriptorElement>> elementListList = pDescriptorFile.getDescElementMap().values();

        TreeSet<EntityDescriptorElement> sortedElementList = new TreeSet<>();
        for (List<EntityDescriptorElement> list : elementListList) {
            sortedElementList.addAll(list);
        }

        // split the list if too large.
        List<EntityDescriptorElement> list = new ArrayList<>();
        for (EntityDescriptorElement element : sortedElementList) {
            list.add(element);
            if (list.size() >= MAX_ELEMENTS_IN_DESC_FILE) {
                DocumentImpl doc = buildDocument(pDescriptorFile, list);
                if (doc != null) {
                    descDocumentList.add(doc);
                }
                list = new ArrayList<>();
            }
        }
        DocumentImpl doc = buildDocument(pDescriptorFile, list);
        if (doc != null) {
            descDocumentList.add(doc);
        }

        return descDocumentList;
    }

    /**
     * retourne une liste de DocumentImpl qui contiennent tous les elements de type update du DescriptorFile. Le nombre
     * d'element que peut contenir un fichier est positionne a @see DescriptorFile#MAX_ELEMENTS_IN_DESC_FILE
     * 
     * @return une liste de <code>DocumentImpl</code>
     */
    public static synchronized List<DocumentImpl> getUpdateDocumentList(DescriptorFile pDescriptorFile) {
        List<DocumentImpl> updateDocumentList = new ArrayList<>();

        // Loop through the blocks to add all blocks data storage objects
        Collection<List<EntityDescriptorElement>> elementListList = pDescriptorFile.getUpdateElementMap().values();
        // split the list if too large.
        SortedSet<EntityDescriptorElement> sortedElementList = new TreeSet<>();
        for (List<EntityDescriptorElement> element : elementListList) {
            sortedElementList.addAll(element);
        }

        List<EntityDescriptorElement> list = new ArrayList<>();
        for (EntityDescriptorElement element : sortedElementList) {
            list.add(element);
            if (list.size() >= MAX_ELEMENTS_IN_DESC_FILE) {
                DocumentImpl doc = buildDocument(pDescriptorFile, list);
                if (doc != null) {
                    updateDocumentList.add(doc);
                }
                list = new ArrayList<>();
            }
        }
        DocumentImpl doc = buildDocument(pDescriptorFile, list);
        if (doc != null) {
            updateDocumentList.add(doc);
        }

        return updateDocumentList;
    }

    /**
     * Cette methode permet d'ajouter un element dans le document en le mergant si il existe deja
     * 
     * @param pElement
     */
    public synchronized void addElementOrMergeToDocument(DescriptorFile pDescriptorFile,
            EntityDescriptorElement pElement) {
        HashMap<String, List<EntityDescriptorElement>> descElementMap = pDescriptorFile.getDescElementMap();
        HashMap<String, List<EntityDescriptorElement>> updateElementMap = pDescriptorFile.getUpdateElementMap();

        List<EntityDescriptorElement> entityDescriptorList;
        if (pElement.getElementType() == ElementType.DESC_ELEMENT_TYPE) {
            entityDescriptorList = descElementMap.get(pElement.getEntityId());
        } else {
            entityDescriptorList = updateElementMap.get(pElement.getEntityId());
        }
        if (entityDescriptorList == null) {
            entityDescriptorList = new ArrayList<>();
        }
        if (entityDescriptorList.contains(pElement)) {
            EntityDescriptorElement oldElement = entityDescriptorList.get(entityDescriptorList.indexOf(pElement));
            SsaltoControlers.getControler(oldElement).merge(oldElement, pElement);
        } else {
            entityDescriptorList.add(pElement);
        }
        if (pElement.getElementType() == ElementType.DESC_ELEMENT_TYPE) {
            descElementMap.put(pElement.getEntityId(), entityDescriptorList);
        } else {
            updateElementMap.put(pElement.getEntityId(), entityDescriptorList);
        }
    }

    /**
     * enleve l'element du document.
     * 
     * @param pElement
     * @throws DescriptorExceptionControler
     *             si l'element n'existe pas
     */
    public synchronized void removeElementFromDocument(DescriptorFile pDescriptorFile, EntityDescriptorElement pElement)
            throws ModuleException {
        List<EntityDescriptorElement> entityDescriptorList;
        if (pElement.getElementType() == ElementType.DESC_ELEMENT_TYPE) {
            entityDescriptorList = pDescriptorFile.getDescElementMap().get(pElement.getEntityId());
        } else {
            entityDescriptorList = pDescriptorFile.getUpdateElementMap().get(pElement.getEntityId());
        }
        if (entityDescriptorList == null) {
            String msg = String.format("No descriptor bloc found in file '%s' for entity '%s'",
                                       pDescriptorFile.getFileName(), pElement.getEntityId());
            LOGGER.error(msg);
            throw new DescriptorException(msg);
        }
        entityDescriptorList.remove(pElement);
    }

    /**
     * supprime tous les dataObject element et tous les dataStorageObject element qui le concerne.
     * 
     * @param pDataSetId
     *            l'identifiant de l'entite dont on veut enlever tous les element correspondants
     * @throws DescriptorExceptionControler
     */
    public synchronized void removeDataObjectElementFromDocument(DescriptorFile pDescriptorFile, String pDataSetId)
            throws ModuleException {
        HashMap<String, List<EntityDescriptorElement>> descElementMap = pDescriptorFile.getDescElementMap();
        HashMap<String, List<EntityDescriptorElement>> updateElementMap = pDescriptorFile.getUpdateElementMap();

        List<EntityDescriptorElement> entityDescriptorList = descElementMap.get(pDataSetId);
        if (entityDescriptorList == null) {
            // 
            String msg = String.format("No descriptor bloc found in file '%s' for entity '%s'",
                                       pDescriptorFile.getFileName(), pDataSetId);
            LOGGER.error(msg);
            throw new DescriptorException(msg);
        }
        descElementMap.remove(pDataSetId);
        entityDescriptorList = updateElementMap.get(pDataSetId);
        if (entityDescriptorList == null) {
            String msg = String.format("No descriptor bloc found in file '%s' for entity '%s'",
                                       pDescriptorFile.getFileName(), pDataSetId);
            LOGGER.error(msg);
            throw new DescriptorException(msg);
        }
        updateElementMap.remove(pDataSetId);
    }

    /**
     * cette methode recupere les element de pFile et les ajoute dans la map des element de cette instance. Dans ce cas,
     * on merge les elements deja existant avec les elements presents dans pFile Chaque type d'element doit decrire la
     * maniere dont il se merge. En ce qui concerne les elements de type "DELETE", ceux ci ne sont pas ajoutés au
     * document, mais sont utilises pour supprimer ces même elements des elements de type update ou desc si ils
     * existent.
     * 
     * @param pFile
     */
    @Override
    public synchronized void merge(DescriptorFile pDescriptorFile, DescriptorFile pFile) {
        HashMap<String, List<EntityDescriptorElement>> descElementMap = pDescriptorFile.getDescElementMap();
        HashMap<String, List<EntityDescriptorElement>> updateElementMap = pDescriptorFile.getUpdateElementMap();

        for (String entityId : pFile.getDescElementMap().keySet()) {
            List<EntityDescriptorElement> elementList = pFile.getDescElementMap().get(entityId);
            for (EntityDescriptorElement descriptorElement : elementList) {
                addElementOrMergeToDocument(pDescriptorFile, descriptorElement);
            }
        }
        for (String entityId : pFile.getUpdateElementMap().keySet()) {
            List<EntityDescriptorElement> elementList = pFile.getUpdateElementMap().get(entityId);
            for (EntityDescriptorElement descriptorElement : elementList) {
                addElementOrMergeToDocument(pDescriptorFile, descriptorElement);
            }
        }
        for (String entityId : pFile.getDeleteElementList()) {
            if (descElementMap.containsKey(entityId)) {
                descElementMap.remove(entityId);
            }
            if (updateElementMap.containsKey(entityId)) {
                updateElementMap.remove(entityId);
            }
        }
    }
}
