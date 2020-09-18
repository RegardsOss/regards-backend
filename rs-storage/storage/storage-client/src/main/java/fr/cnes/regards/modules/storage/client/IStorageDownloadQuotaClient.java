/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.client;

import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

public interface IStorageDownloadQuotaClient {

    public static final String PATH_QUOTA = "/quota";

    public static final String PATH_CURRENT_QUOTA = "/quota/current";

    @RequestMapping(method = RequestMethod.POST, path = PATH_QUOTA)
    @ResponseBody
    ResponseEntity<DownloadQuotaLimitsDto> createQuotaLimits(@Valid @RequestBody DownloadQuotaLimitsDto toBeCreated);

    @RequestMapping(method = RequestMethod.GET, path = PATH_QUOTA)
    @ResponseBody
    ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(@Valid @PathVariable("email") String userEmail);

    @RequestMapping(method = RequestMethod.GET, path = PATH_CURRENT_QUOTA)
    @ResponseBody
    ResponseEntity<UserCurrentQuotas> getCurrentQuotas(@Valid @PathVariable("email") String userEmail);

}
