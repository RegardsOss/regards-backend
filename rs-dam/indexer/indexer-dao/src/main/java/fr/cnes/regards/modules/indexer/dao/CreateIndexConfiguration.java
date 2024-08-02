/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.indexer.dao;

import fr.cnes.regards.modules.dam.domain.settings.DamSettings;

/**
 * Configuration for ES index creation
 *
 * @author Thibaud Michaudel
 **/
public class CreateIndexConfiguration {

    private long numberOfShards;

    private long numberOfReplicas;

    /**
     * Default configuration using 5 shards and 1 replica for each shard.
     */
    public static final CreateIndexConfiguration DEFAULT = new CreateIndexConfiguration(DamSettings.DEFAULT_INDEX_NUMBER_OF_SHARDS,
                                                                                        DamSettings.DEFAULT_INDEX_NUMBER_OF_REPLICAS);

    /**
     * Constructor
     *
     * @param numberOfShards   number of shards in the index, this number cannot be changed after index creation
     * @param numberOfReplicas number of replicas for each shards in the index, this number cannot be changed after index creation
     */
    public CreateIndexConfiguration(long numberOfShards, long numberOfReplicas) {
        this.numberOfShards = numberOfShards;
        this.numberOfReplicas = numberOfReplicas;
    }

    public CreateIndexConfiguration() {
    }

    public long getNumberOfShards() {
        return numberOfShards;
    }

    public void setNumberOfShards(long numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    public long getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public void setNumberOfReplicas(long numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }
}
