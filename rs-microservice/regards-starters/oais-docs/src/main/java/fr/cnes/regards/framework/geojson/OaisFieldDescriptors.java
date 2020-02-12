/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.base.Strings;

import fr.cnes.regards.framework.oais.AccessRightInformation;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;
import fr.cnes.regards.framework.oais.ProvenanceInformation;
import fr.cnes.regards.framework.oais.RepresentationInformation;
import fr.cnes.regards.framework.oais.Syntax;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.urn.DataType;

/**
 * Builds the description of all fields found in {@link AbstractFeatureCollection}.
 * @author Christophe Mertz
 */
public class OaisFieldDescriptors {

    private final String initPrefix;

    public OaisFieldDescriptors() {
        super();
        initPrefix = null;
    }

    public OaisFieldDescriptors(String prefix) {
        super();
        this.initPrefix = prefix;
    }

    public List<FieldDescriptor> build() {
        List<FieldDescriptor> lfd = new ArrayList<>();

        /**
         * OAIS content information
         */
        ConstrainedFields contentInformationField = new ConstrainedFields(ContentInformation.class);

        lfd.add(contentInformationField
                .withPath(addPrefix("properties.contentInformations[]"),
                          "A set of information that is the original target of preservation or that includes part or all of that information. It is an information object composed of its content data object and its representation information."));

        /**
         * Representation information
         */
        lfd.add(contentInformationField
                .withPath(addPrefix("properties.contentInformations[].representationInformation"),
                          "representationInformation",
                          "The information that maps a data object into more meaningful concepts."));

        lfd.addAll(buildRepresentationInformationDescription());

        /**
         * Data object
         */
        lfd.add(contentInformationField.withPath(addPrefix("properties.contentInformations[].dataObject"), "dataObject",
                                                 "A data object"));

        lfd.addAll(buildDataObjectDescription(addPrefix("properties.contentInformations[].dataObject.")));

        /**
         * Preservation description information
         */
        ConstrainedFields informationPackageField = new ConstrainedFields(InformationPackageProperties.class);
        lfd.add(informationPackageField
                .withPath(addPrefix("properties.pdi"), "pdi",
                          "The information which is necessary for adequate preservation of the Content Information"));

        lfd.addAll(buildPdiDescription(addPrefix("properties.pdi.")));

        return lfd;
    }

    private List<FieldDescriptor> buildRepresentationInformationDescription() {
        List<FieldDescriptor> lfd = new ArrayList<>();

        String path = addPrefix("properties.contentInformations[].representationInformation.");

        ConstrainedFields representationInformationField = new ConstrainedFields(RepresentationInformation.class);

        lfd.add(representationInformationField.withPath(addPrefix(path, "syntax"), "syntax", "A data objet syntax"));
        // TODO manque Semantic

        lfd.addAll(buildSyntaxDescription(addPrefix(path, "syntax.")));

        return lfd;
    }

    private List<FieldDescriptor> buildSyntaxDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<>();

        ConstrainedFields syntaxField = new ConstrainedFields(Syntax.class);

        lfd.add(syntaxField.withPath(addPrefix(prefix, "name"), "name", "A syntax name").optional().type("String"));
        lfd.add(syntaxField.withPath(addPrefix(prefix, "description"), "description", "A description").optional()
                .type("String"));
        lfd.add(syntaxField.withPath(addPrefix(prefix, "mimeType"), "mimetype",
                                     "A two-part identifier for file formats and format contents"));

