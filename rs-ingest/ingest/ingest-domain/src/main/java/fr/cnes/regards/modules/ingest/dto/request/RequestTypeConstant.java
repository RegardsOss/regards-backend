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
package fr.cnes.regards.modules.ingest.dto.request;

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;

/**
 * {@link AbstractRequest} types.
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 */
public class RequestTypeConstant {

    private RequestTypeConstant() {
    }

    public static final String UPDATE_VALUE = "UPDATE";

    public static final String AIP_UPDATES_CREATOR_VALUE = "AIP_UPDATES_CREATOR";

    public static final String INGEST_VALUE = "INGEST";

    public static final String OAIS_DELETION_CREATOR_VALUE = "OAIS_DELETION_CREATOR";

    public static final String OAIS_DELETION_VALUE = "OAIS_DELETION";

    public static final String AIP_POST_PROCESS_VALUE = "AIP_POST_PROCESS";

    public static final String AIP_SAVE_METADATA_VALUE = "AIP_SAVE_METADATA";
}