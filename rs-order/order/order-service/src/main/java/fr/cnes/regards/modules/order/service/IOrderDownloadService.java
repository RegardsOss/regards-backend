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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.exception.TooManyDownloadException;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface IOrderDownloadService {

    /**
     * Create a ZIP containing all currently available files. Once a file has been part of ZIP file, it will not be
     * part of another again.
     *
     * @param orderOwner  order owner
     * @param inDataFiles concerned order data files
     */
    List<OrderDataFile> downloadOrderCurrentZip(String orderOwner, List<OrderDataFile> inDataFiles, OutputStream os)
        throws IOException;

    /**
     * Create a metalink file with all files.
     *
     * @param orderId concerned order id
     */
    void downloadOrderMetalink(Long orderId, OutputStream os) throws ModuleException;

    /**
     * Try to lock an order download.
     * If a download is already processing for the given orderId, a exception is thronw;
     */
    void lockDownloadOrder(String tenant, @NonNull String user, Long orderId, String orderLabel)
        throws TooManyDownloadException;

    /**
     * Unlock an order download processing.
     */
    void unlockDownloadOrder(String tenant, Long orderId);

}
