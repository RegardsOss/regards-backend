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
package fr.cnes.regards.framework.modules.dump.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

/**
 *
 * @author Iliana Ghazali
 */

public class TestData {

    public static ArrayList<ObjectDump> buildJsonCollection(int numOfJson) {
        ArrayList<ObjectDump> jsonList = new ArrayList<>();
        int numDate;

        ArrayList<OffsetDateTime> dateSet = getDateSet();

        for (int i = 0; i < numOfJson; i++) {
            numDate = i % 4;
            ContentObject contentObject = new ContentObject();
            contentObject.setContent("This is the content of example " + i);
            jsonList.add(new ObjectDump(dateSet.get(numDate), "json" + i, contentObject, Integer.toString(i)));
        }

        return jsonList;
    }

    public static ArrayList<ObjectDump> buildDuplicatedJsonCollection(int numOfJson) {
        ArrayList<ObjectDump> jsonList = new ArrayList<>();
        int numDate;
        ArrayList<OffsetDateTime> dateSet = getDateSet();

        for (int i = 0; i < numOfJson; i++) {
            ContentObject contentObject = new ContentObject();
            contentObject.setContent("This is the content of example " + 0);
            jsonList.add(new ObjectDump(dateSet.get(0), "json" + 0, contentObject, Integer.toString(i)));
        }
        return jsonList;
    }

    public static ArrayList<OffsetDateTime> getDateSet() {
        ArrayList<OffsetDateTime> dateSet = new ArrayList<>();
        dateSet.add(OffsetDateTime.of(2019, 1, 31, 15, 15, 50, 345875000, ZoneOffset.of("+02:00")));
        dateSet.add(OffsetDateTime.of(2019, 1, 31, 0, 0, 55, 345875000, ZoneOffset.of("+06:00")));
        dateSet.add(OffsetDateTime.of(1980, 4, 9, 20, 12, 45, 345875000, ZoneOffset.of("+01:00")));
        dateSet.add(OffsetDateTime.of(2020, 12, 22, 23, 11, 55, 345875000, ZoneOffset.of("+07:00")));
        return dateSet;
    }

    public static class ContentObject {

        private String content;

        public void setContent(String content) {
            this.content = content;
        }
    }

}