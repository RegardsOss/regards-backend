/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License; or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful;
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not; see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.storage.domain.dto.request;

import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Information about a file for a reference request.<br/>
 * Mandatory information are : <ul>
 * <li> Filename</li>
 * <li> Checksum</li>
 * <li> Checksum algorithm </li>
 * <li> mimeType </li>
 * <li> FileSize as the file is not accessible </li>
 * <li> Storage location where to delete the file</li>
 * <li> Owner of the file who ask for deletion </li>
 * <li> Url to access file in the storage location </li>
 * </ul>
 * See {@link ReferenceFlowItem} for more information about reference request process.
 *
 * @author Sébastien Binda
 */
public class FileReferenceRequestDTO {

    @NotBlank(message = "File name is required for FileReferenceRequest")
    private String fileName;

    @NotBlank(message = "Checksum is required for FileReferenceRequest")
    private String checksum;

    @NotBlank(message = "Algorithm is required for FileReferenceRequest")
    private String algorithm;

    @NotBlank(message = "Mimetype is required for FileReferenceRequest")
    private String mimeType;

    @NotNull
    private Long fileSize;

    private Integer height;

    private Integer width;

    @NotBlank(message = "Owner is required for FileReferenceRequest")
    private String owner;

    @NotBlank(message = "Storage is required for FileReferenceRequest")
    private String storage;

    @NotBlank(message = "URL is required for FileReferenceRequest")
    private String url;

    private String type;

    private String sessionOwner;

    private String session;

    public String getFileName() {
        return fileName;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getOwner() {
        return owner;
    }

    public String getStorage() {
        return storage;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public static FileReferenceRequestDTO build(String fileName,
                                                String checksum,
                                                String algorithm,
                                                String mimeType,
                                                Long fileSize,
                                                String owner,
                                                String storage,
                                                String url,
                                                String sessionOwner,
                                                String session) {

        Assert.notNull(fileName, "File name is mandatory.");
        Assert.notNull(checksum, "Checksum is mandatory.");
        Assert.notNull(algorithm, "Algorithm is mandatory.");
        Assert.notNull(mimeType, "MimeType is mandatory.");
        Assert.notNull(fileSize, "FileSize is mandatory.");
        Assert.notNull(owner, "Owner is mandatory.");
        Assert.notNull(storage, "Destination storage location is mandatory");
        Assert.notNull(url, "Url storage location is mandatory");

        FileReferenceRequestDTO request = new FileReferenceRequestDTO();
        request.fileName = fileName;
        request.checksum = checksum;
        request.algorithm = algorithm;
        request.mimeType = mimeType;
        request.fileSize = fileSize;
        request.owner = owner;
        request.storage = storage;
        request.url = url;
        request.sessionOwner = sessionOwner;
        request.session = session;
        return request;
    }

    public FileReferenceRequestDTO withType(String type) {
        this.type = type;
        return this;
    }

    public FileReferenceRequestDTO withHeight(Integer height) {
        this.height = height;
        return this;
    }

    public FileReferenceRequestDTO withWidth(Integer width) {
        this.width = width;
        return this;
    }

    public FileReferenceMetaInfo buildMetaInfo() {
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(checksum,
                                                                   algorithm,
                                                                   fileName,
                                                                   fileSize,
                                                                   MediaType.valueOf(mimeType));
        metaInfo.setHeight(height);
        metaInfo.setWidth(width);
        metaInfo.setType(type);
        return metaInfo;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        String fn = (fileName != null ? "fileName=" + fileName + ", " : "");
        String cs = (checksum != null ? "checksum=" + checksum + ", " : "");
        String algo = (algorithm != null ? "algorithm=" + algorithm + ", " : "");
        String mt = (mimeType != null ? "mimeType=" + mimeType + ", " : "");
        String fs = (fileSize != null ? "fileSize=" + fileSize + ", " : "");
        String h = (height != null ? "height=" + height + ", " : "");
        String w = (width != null ? "width=" + width + ", " : "");
        String ow = (owner != null ? "owner=" + owner + ", " : "");
        String sto = (storage != null ? "storage=" + storage + ", " : "");
        String u = (url != null ? "url=" + url + ", " : "");
        String t = (type != null ? "type=" + type : "");
        String so = (sessionOwner != null ? "sessionOwner=" + sessionOwner : "");
        String s = (session != null ? "session=" + session : "");
        return "FileReferenceRequestDTO [" + fn + cs + algo + mt + fs + h + w + ow + sto + u + t + so + s + "]";
    }

}
