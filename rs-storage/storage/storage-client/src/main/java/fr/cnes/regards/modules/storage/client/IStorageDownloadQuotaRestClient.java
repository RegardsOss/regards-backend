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
package fr.cnes.regards.modules.storage.client;

import javax.validation.Valid;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import fr.cnes.regards.framework.modules.tenant.settings.client.IDynamicTenantSettingClient;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;

public interface IStorageDownloadQuotaRestClient {

    String PATH_USER_QUOTA = "/quota/{user_email}";

    String PATH_QUOTA = "/quota";

    String PATH_QUOTA_LIST = "/quotas";

    String PATH_CURRENT_QUOTA = "/quota/current";

    String PATH_USER_CURRENT_QUOTA = "/quota/current/{user_email}";

    String PATH_CURRENT_QUOTA_LIST = "/quota/currents";

    String USER_EMAIL_PARAM = "user_email";

    @GetMapping(path = PATH_USER_QUOTA, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(@PathVariable(USER_EMAIL_PARAM) String userEmail);

    @PutMapping(path = PATH_USER_QUOTA, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(@PathVariable(USER_EMAIL_PARAM) String userEmail,
            @Valid @RequestBody DownloadQuotaLimitsDto quotaLimits);

    @GetMapping(path = PATH_QUOTA_LIST, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<DownloadQuotaLimitsDto>> getQuotaLimits(
            @RequestParam(value = USER_EMAIL_PARAM) String[] userEmails);

    @GetMapping(path = PATH_QUOTA, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits();

    @GetMapping(path = PATH_CURRENT_QUOTA, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<UserCurrentQuotas> getCurrentQuotas();

    @GetMapping(path = PATH_USER_CURRENT_QUOTA, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<UserCurrentQuotas> getCurrentQuotas(@PathVariable(USER_EMAIL_PARAM) String userEmail);

    @PostMapping(path = PATH_CURRENT_QUOTA_LIST, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<UserCurrentQuotas>> getCurrentQuotasList(@Valid @RequestBody String[] userEmails);
}
