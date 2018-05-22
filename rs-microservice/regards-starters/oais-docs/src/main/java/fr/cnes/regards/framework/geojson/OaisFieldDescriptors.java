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
package fr.cnes.regards.framework.geojson;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.restdocs.payload.FieldDescriptor;

import com.netflix.servo.util.Strings;

import fr.cnes.regards.framework.oais.AccessRightInformation;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;
import fr.cnes.regards.framework.oais.ProvenanceInformation;
import fr.cnes.regards.framework.oais.RepresentationInformation;
import fr.cnes.regards.framework.oais.Syntax;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;

/**
 * Builds the description of all fields found in {@link AbstractFeatureCollection}.
 *  
 * @author Christophe Mertz
 *
 */
public class OaisFieldDescriptors {

    private String initPrefix;

    public OaisFieldDescriptors() {
        super();
        initPrefix = null;
    }

    public OaisFieldDescriptors(String prefix) {
        super();
        this.initPrefix = prefix;
    }

    public List<FieldDescriptor> build() {
        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        /**
         * OAIS content information
         */
        ConstrainedFields contentInformationField = new ConstrainedFields(ContentInformation.class);

        lfd.add(contentInformationField.withPath(addPrefix("properties.contentInformations[]"),
                                                 "this include the data objects and its representation information"));

        /**
         * Representation information
         */
        lfd.add(contentInformationField
                .withPath(addPrefix("properties.contentInformations[].representationInformation"),
                          "representationInformation", "the data objet representation information"));

        lfd.addAll(buildRepresentationInformationDescription());

        /**
         * Data object
         */
        lfd.add(contentInformationField.withPath(addPrefix("properties.contentInformations[].dataObject"), "dataObject",
                                                 "the data object"));

        lfd.addAll(buildDataObjectDescription(addPrefix("properties.contentInformations[].dataObject.")));

        /**
         * Preservation description information
         */
        ConstrainedFields informationPackageField = new ConstrainedFields(InformationPackageProperties.class);
        lfd.add(informationPackageField.withPath(addPrefix("properties.pdi"), "pdi",
                                                 "preservation description information"));

        lfd.addAll(buildPdiDescription(addPrefix("properties.pdi.")));

        return lfd;
    }

    private List<FieldDescriptor> buildRepresentationInformationDescription() {
        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        String path = addPrefix("properties.contentInformations[].representationInformation.");

        ConstrainedFields representationInformationField = new ConstrainedFields(RepresentationInformation.class);

        lfd.add(representationInformationField.withPath(addPrefix(path, "syntax"), "syntax", "the data objet syntax"));

        lfd.addAll(buildSyntaxDescription(addPrefix(path, "syntax.")));

        return lfd;
    }

    private List<FieldDescriptor> buildSyntaxDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        ConstrainedFields syntaxField = new ConstrainedFields(Syntax.class);

        lfd.add(syntaxField.withPath(addPrefix(prefix, "name"), "name"));
        lfd.add(syntaxField.withPath(addPrefix(prefix, "description"), "description"));
        lfd.add(syntaxField.withPath(addPrefix(prefix, "mimeType"), "mimetype",
                                     "two-part identifier for file formats and format contents"));

        return lfd;
    }

    private List<FieldDescriptor> buildDataObjectDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        ConstrainedFields oaisDataObjectField = new ConstrainedFields(OAISDataObject.class);

        StringJoiner joiner = new StringJoiner(", ");
        for (DataType state : DataType.values()) {
            joiner.add(state.name());
        }
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "regardsDataType"), "regardsDataType",
                                             "REGARDS data object type", "Allowed values : " + joiner.toString()));
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "filename"), "the data object file name"));
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "urls"), "urls", "a set of URL"));
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "algorithm"), "the checksum algorithm"));
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "checksum"), "the calculated data object checksum"));
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "fileSize"), "the data object size in bytes"));

        return lfd;
    }

    private List<FieldDescriptor> buildPdiDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        ConstrainedFields preservationField = new ConstrainedFields(PreservationDescriptionInformation.class);

        lfd.add(preservationField.withPath(addPrefix(prefix, "contextInformation.tags[]"), "tags", "a set of tags"));
        lfd.add(preservationField.withPath(addPrefix(prefix, "referenceInformation"), "referenceInformation",
                                           "additional identifier"));
        lfd.add(preservationField.withPath(addPrefix(prefix, "fixityInformation"), "fixityInformation",
                                           "fixity information"));

        lfd.add(preservationField.withPath(addPrefix(prefix, "provenanceInformation"), "provenanceInformation",
                                           "provenance information"));
        lfd.addAll(buildProvenanceDescription(addPrefix(prefix, "provenanceInformation.")));

        lfd.add(preservationField.withPath(addPrefix(prefix, "accessRightInformation"), "accessRightInformation",
                                           "access right information"));
        lfd.addAll(buildAccessRightDescription(addPrefix(prefix, "accessRightInformation.")));

        

        return lfd;
    }

    private List<FieldDescriptor> buildProvenanceDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        ConstrainedFields provenanceField = new ConstrainedFields(ProvenanceInformation.class);

        lfd.add(provenanceField.withPath(addPrefix(prefix, "history[]"), "history", "list of events"));
        lfd.addAll(buildEventDescription(addPrefix(prefix, "history[].")));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "facility"), "facility"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "instrument"), "instrument"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "filter"), "filter"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "detector"), "detector"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "proposal"), "proposal"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "additional"), "additional"));

        return lfd;
    }

    private List<FieldDescriptor> buildEventDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        ConstrainedFields eventField = new ConstrainedFields(Event.class);

        lfd.add(eventField.withPath(addPrefix(prefix, "type"), "type"));
        lfd.add(eventField.withPath(addPrefix(prefix, "comment"), "comment"));
        lfd.add(eventField.withPath(addPrefix(prefix, "date"), "date", "UTC format"));

        return lfd;
    }

    private List<FieldDescriptor> buildAccessRightDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        ConstrainedFields accessRightField = new ConstrainedFields(AccessRightInformation.class);

        lfd.add(accessRightField.withPath(addPrefix(prefix, "licence"), "licence"));
        lfd.add(accessRightField.withPath(addPrefix(prefix, "dataRights"), "dataRights", "data access rights"));
        lfd.add(accessRightField.withPath(addPrefix(prefix, "publicReleaseDate"), "publicReleaseDate", "UTC format"));

        return lfd;
    }

    private String addPrefix(String path) {
        return Strings.isNullOrEmpty(this.initPrefix) ? path : initPrefix + path;
    }

    private String addPrefix(String prefix, String path) {
        return Strings.isNullOrEmpty(prefix) ? path : prefix + path;
    }
}
