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
package fr.cnes.regards.modules.order.domain.dto;

import fr.cnes.regards.modules.order.domain.OrderDataFile;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.MimeType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * @author Thomas GUILLOU
 **/
public class OrderDataFileDTO {

    @Schema(description = "Identifier of the file represented by this dto.")
    private final Long id;

    @Schema(description = "providerId of the product containing this file.", maxLength = 255)
    private final String productId;

    @Schema(description = "version of the product.")
    private final Integer version;

    /**
     * Download link. This DTO is used to download files with API rest.
     * Entity OrderDAtaFile url does not update correctly fields at downloading, thus we use this attribute to send a
     * better download link
     */
    @Schema(description = "downloadUrl of the file represented by this dto.", maxLength = 255)
    @NotBlank(message = "downloadUrl is required")
    private String downloadUrl;

    /**
     * Required {@link MimeType}
     */
    @Schema(description = "MIME type of the file represented by this dto.")
    @NotNull(message = "MIME type is required")
    private final MimeType mimeType;

    /**
     * Optional file checksum to verify data consistency
     */
    @Schema(description = "Checksum of the file represented by this dto.", maxLength = 255)
    private final String checksum;

    /**
     * Optional file size
     */
    @Schema(description = "Size of the file represented by this dto.")
    private final Long filesize;

    /**
     * Required filename
     */
    @NotBlank(message = "Filename is required")
    @Schema(description = "Name of the file represented by this dto.", maxLength = 255)
    private final String filename;

    public OrderDataFileDTO(Long id,
                            @Nullable String productId,
                            @Nullable Integer version,
                            String downloadUrl,
                            MimeType mimeType,
                            @Nullable String checksum,
                            @Nullable Long filesize,
                            String filename) {
        this.id = id;
        this.productId = productId;
        this.version = version;
        this.downloadUrl = downloadUrl;
        this.mimeType = mimeType;
        this.checksum = checksum;
        this.filesize = filesize;
        this.filename = filename;
    }

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public String getChecksum() {
        return checksum;
    }

    public Long getFilesize() {
        return filesize;
    }

    public String getFilename() {
        return filename;
    }

    public Integer getVersion() {
        return version;
    }

    public static OrderDataFileDTO fromOrderDataFile(OrderDataFile orderDataFile) {
        return new OrderDataFileDTO(orderDataFile.getId(),
                                    orderDataFile.getProductId(),
                                    orderDataFile.getVersion(),
                                    orderDataFile.getUri(),
                                    orderDataFile.getMimeType(),
                                    orderDataFile.getChecksum(),
                                    orderDataFile.getFilesize(),
                                    orderDataFile.getFilename());
    }

    @Override
    public String toString() {
        return "OrderDataFileDTO{"
               + "id="
               + id
               + ", productId='"
               + productId
               + '\''
               + ", version="
               + version
               + ", downloadUrl='"
               + downloadUrl
               + '\''
               + ", mimeType="
               + mimeType
               + ", checksum='"
               + checksum
               + '\''
               + ", filesize="
               + filesize
               + ", filename='"
               + filename
               + '\''
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderDataFileDTO that = (OrderDataFileDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
