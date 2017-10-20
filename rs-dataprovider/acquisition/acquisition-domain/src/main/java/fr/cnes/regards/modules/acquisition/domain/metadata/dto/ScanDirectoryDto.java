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

import org.springframework.beans.BeanUtils;

import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;

/**
 * {@link ScanDirectory} Dto
 * 
 * @author Christophe Mertz
 *
 */
public class ScanDirectoryDto {

    private Long id;

    private String scanDir;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScanDir() {
        return scanDir;
    }

    public void setScanDir(String scanDir) {
        this.scanDir = scanDir;
    }

    public static ScanDirectoryDto fromScanDirectory(ScanDirectory scanDirectory) {
        ScanDirectoryDto dto = new ScanDirectoryDto();
        BeanUtils.copyProperties(scanDirectory, dto);
        return dto;
    }

}
