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
package fr.cnes.regards.modules.ltamanager.dto.submission.input;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A submission request dto contains information to store a product in a long-term storage space.
 *
 * @author Iliana Ghazali
 **/
public class SubmissionRequestDto {

    @NotBlank(message = "correlationId is required to track this request.")
    @Size(max = 255, message = "correlationId length is limited to 255 characters.")
    @Schema(description = "Identifier to track this request during the entire workflow. It must be unique.",
        maxLength = 255)
    private final String correlationId;

    @NotBlank(message = "id is required.")
    @Size(max = 255, message = "id length is limited to 255 characters.")
    @Schema(description = "Provider id of the OAIS product to generate.", maxLength = 255)
    private final String id;

    @NotBlank(message = "datatype is required.")
    @Size(max = 255, message = "datatype length is limited to 255 characters.")
    @Schema(description = "Product datatype. Must be present in the lta-manager configuration.", maxLength = 255)
    private final String datatype;

    @NotNull(message = "geometry is required.")
    @Valid
    @Schema(description = "Product geometry in GeoJSON RFC 7946 Format.")
    private final IGeometry geometry;

    @NotEmpty(message = "At least one file in a valid format is required.")
    @Valid
    @Schema(description = "Files linked to the product. At least one is required.", minimum = "1")
    private final List<ProductFileDto> files;

    @Nullable
    @Schema(description = "List of string tags.", nullable = true)
    private List<String> tags;

    @Nullable
    @Size(max = 255, message = "origin urn is limited to 255 characters.")
    @Schema(description = "Id of the product in the original catalog.", nullable = true)
    private String originUrn;

    @Nullable
    @Schema(description = "Map of key/value properties.", nullable = true)
    private Map<String, Object> properties;

    @Nullable
    @Size(max = 255, message = "storePath length is limited to 255 characters.")
    @Pattern(regexp = "^[\\w\\/\\-_:]*$",
        message = "storePath must only contain alphanumeric characters and the following characters [/-_:].")
    @Schema(description = "Path to store the product. If null, the storePath will be built from the lta-manager "
                          + "configuration.", nullable = true)
    private String storePath;

    @Nullable
    @Size(max = 128, message = "session length is limited to 128 characters.")
    @Schema(description = "Session to monitor the generation of the product. If not provided, a default session will "
                          + "be used.", nullable = true)
    private String session;

    @Schema(description = "If true, overrides the product if it already exists.", defaultValue = "false")
    private boolean replaceMode;

    // owner is set after the construction of the request
    private String owner;

    @ConstructorProperties({ "correlationId", "id", "datatype", "geometry", "files" })
    public SubmissionRequestDto(String correlationId,
                                String id,
                                String datatype,
                                IGeometry geometry,
                                List<ProductFileDto> files) {
        Assert.notNull(correlationId, "correlationId is mandatory ! Make sure other constraints are satisfied.");
        Assert.notNull(id, "id is mandatory ! Make sure other constraints are satisfied.");
        Assert.notNull(datatype, "datatype is mandatory ! Make sure other constraints are satisfied.");
        Assert.notNull(geometry, "geometry is mandatory ! Make sure other constraints are satisfied.");
        Assert.notEmpty(files, "at least one file is mandatory ! Make sure other constraints are satisfied. ");

        this.correlationId = correlationId;
        this.id = id;
        this.datatype = datatype;
        this.geometry = geometry;
        this.files = files;
    }

    public SubmissionRequestDto(String correlationId,
                                String id,
                                String datatype,
                                IGeometry geometry,
                                List<ProductFileDto> files,
                                @Nullable List<String> tags,
                                @Nullable String originUrn,
                                @Nullable Map<String, Object> properties,
                                @Nullable String storePath,
                                @Nullable String session,
                                boolean replaceMode) {
        this(correlationId, id, datatype, geometry, files);
        this.tags = tags;
        this.originUrn = originUrn;
        this.properties = properties;
        this.storePath = storePath;
        this.session = session;
        this.replaceMode = replaceMode;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getId() {
        return id;
    }

    public String getDatatype() {
        return datatype;
    }

    public IGeometry getGeometry() {
        return geometry;
    }

    public List<ProductFileDto> getFiles() {
        return files;
    }

    @Nullable
    public List<String> getTags() {
        return tags;
    }

    public void setTags(@Nullable List<String> tags) {
        this.tags = tags;
    }

    @Nullable
    public String getOriginUrn() {
        return originUrn;
    }

    public void setOriginUrn(@Nullable String originUrn) {
        this.originUrn = originUrn;
    }

    @Nullable
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(@Nullable Map<String, Object> properties) {
        this.properties = properties;
    }

    @Nullable
    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(@Nullable String storePath) {
        this.storePath = storePath;
    }

    @Nullable
    public String getSession() {
        return session;
    }

    public void setSession(@Nullable String session) {
        this.session = session;
    }

    public boolean isReplaceMode() {
        return replaceMode;
    }

    public void setReplaceMode(boolean replaceMode) {
        this.replaceMode = replaceMode;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmissionRequestDto that = (SubmissionRequestDto) o;
        return correlationId.equals(that.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId);
    }

    @Override
    public String toString() {
        return "SubmissionRequestDto{"
               + "correlationId='"
               + correlationId
               + '\''
               + ", id='"
               + id
               + '\''
               + ", datatype='"
               + datatype
               + '\''
               + ", geometry="
               + geometry
               + ", files="
               + files
               + ", tags="
               + tags
               + ", originUrn='"
               + originUrn
               + '\''
               + ", properties="
               + properties
               + ", storePath='"
               + storePath
               + '\''
               + ", session='"
               + session
               + '\''
               + ", replaceMode="
               + replaceMode
               + ", owner='"
               + owner
               + '\''
               + '}';
    }
}
