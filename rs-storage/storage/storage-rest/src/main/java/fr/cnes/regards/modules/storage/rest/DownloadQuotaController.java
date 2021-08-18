/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

@RestController
public class DownloadQuotaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadQuotaController.class);

    public static final String PATH_DEFAULT_QUOTA = "/quota/defaults";

    public static final String PATH_USER_QUOTA = "/quota/{user_email}";

    public static final String PATH_QUOTA_LIST = "/quotas";

    public static final String PATH_QUOTA = "/quota";

    public static final String PATH_USER_CURRENT_QUOTA = "/quota/current/{user_email}";

    public static final String PATH_CURRENT_QUOTA = "/quota/current";

    public static final String PATH_CURRENT_QUOTA_LIST = "/quota/currents";

    public static final String USER_EMAIL_PARAM = "user_email";

    @Autowired
    private IQuotaService<ResponseEntity<StreamingResponseBody>> quotaService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    @GetMapping(path = PATH_USER_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get user download quota limits.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(
        @PathVariable(USER_EMAIL_PARAM) String userEmail
    ) {
        return quotaService.getDownloadQuotaLimits(userEmail)
            .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
            .get();
    }

    @PutMapping(path = PATH_USER_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Update user download quota limits.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(
        @PathVariable(USER_EMAIL_PARAM) String userEmail,
        @Valid @RequestBody DownloadQuotaLimitsDto quotaLimits
    ) {
        if (!Objects.equals(userEmail, quotaLimits.getEmail())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return quotaService.upsertDownloadQuotaLimits(quotaLimits)
            .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
            .get();
    }

    @GetMapping(path = PATH_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get current user download quota limits.", role = DefaultRole.PUBLIC)
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits() {
        return quotaService.getDownloadQuotaLimits(authResolver.getUser())
            .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
            .get();
    }

    @GetMapping(path = PATH_QUOTA_LIST)
    @ResponseBody
    @ResourceAccess(description = "Get download quota limits for the specified users.", role = DefaultRole.PUBLIC)
    public ResponseEntity<List<DownloadQuotaLimitsDto>> getQuotaLimits(@RequestParam(value = USER_EMAIL_PARAM) String[] userEmails) {
        return quotaService.getDownloadQuotaLimits(userEmails)
            .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
            .get();
    }

    @GetMapping(path = PATH_CURRENT_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get current download quota values for current user.", role = DefaultRole.PUBLIC)
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas() {
        return new ResponseEntity<>(
            quotaService.getCurrentQuotas(authResolver.getUser()),
            HttpStatus.OK
        );
    }

    @GetMapping(path = PATH_USER_CURRENT_QUOTA)
    @ResponseBody
    @ResourceAccess(description = "Get user download quota limits.", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas(
        @PathVariable(USER_EMAIL_PARAM) String userEmail
    ) {
        return new ResponseEntity<>(
            quotaService.getCurrentQuotas(userEmail),
            HttpStatus.OK
        );
    }

    @PostMapping(path = PATH_CURRENT_QUOTA_LIST)
    @ResponseBody
    @ResourceAccess(description = "Get current download quota values for the specified users.", role = DefaultRole.ADMIN)
    public ResponseEntity<List<UserCurrentQuotas>> getCurrentQuotasList(@Valid @RequestBody String[] userEmails) {
        return quotaService.getCurrentQuotas(userEmails)
            .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
            .get();
    }
}
