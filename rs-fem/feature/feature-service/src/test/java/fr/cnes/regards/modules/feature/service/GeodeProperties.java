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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;

import java.time.OffsetDateTime;

/**
 * @author Marc SORDI
 */
public final class GeodeProperties {

    public static void addGeodeProperties(Feature feature) {
        // System
        ObjectProperty system = IProperty.buildObject("system",
                                                      IProperty.buildInteger("filesize", 8648),
                                                      IProperty.buildString("filename", "test.tar"),
                                                      IProperty.buildString("checksum",
                                                                            "4e188bd8a6288164c25c3728ce394927"),
                                                      IProperty.buildDate("ingestion_date", OffsetDateTime.now()),
                                                      IProperty.buildDate("change_date", OffsetDateTime.now()),
                                                      IProperty.buildString("extension", "tar"));
        // SWOT
        ObjectProperty swot = IProperty.buildObject("swot",
                                                    IProperty.buildString("crid", "crid"),
                                                    IProperty.buildInteger("product_counter", 1),
                                                    IProperty.buildString("station", "KUX"),
                                                    IProperty.buildDate("day_date", OffsetDateTime.now()),
                                                    IProperty.buildInteger("pass_number", 125),
                                                    IProperty.buildInteger("tile_number", 25),
                                                    IProperty.buildString("tile_side", "Full"),
                                                    IProperty.buildString("granule_type", "Cycle"),
                                                    IProperty.buildIntegerArray("continent_id", 1),
                                                    IProperty.buildString("bassin_id", "bass1"));

        // DATA
        ObjectProperty data = IProperty.buildObject("data", IProperty.buildString("type", "L2_LR_SSH"));
        // CORPUS
        ObjectProperty corpus = IProperty.buildObject("corpus",
                                                      IProperty.buildInteger("corpus_id", 10),
                                                      IProperty.buildString("corpus_lot", "lot2"));

        feature.setProperties(IProperty.set(system, swot, corpus, data));
    }
}
