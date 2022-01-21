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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.modules.order.domain.OrderDataFile;

import java.io.OutputStream;
import java.util.List;

public interface IOrderDownloadService {

    /**
     * Create a ZIP containing all currently available files. Once a file has been part of ZIP file, it will not be
     * part of another again.
     *
     * @param orderOwner  order owner
     * @param inDataFiles concerned order data files
     * @param os
     */
    void downloadOrderCurrentZip(String orderOwner, List<OrderDataFile> inDataFiles, OutputStream os);

    /**
     * Create a metalink file with all files.
     *
     * @param orderId concerned order id
     * @param os
     */
    void downloadOrderMetalink(Long orderId, OutputStream os);

}
