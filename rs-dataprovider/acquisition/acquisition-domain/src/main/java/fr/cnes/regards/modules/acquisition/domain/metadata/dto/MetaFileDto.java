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
package fr.cnes.regards.modules.acquisition.domain.metadata.dto;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeanUtils;

import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;

/**
 * {@link MetaFile} Dto
 * 
 * @author Christophe Mertz
 *
 */
public class MetaFileDto {

    private Long id;

    private Boolean mandatory;

    private String fileNamePattern;

    private Set<ScanDirectoryDto> scanDirectories = new HashSet<ScanDirectoryDto>();

    private String invalidFolder;

    private String fileType;

    private String comment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    public Set<ScanDirectoryDto> getScanDirectories() {
        return scanDirectories;
    }

    public void setScanDirectories(Set<ScanDirectoryDto> scanDirectories) {
        this.scanDirectories = scanDirectories;
    }

    public String getInvalidFolder() {
        return invalidFolder;
    }

    public void addScanDirectory(ScanDirectoryDto scanDirectoryDto) {
        scanDirectories.add(scanDirectoryDto);
    }

    public void setInvalidFolder(String invalidFolder) {
        this.invalidFolder = invalidFolder;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public static MetaFileDto fromMetaFile(MetaFile metaFile) {
        MetaFileDto dto = new MetaFileDto();
        BeanUtils.copyProperties(metaFile, dto, "scanDirectories");

        for (ScanDirectory scanDir : metaFile.getScanDirectories()) {
            dto.addScanDirectory(ScanDirectoryDto.fromScanDirectory(scanDir));
        }

        return dto;
    }

}