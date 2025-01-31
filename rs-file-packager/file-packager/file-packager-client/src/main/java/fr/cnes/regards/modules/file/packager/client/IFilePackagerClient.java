/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.file.packager.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Rest client to access file-packager actions
 *
 * @author Thibaud Michaudel
 **/
@RestClient(name = "rs-file-packager", contextId = "rs-file-packager.config.client")
public interface IFilePackagerClient {

    String BASE_PATH = "/file-packager";

    String SCHEDULE_COMPLETE_PACKAGE_PATH = "/storeCompletePackages";

    @PostMapping(path = BASE_PATH + SCHEDULE_COMPLETE_PACKAGE_PATH)
    ResponseEntity<Void> scheduleCompletePackage();

}
