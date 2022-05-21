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

package fr.cnes.regards.modules.ingest.service;

import com.google.common.collect.Lists;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utils to publish random SIPs
 *
 * @author Iliana Ghazali
 */
public class TestData {

    public static final List<String> STORAGES = Lists.newArrayList("AWS",
                                                                   "Azure",
                                                                   "IBM",
                                                                   "Oracle",
                                                                   "Google",
                                                                   "Pentagon",
                                                                   "NASA");

    public static final List<String> SESSION_OWNERS = Lists.newArrayList("CNES",
                                                                         "NASA",
                                                                         "ESA",
                                                                         "JAXA",
                                                                         "Roscosmos",
                                                                         "ISRO",
                                                                         "CNSA",
                                                                         "ASI",
                                                                         "CSA");

    public static List<String> getRandomCategories() {
        List randomCategories = Lists.newArrayList();
        int nbCategories = ThreadLocalRandom.current().nextInt(1, 6);
        for (int i = 0; i <= nbCategories; i++) {
            randomCategories.add("CATEGORY" + "_" + String.format("%04d", i));
        }
        return randomCategories;
    }

    public static List<String> getRandomTags() {
        List randomTags = Lists.newArrayList();
        int nbTags = ThreadLocalRandom.current().nextInt(1, 6);
        for (int i = 0; i <= nbTags; i++) {
            randomTags.add("TAG" + "_" + String.format("%04d", i));
        }
        return randomTags;
    }

    public static List<String> getRandomStorage() {
        List randomStorages = Lists.newArrayList();
        int nbStorages = ThreadLocalRandom.current().nextInt(1, STORAGES.size());
        Collections.shuffle(STORAGES);
        for (int i = 0; i <= nbStorages; i++) {
            randomStorages.add(STORAGES.get(i));
        }
        return randomStorages;
    }

    public static String getRandomSessionOwner() {
        return SESSION_OWNERS.get(ThreadLocalRandom.current().nextInt(0, SESSION_OWNERS.size()));
    }

    public static String getRandomSession() {
        return OffsetDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 365)).toString();
    }

}
