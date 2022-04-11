/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.eo.impl;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.io.ModuleGenerator;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.eo.EarthObservationAttribute;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.eo.EOModule;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Module to handle Earth Observation opensearch parameters into ATOM format responses.
 * This ModuleGenerator is executed by rome (see rome.properties)
 *
 * @author Léo Mieulet
 * @see <a href="https://docs.opengeospatial.org/is/13-026r9/13-026r9.html"> Annex D (informative): Metadata Mappings</a>
 */
public class EOModuleGenerator implements ModuleGenerator {

    public static final Namespace EOP_NS = Namespace.getNamespace("eop", EOModule.URI);

    public static final String EOP = "eop";

    private static final Set<Namespace> NAMESPACES;

    static {
        final Set<Namespace> nss = new HashSet<Namespace>();
        nss.add(Namespace.getNamespace(EOP, EOModule.URI));
        NAMESPACES = Collections.unmodifiableSet(nss);
    }

    @Override
    public String getNamespaceUri() {
        return EOModule.URI;
    }

    /**
     * Returns a set with all the URIs (JDOM Namespace elements) this module generator uses.
     * <p/>
     * It is used by the the feed generators to add their namespace definition in the root element
     * of the generated document (forward-missing of Java 5.0 Generics).
     * <p/>
     *
     * @return a set with all the URIs (JDOM Namespace elements) this module generator uses.
     */
    @Override
    public Set<Namespace> getNamespaces() {
        return NAMESPACES;
    }

    @Override
    public void generate(final Module module, final Element element) {
        final EOModuleImpl eoMod = (EOModuleImpl) module;
        Map<EarthObservationAttribute, Object> activeProperties = eoMod.getActiveProperties();
        if (!activeProperties.isEmpty()) {
            handleMetaDataPropertyNode(activeProperties, element, eoMod.getGsonBuilder());
            handleProcedureNode(activeProperties, element, eoMod.getGsonBuilder());
        }
    }

