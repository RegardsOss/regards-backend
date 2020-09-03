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


package fr.cnes.regards.framework.dump;

import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.util.Assert;

/**
 *
 * @author Iliana Ghazali
 */

public class ObjectDump implements Comparable<ObjectDump> {

    private final OffsetDateTime creationDate;

    private Object jsonContent;

    private final String jsonName;

    public ObjectDump(OffsetDateTime creationDate, String jsonName, Object jsonContent) {
        Assert.notNull(creationDate, "Objects cannot be dumped without creation date, Please provide one!");
        Assert.notNull(jsonName, "Objects cannot be dumped without a proper name, Please provide one!");
        this.creationDate = creationDate;
        this.jsonName = jsonName;
        this.jsonContent = jsonContent;
    }


    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public Object getJsonContent() {
        return jsonContent;
    }

    public String getJsonName() {
        return jsonName;
    }

    @Override
    public String toString() {
        return "JsonEntity{" + "creationDate=" + creationDate.toString() + ", jsonContent='" + jsonContent + '\''
                + ", jsonName='" + jsonName + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ObjectDump that = (ObjectDump) o;
        return creationDate.equals(that.creationDate) && jsonName.equals(that.jsonName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDate, jsonName);
    }

    @Override
    public int compareTo(ObjectDump d) {
        if(Objects.equals(this, d)) {
            return 0;
        }
        return getCreationDate().compareTo(d.getCreationDate());
    }
}
