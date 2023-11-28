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

import fr.cnes.regards.modules.filecatalog.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.filecatalog.dto.quota.UserCurrentQuotasDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

public interface IStorageDownloadQuotaRestClient {

    String PATH_USER_QUOTA = "/quota/{user_email}";

    String PATH_QUOTA = "/quota";

    String PATH_QUOTA_LIST = "/quotas";

    String PATH_CURRENT_QUOTA = "/quota/current";

    String PATH_USER_CURRENT_QUOTA = "/quota/current/{user_email}";

    String PATH_CURRENT_QUOTA_LIST = "/quota/currents";

    String PATH_MAX_QUOTA = "/quota/max";

    String USER_EMAIL_PARAM = "user_email";

    @GetMapping(path = PATH_USER_QUOTA,
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(@PathVariable(USER_EMAIL_PARAM) String userEmail);

    @PutMapping(path = PATH_USER_QUOTA,
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(@PathVariable(USER_EMAIL_PARAM) String userEmail,
                                                             @Valid @RequestBody DownloadQuotaLimitsDto quotaLimits);

    @GetMapping(path = PATH_QUOTA_LIST,
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<DownloadQuotaLimitsDto>> getQuotaLimits(
        @RequestParam(value = USER_EMAIL_PARAM) String[] userEmails);

    @GetMapping(path = PATH_QUOTA,
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits();

    @GetMapping(path = PATH_CURRENT_QUOTA,
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas();

    @GetMapping(path = PATH_USER_CURRENT_QUOTA,
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas(@PathVariable(USER_EMAIL_PARAM) String userEmail);

    @GetMapping(path = PATH_QUOTA,
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Long> getMaxQuota();

    @PostMapping(path = PATH_CURRENT_QUOTA_LIST,
                 produces = MediaType.APPLICATION_JSON_VALUE,
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<UserCurrentQuotasDto>> getCurrentQuotasList(@Valid @RequestBody String[] userEmails);
}
