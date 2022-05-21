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
package fr.cnes.regards.modules.storage.rest;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.domain.dto.cache.CacheDTO;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to provide information about cache system
 *
 * @author sbinda
 */
@RestController
@RequestMapping(CacheController.CACHE_PATH)
public class CacheController {

    public static final String CACHE_PATH = "/cache";

    @Autowired
    private CacheService cacheService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Download one file by checksum.", role = DefaultRole.EXPLOIT)
    public ResponseEntity<CacheDTO> getOccupation() {
        return new ResponseEntity<CacheDTO>(CacheDTO.build(cacheService.getCacheSizeLimit(),
                                                           cacheService.getCacheSizeUsedBytes()), HttpStatus.OK);
    }

}
