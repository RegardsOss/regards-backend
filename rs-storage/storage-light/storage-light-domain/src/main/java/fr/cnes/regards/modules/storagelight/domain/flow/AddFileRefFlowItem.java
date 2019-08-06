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

    private FileLocation origin;

    private FileLocation destination;

    public AddFileRefFlowItem(String fileName, String checksum, String algorithm, String mimeType, Long fileSize,
            String owner, String origineStorage, String origineUrl, String destinationStorage,
            String destinationDirectory) {
        super();
        Assert.notNull(fileName, "File name is mandatory.");
        Assert.notNull(checksum, "Checksum is mandatory.");
        Assert.notNull(algorithm, "Algorithm is mandatory.");
        Assert.notNull(mimeType, "MimeType is mandatory.");
        Assert.notNull(fileSize, "FileSize is mandatory.");
        Assert.notNull(owner, "Owner is mandatory.");
        this.fileName = fileName;
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.owner = owner;
        this.origin = new FileLocation(origineStorage, origineUrl);
        this.destination = new FileLocation(destinationStorage, destinationDirectory);
    }

    public AddFileRefFlowItem(String fileName, String checksum, String algorithm, String mimeType, Long fileSize,
            String owner, String origineStorage, String origineUrl, String destinationStorage) {
        this(fileName, checksum, algorithm, mimeType, fileSize, owner, origineStorage, origineUrl, destinationStorage,
             null);
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

    public FileLocation getOrigin() {
        return origin;
    }

    public void setOrigin(FileLocation origine) {
        this.origin = origine;
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

}
