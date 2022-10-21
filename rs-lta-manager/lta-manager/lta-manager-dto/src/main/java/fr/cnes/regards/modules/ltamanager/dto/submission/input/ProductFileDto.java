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

import fr.cnes.regards.framework.urn.DataType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.URL;
import org.springframework.util.MimeType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Objects;

/**
 * A product file is related to a {@link SubmissionRequestDto}. It contains metadata related to a file to store.
 *
 * @author Iliana Ghazali
 **/
public final class ProductFileDto {

    @NotNull(message = "type is required")
    @Schema(description = "Type of the file among RAWDATA, QUICKLOOK(_SD|_MD_|_HD), THUMBNAIL.")
    private final DataType type;

    @URL(regexp = "^(http|file).*", message = "The url must be valid and declare http(s) or file protocol")
    @Schema(description = "Location of the file. Only http(s) or file protocols are accepted.")
    private final String url;

    @NotNull(message = "filename is required.")
    @Schema(description = "Name of the file.")
    private final String filename;

    @Pattern(regexp = "^[a-fA-F0-9]{32}$", message = "checksum must be in a valid md5 format")
    @Schema(description = "Checksum of the file in md5 format.")
    private final String checksumMd5;

    @NotNull(message = "mimetype is required")
    @Schema(description = "Mimetype of the file.")
    private final MimeType mimeType;

    public ProductFileDto(DataType type, String url, String filename, String checksumMd5, MimeType mimeType) {
        this.type = type;
        this.url = url;
        this.filename = filename;
        this.checksumMd5 = checksumMd5;
        this.mimeType = mimeType;
    }

    public DataType getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getFilename() {
        return filename;
    }

    public String getChecksumMd5() {
        return checksumMd5;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductFileDto that = (ProductFileDto) o;
        return type == that.type
               && url.equals(that.url)
               && filename.equals(that.filename)
               && checksumMd5.equals(that.checksumMd5)
               && mimeType.equals(that.mimeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, url, filename, checksumMd5, mimeType);
    }

    @Override
    public String toString() {
        return "ProductFileDto{"
               + "type="
               + type
               + ", url='"
               + url
               + '\''
               + ", filename='"
               + filename
               + '\''
               + ", checksumMd5='"
               + checksumMd5
               + '\''
               + ", mimeType="
               + mimeType
               + '}';
    }
}