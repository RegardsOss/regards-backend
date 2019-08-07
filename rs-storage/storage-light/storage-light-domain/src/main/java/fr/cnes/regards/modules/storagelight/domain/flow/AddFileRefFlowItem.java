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
package fr.cnes.regards.modules.storagelight.domain.flow;

import java.net.URL;
import java.util.Optional;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;

/**
 * @author Sébastien Binda
 *
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class AddFileRefFlowItem implements ISubscribable {

    private String fileName;

    private String checksum;

    private String algorithm;

    private String mimeType;

    private Long fileSize;

    private String owner;

    private String type;

    private FileLocation destination;

    private Optional<URL> originUrl = Optional.empty();

    public AddFileRefFlowItem() {
        super();
    }

    private AddFileRefFlowItem(String fileName, String checksum, String algorithm, String mimeType, String owner,
            String destinationStorage) {
        super();
        Assert.notNull(fileName, "File name is mandatory.");
        Assert.notNull(checksum, "Checksum is mandatory.");
        Assert.notNull(algorithm, "Algorithm is mandatory.");
        Assert.notNull(mimeType, "MimeType is mandatory.");
        Assert.notNull(owner, "Owner is mandatory.");
        Assert.notNull(destinationStorage, "Destination storage location is mandatory");
        this.fileName = fileName;
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.mimeType = mimeType;
        this.owner = owner;
        this.destination = new FileLocation(destinationStorage, null);
    }

    public AddFileRefFlowItem withType(String type) {
        this.setType(type);
        return this;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FileLocation getDestination() {
        return destination;
    }

    public void setDestination(FileLocation destination) {
        this.destination = destination;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Optional<URL> getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(URL originUrl) {
        this.originUrl = Optional.ofNullable(originUrl);
    }

    /**
     * Build a {@link AddFileRefFlowItem} to request reference of a new file. No file movement is requested here.
     * @param fileName
     * @param checksum
     * @param algorithm
     * @param mimeType
     * @param fileSize
     * @param owner
     * @param storage Name of the destination storage location
     * @param url location of the file to reference in the destination storage
     * @return new {@link AddFileRefFlowItem}
     */
    public static AddFileRefFlowItem build(String fileName, String checksum, String algorithm, String mimeType,
            Long fileSize, String owner, String storage, String url) {
        AddFileRefFlowItem item = new AddFileRefFlowItem(fileName, checksum, algorithm, mimeType, owner, storage);
        item.storeIn(url);
        item.setFileSize(fileSize);
        return item;
    }

    /**
     * Build a {@link AddFileRefFlowItem} to request storage of a new file. Storage copy the file from originUrl to destination storage.
     * To define a sub directory where to copy file in destination storage, use {@link AddFileRefFlowItem#storeIn(String)}}
     * @param fileName
     * @param checksum
     * @param algorithm
     * @param mimeType
     * @param fileSize
     * @param owner
     * @param destinationStorage
     * @param originUrl
     * @return new {@link AddFileRefFlowItem}
     */
    public static AddFileRefFlowItem build(String fileName, String checksum, String algorithm, String mimeType,
            String owner, String destinationStorage, URL originUrl) {
        AddFileRefFlowItem item = new AddFileRefFlowItem(fileName, checksum, algorithm, mimeType, owner,
                destinationStorage);
        item.setOriginUrl(originUrl);
        return item;
    }

    /**
     * Define a sub directory where to copy file in destination storage.
     * @param subdirectory
     * @return updated current {@link AddFileRefFlowItem}
     */
    public AddFileRefFlowItem storeIn(String subdirectory) {
        this.getDestination().setUrl(subdirectory);
        return this;
    }

}
