package fr.cnes.regards.framework.modules.dump.rest;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.modules.dump.service.settings.IDumpSettingsService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * This controller manages dump settings
 * @author Iliana Ghazali
 */

@RestController
@RequestMapping(DumpController.TYPE_MAPPING)
public class DumpController {

    public static final String TYPE_MAPPING = "/dumps";

    @Autowired
    IDumpSettingsService dumpSettingsService;

    /**
     * Controller path to reset lastDumpDate
     */
    public static final String RESET_LAST_DUMP_DATE = "/reset";

    @RequestMapping(path = RESET_LAST_DUMP_DATE, method = RequestMethod.PATCH)
    @ResourceAccess(description = "Reset last dump date", role = DefaultRole.ADMIN)
    public void resetLastDumpDate() {
        dumpSettingsService.resetLastDumpDate();
    }
}