        return lfd;
    }

    private List<FieldDescriptor> buildDataObjectDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<>();

        ConstrainedFields oaisDataObjectField = new ConstrainedFields(OAISDataObject.class);

        StringJoiner joiner = new StringJoiner(", ");
        for (DataType state : DataType.values()) {
            joiner.add(state.name());
        }

        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "regardsDataType"), "regardsDataType",
                                             "REGARDS data object type", "Allowed values : " + joiner.toString()));
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "filename"), "filename", "The data object file name")
                .optional().type("String"));
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "locations"), "locations", "A set of locations"));
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "locations[].url"), "location URL",
                                             "URL location associated to optional storage property"));
        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "fileSize"), "fileSize", "The data object size in bytes")
                .optional().type("Long"));

        lfd.add(oaisDataObjectField.withPath(addPrefix(prefix, "checksum"), "checksum",
                                             "The calculated data object checksum"));
        // the property path "algorithm-fake" is unknown, if we set "algorithm"
        // the doc generation does not work :
        // java.util.MissingResourceException: Can't find resource for bundle java.util.PropertyResourceBundle, key fr.cnes.regards.framework.utils.file.validation.HandledMessageDigestAlgorithm.description
        lfd.add(oaisDataObjectField
                .withPath(addPrefix(prefix, "algorithm"), "algorithm-fake", "The checksum algorithm used",
                          "see https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html[java.security.MessageDigest]"));

        return lfd;
    }

    private List<FieldDescriptor> buildPdiDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<>();

        ConstrainedFields preservationField = new ConstrainedFields(PreservationDescriptionInformation.class);

        lfd.add(preservationField.withPath(addPrefix(prefix, "contextInformation.tags[]"), "tags", "A set of tags"));
        lfd.add(preservationField.withPath(addPrefix(prefix, "referenceInformation"),
                                           "The information that is used as an identifier for the content information.",
                                           "additional identifier"));
        lfd.add(preservationField
                .withPath(addPrefix(prefix, "fixityInformation"), "fixityInformation",
                          "The information which documents the mechanisms that ensure that the content information object has not been altered in an undocumented manner. An example is a Cyclical Redundancy Check (CRC) code for a file. "));

        lfd.add(preservationField.withPath(addPrefix(prefix, "provenanceInformation"), "provenanceInformation",
                                           "The information that documents the history of the content information"));
        lfd.addAll(buildProvenanceDescription(addPrefix(prefix, "provenanceInformation.")));

        lfd.add(preservationField
                .withPath(addPrefix(prefix, "accessRightInformation"), "accessRightInformation",
                          "The information that identifies the access restrictions pertaining to the content information, including the legal framework, licensing terms, and access control."));
        lfd.addAll(buildAccessRightDescription(addPrefix(prefix, "accessRightInformation.")));

        return lfd;
    }

    private List<FieldDescriptor> buildProvenanceDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<>();

        ConstrainedFields provenanceField = new ConstrainedFields(ProvenanceInformation.class);

        lfd.add(provenanceField.withPath(addPrefix(prefix, "history[]"), "history", "A list of events").optional()
                .type("String"));
        lfd.addAll(buildEventDescription(addPrefix(prefix, "history[].")));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "facility"), "facility", "A facility").optional()
                .type("String"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "instrument"), "instrument", "An instrument").optional()
                .type("String"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "filter"), "filter", "A filter").optional().type("String"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "detector"), "detector", "A detector").optional()
                .type("String"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "proposal"), "proposal", "A proposal").optional()
                .type("String"));
        lfd.add(provenanceField.withPath(addPrefix(prefix, "additional"), "additional", "An additional information")
                .optional().type("String"));

        return lfd;
    }

    private List<FieldDescriptor> buildEventDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<>();

        ConstrainedFields eventField = new ConstrainedFields(Event.class);

        lfd.add(eventField.withPath(addPrefix(prefix, "type"), "The event's type").optional().type("String"));
        lfd.add(eventField.withPath(addPrefix(prefix, "comment"), "The event's comment"));
        lfd.add(eventField.withPath(addPrefix(prefix, "date"), "date", "ISO Date time",
                                    ". Required format : yyyy-MM-dd’T’HH:mm:ss.SSSZ"));

        return lfd;
    }

    private List<FieldDescriptor> buildAccessRightDescription(String prefix) {
        List<FieldDescriptor> lfd = new ArrayList<>();

        ConstrainedFields accessRightField = new ConstrainedFields(AccessRightInformation.class);

        lfd.add(accessRightField.withPath(addPrefix(prefix, "licence"), "The licence").optional().type("String"));
        lfd.add(accessRightField.withPath(addPrefix(prefix, "dataRights"), "dataRights", "A data access rights"));
        lfd.add(accessRightField.withPath(addPrefix(prefix, "publicReleaseDate"), "publicReleaseDate", "ISO Date time",
                                          "Required format : yyyy-MM-dd’T’HH:mm:ss.SSSZ")
                .optional().type("String"));

        return lfd;
    }

    private String addPrefix(String path) {
        return Strings.isNullOrEmpty(this.initPrefix) ? path : initPrefix + path;
    }

    private String addPrefix(String prefix, String path) {
        return Strings.isNullOrEmpty(prefix) ? path : prefix + path;
    }
}
