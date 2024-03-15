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

import fr.cnes.regards.modules.ltamanager.dto.submission.LtaDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.URL;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * A product file is related to a {@link SubmissionRequestDto}. It contains metadata related to a file to store.
 *
 * @author Iliana Ghazali
 **/
public final class ProductFileDto {

    @NotNull(message = "type is required")
    @Schema(description = "Type of the file among RAWDATA, QUICKLOOK(_SD|_MD_|_HD), THUMBNAIL.", example = "THUMBNAIL")
    private final LtaDataType type;

    @NotNull(message = "url is required.")
    @URL(regexp = "^(http|file).*", message = "The url must be valid and declare http(s) or file protocol")
    @Schema(description = "Location of the file. Only http(s) or file protocols are accepted.",
            example = "file:/input/file-lta-100.png")
    private final String url;

    @NotNull(message = "filename is required.")
    @Schema(description = "Name of the file.", example = "thumbnail.png")
    private final String filename;

    @NotNull(message = "checksum is required.")
    @Pattern(regexp = "^[a-fA-F0-9]{32}$", message = "checksum must be in a valid md5 format")
    @Schema(description = "Checksum of the file in md5 format.", example = "d326ed75d1e9c1109a9dbabf114f6b61")
    private final String checksumMd5;

    @NotNull(message = "mimetype is required")
    @Schema(description = "Mimetype of the file.", example = "image/png")
    private final MimeType mimeType;

    @ConstructorProperties({ "type", "url", "filename", "checksumMd5", "mimeType" })
    public ProductFileDto(LtaDataType type, String url, String filename, String checksumMd5, MimeType mimeType) {
        Assert.notNull(type, "type is mandatory !");
        Assert.notNull(url, "url is mandatory !");
        Assert.notNull(filename, "filename is mandatory !");
        Assert.notNull(checksumMd5, "checksumMd5 is mandatory !");
        Assert.notNull(mimeType, "mimeType is mandatory !");

        this.type = type;
        this.url = url;
        this.filename = filename;
        this.checksumMd5 = checksumMd5;
        this.mimeType = mimeType;
    }

    public LtaDataType getType() {
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