    /**
     * Create the eop:metaDataProperty/eop:EarthObservationMetaData Elements if necessary
     */
    private void handleMetaDataPropertyNode(Map<EarthObservationAttribute, Object> activeProperties,
                                            Element rootElement,
                                            Gson gson) {
        HashSet<EarthObservationAttribute> relatedAttrs = Sets.newHashSet(EarthObservationAttribute.PARENT_IDENTIFIER,
                                                                          EarthObservationAttribute.ACQUISITION_TYPE,
                                                                          EarthObservationAttribute.ACQUISITION_STATION,
                                                                          EarthObservationAttribute.PROCESSOR_NAME,
                                                                          EarthObservationAttribute.PROCESSING_CENTER,
                                                                          EarthObservationAttribute.PROCESSING_DATE,
                                                                          EarthObservationAttribute.ARCHIVING_CENTER,
                                                                          EarthObservationAttribute.PROCESSING_MODE,
                                                                          EarthObservationAttribute.ACQUISITION_SUB_TYPE);
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            Element metaDataPropertyElement = new Element("metaDataProperty", EOP_NS);
            Element earthObservationMetaDataElement = new Element("EarthObservationMetaData", EOP_NS);

            handleMetaDataProperty_DownlinkedToNode(activeProperties, earthObservationMetaDataElement, gson);
            handleMetaDataProperty_ProcessingNode(activeProperties, earthObservationMetaDataElement, gson);
            handleMetaDataProperty_ArchiveInNode(activeProperties, earthObservationMetaDataElement, gson);

            addStandardElement(earthObservationMetaDataElement,
                               EarthObservationAttribute.PARENT_IDENTIFIER,
                               activeProperties.get(EarthObservationAttribute.PARENT_IDENTIFIER),
                               gson);
            addStandardElement(earthObservationMetaDataElement,
                               EarthObservationAttribute.ACQUISITION_TYPE,
                               activeProperties.get(EarthObservationAttribute.ACQUISITION_TYPE),
                               gson);
            addStandardElement(earthObservationMetaDataElement,
                               EarthObservationAttribute.ACQUISITION_SUB_TYPE,
                               activeProperties.get(EarthObservationAttribute.ACQUISITION_SUB_TYPE),
                               gson);

            metaDataPropertyElement.addContent(earthObservationMetaDataElement);
            rootElement.addContent(metaDataPropertyElement);
        }
    }

    private void handleMetaDataProperty_DownlinkedToNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                         Element earthObservationMetaDataElement,
                                                         Gson gson) {
        EarthObservationAttribute property = EarthObservationAttribute.ARCHIVING_CENTER;
        String intermediateElementName = "downlinkedTo";
        String finalElementName = "DownlinkInformation";

        handleUniqueAttributeUnderTwoContent(activeProperties,
                                             earthObservationMetaDataElement,
                                             gson,
                                             property,
                                             intermediateElementName,
                                             finalElementName);
    }

    private void handleUniqueAttributeUnderTwoContent(Map<EarthObservationAttribute, Object> activeProperties,
                                                      Element earthObservationMetaDataElement,
                                                      Gson gson,
                                                      EarthObservationAttribute attribute,
                                                      String intermediateElementName,
                                                      String finalElementName) {
        if (activeProperties.containsKey(attribute)) {
            Element archivedInElement = new Element(intermediateElementName, EOP_NS);
            Element archivingInformationElement = new Element(finalElementName, EOP_NS);

            addStandardElement(archivingInformationElement, attribute, activeProperties.get(attribute), gson);

            archivedInElement.addContent(archivingInformationElement);
            earthObservationMetaDataElement.addContent(archivedInElement);
        }
    }

    private void handleMetaDataProperty_ProcessingNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                       Element earthObservationMetaDataElement,
                                                       Gson gson) {
        HashSet<EarthObservationAttribute> relatedAttrs = Sets.newHashSet(EarthObservationAttribute.PROCESSOR_NAME,
                                                                          EarthObservationAttribute.PROCESSING_CENTER,
                                                                          EarthObservationAttribute.PROCESSING_DATE,
                                                                          EarthObservationAttribute.PROCESSING_MODE);
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            Element processingElement = new Element("processing", EOP_NS);
            Element processingInformationElement = new Element("ProcessingInformation", EOP_NS);

            addStandardElement(processingInformationElement,
                               EarthObservationAttribute.PROCESSOR_NAME,
                               activeProperties.get(EarthObservationAttribute.PROCESSOR_NAME),
                               gson);
            addStandardElement(processingInformationElement,
                               EarthObservationAttribute.PROCESSING_CENTER,
                               activeProperties.get(EarthObservationAttribute.PROCESSING_CENTER),
                               gson);
            addStandardElement(processingInformationElement,
                               EarthObservationAttribute.PROCESSING_DATE,
                               activeProperties.get(EarthObservationAttribute.PROCESSING_DATE),
                               gson);
            addStandardElement(processingInformationElement,
                               EarthObservationAttribute.PROCESSING_MODE,
                               activeProperties.get(EarthObservationAttribute.PROCESSING_MODE),
                               gson);

            processingElement.addContent(processingInformationElement);
            earthObservationMetaDataElement.addContent(processingElement);
        }
    }

    private void handleMetaDataProperty_ArchiveInNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                      Element earthObservationMetaDataElement,
                                                      Gson gson) {
        EarthObservationAttribute property = EarthObservationAttribute.ARCHIVING_CENTER;
        String intermediateElementName = "archivedIn";
        String finalElementName = "ArchivingInformation";

        handleUniqueAttributeUnderTwoContent(activeProperties,
                                             earthObservationMetaDataElement,
                                             gson,
                                             property,
                                             intermediateElementName,
                                             finalElementName);
    }

    /**
     * Create the /om:procedure/eop:EarthObservationEquipment/eop:acquisitionParameters/eop:Acquisition/ Element if necessary
     */
    private void handleProcedureNode(Map<EarthObservationAttribute, Object> activeProperties,
                                     Element rootElement,
                                     Gson gson) {
        if (activeProperties.containsKey(EarthObservationAttribute.ORBIT_NUMBER)) {
            Element procedureElement = new Element("procedure", EOP_NS);
            Element earthObservationEquipmentElement = new Element("EarthObservationEquipment", EOP_NS);
            Element acquisitionParametersElement = new Element("acquisitionParameters", EOP_NS);
            Element acquisitionElement = new Element("Acquisition", EOP_NS);

            addStandardElement(acquisitionElement,
                               EarthObservationAttribute.ORBIT_NUMBER,
                               activeProperties.get(EarthObservationAttribute.ORBIT_NUMBER),
                               gson);

            acquisitionParametersElement.addContent(acquisitionElement);
            earthObservationEquipmentElement.addContent(acquisitionParametersElement);
            procedureElement.addContent(earthObservationEquipmentElement);
            rootElement.addContent(procedureElement);
        }
    }

    protected void addStandardElement(Element rootElement,
                                      EarthObservationAttribute attribute,
                                      Object value,
                                      Gson gson) {
        if ((rootElement != null) && (value != null)) {
            rootElement.addContent(generateElement(attribute, value, gson));
        }
    }

    protected Element generateElement(EarthObservationAttribute attribute, Object value, Gson gson) {
        Element element = new Element(attribute.toString(), EOP_NS);
        element.addContent(gson.toJson(value));
        return element;
    }

}
