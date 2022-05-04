/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.domain.download;

import org.springframework.core.io.InputStreamResource;

import java.io.InputStream;

/**
 * DTO that represents the failure of the download of a file from the catalog.
 * It contains the stream of the error raised by the storage service
 *
 * @author Thomas Fache
 **/
public class FailedDownload extends InputStreamResource implements Download {

    /**
     * Construtor of the download failure DTO
     *
     * @param errorStream the error stream contained
     */
    public FailedDownload(InputStream errorStream) {
        super(errorStream);
    }
}
