/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain;

/**
 *
 * Ingest validation messages
 * @author Marc SORDI
 *
 */
public final class IngestValidationMessages {

    // Ingest metadata messages

    public static final String MISSING_INGEST_CHAIN_ERROR = "Ingest processing chain name is required";

    public static final String MISSING_SESSION_OWNER_ERROR = "Identifier of the session owner that submitted the SIP is required";

    public static final String MISSING_SESSION_ERROR = "Session is required";

    public static final String MISSING_STORAGE_METADATA_ERROR = "Storage metadata is required";

    // Common validation messages

    public static final String MISSING_REQUEST_ID_ERROR = "Request identifier is required";

    public static final String MISSING_METADATA_ERROR = "Ingest metadata is required";

    public static final String MISSING_SIP_ERROR = "SIP is required";

    private IngestValidationMessages() {
    }
}
