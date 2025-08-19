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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.indexer.dao.scripts;

import java.io.IOException;

/**
 * Script to upsert a feature in Elasticsearch.
 * This script erase all fields, but preserves calculated fields like 'metadata', 'creationDate', 'groups' and 'datasetModelName'.
 * The existing tags that start with "URN:AIP:DATASET" are preserved, and the script
 * merges them with new tags provided.
 * Note : Limitation of the algorithm: if a user add a custom tag that starts with URN:AIP:DATASET, it can never be removed.
 *
 * @author tguillou
 */
public class UpsertDataObjectEsScript extends AbstractEsScript {

    public static final String ID = "upsertDataObject";

    public UpsertDataObjectEsScript() throws IOException {
        super(ID, "es-scripts/upsertDataObject.painless");
    }
}
