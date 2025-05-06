/*

 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.oais.dto;

import fr.cnes.regards.framework.oais.dto.validator.ValidOAISDataObjectChecksum;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.file.validation.HandledMessageDigestAlgorithm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * OAIS data object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 * @author Michael Nguyen
 */
@ValidOAISDataObjectChecksum(message = "The data object checksum is not valid for this algorithm")
public class OAISDataObjectDto {

    /**
     * The regards data type
     */
    @NotNull(message = "REGARDS data type is required to qualify the related data file")
    @Schema(description = "File type.")
    private DataType regardsDataType;

    /**
     * File locations (a file can be stored at several locations)
     */
    @Valid
    @NotEmpty(message = "At least one location is required")
    @Schema(description = "File locations.")
    private Set<OAISDataObjectLocationDto> locations = new HashSet<>();

    /**
     * The file name
     */
    @NotBlank(message = "Filename is required")
    @Schema(description = "File name.", example = "data_file.raw")
    private String filename;

    /**
     * The checksum algorithm (<b>required</b> if data object is not a reference)
     */
    @HandledMessageDigestAlgorithm
    @Schema(description = "File checksum algorithm.", example = "MD5", allowableValues = { "MD5" })
    private String algorithm;

    /**
     * The checksum (<b>required</b>)
     */
    @NotEmpty(message = "Checksum is required")
    @Schema(description = "File checksum.", example = "145ff4e2fb057359fe66bd398aef3f9b")
    private String checksum;

    /**
     * The file size
     */
    @Schema(description = "File size bytes.", example = "120568")
    private Long fileSize;

    @Schema(type = "object", description = "Additional fields in JSON format", hidden = true)
    private Object additionalFields;

    /**
     * @return the file name
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Set the file name
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the regards data type
     */
    public DataType getRegardsDataType() {
        return regardsDataType;
    }

    /**
     * Set the regards data type
     */
    public void setRegardsDataType(DataType regardsDataType) {
        this.regardsDataType = regardsDataType;
    }

    public Set<OAISDataObjectLocationDto> getLocations() {
        return locations;
    }

    public void setLocations(Set<OAISDataObjectLocationDto> locations) {
        this.locations = locations;
    }

    public void addLocation(OAISDataObjectLocationDto location) {
        this.locations.add(location);
    }

    /**
     * @return the checksum algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the checksum algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Set the checksum
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the file size
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * Set the file size
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Object getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Object additionalFields) {
        this.additionalFields = additionalFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OAISDataObjectDto that = (OAISDataObjectDto) o;
        return regardsDataType == that.regardsDataType
               && Objects.equals(locations, that.locations)
               && Objects.equals(filename,
                                 that.filename)
               && Objects.equals(algorithm, that.algorithm)
               && Objects.equals(checksum, that.checksum)
               && Objects.equals(fileSize, that.fileSize)
               && Objects.equals(additionalFields, that.additionalFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regardsDataType, locations, filename, algorithm, checksum, fileSize, additionalFields);
    }

}
