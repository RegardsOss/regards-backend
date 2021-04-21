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
package fr.cnes.regards.modules.ingest.domain;

/**
 *
 * Ingest validation messages
 * @author Marc SORDI
 *
 */
public final class IngestValidationMessages {

    // Ingest metadata messages

    public static final String MISSING_INGEST_CHAIN = "Ingest processing chain name is required";

    public static final String MISSING_SESSION_OWNER = "Identifier of the session owner that submitted the SIP is required";

    public static final String MISSING_SESSION = "Session is required";

    public static final String MISSING_STORAGE_METADATA = "Storage metadata is required";

    public static final String MISSING_CATEGORIES = "Categories are required";

    public static final String MISSING_VERSIONING_MODE = "Request versioning mode is required";

    // Common validation messages

    public static final String MISSING_REQUEST_ID = "Request identifier is required";

    public static final String MISSING_METADATA = "Ingest metadata is required";

    public static final String MISSING_SIP = "SIP is required";

    public static final String MISSING_SIPID = "SIP identifier is required";

    // Deletion message

    public static final String MISSING_SESSION_DELETION_MODE = "Session deletion mode is required";

    public static final String MISSING_SESSION_DELETION_SELECTION_MODE = "Session selection mode is required";

    private IngestValidationMessages() {
    }
}
