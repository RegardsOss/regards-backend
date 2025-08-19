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
 * Script to update groups and dataset associations in Elasticsearch.
 * 1. add tags
 * 2. remove existing groups and dataset associations
 * 3. add new groups and dataset associations
 * The script removes old groups to make sure rules or access have not changed.
 *
 * @author tguillou
 */
public class UpdateGroupsAndDatasetAssociationEsScript extends AbstractEsScript {

    public static final String ID = "updateGroupsAndDatasetAssociation";

    public UpdateGroupsAndDatasetAssociationEsScript() throws IOException {
        super(ID, "es-scripts/updateGroupsAndDatasetAssociations.painless");
    }
}
