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
 * Object used to dump data
 * @author Iliana Ghazali
 * @author Sylvain VISSIERE-GUERINET
 */

public class ObjectDump implements Comparable<ObjectDump> {

    private final OffsetDateTime creationDate;

    private Object jsonContent;

    private final String jsonName;

    private final String dumpId;

    public ObjectDump(OffsetDateTime creationDate, String jsonName, Object jsonContent, String aipId) {
        Assert.notNull(creationDate, "Objects cannot be dumped without creation date, Please provide one.");
        Assert.notNull(jsonName, "Objects cannot be dumped without a proper name, Please provide one.");
        Assert.notNull(aipId, "Objects cannot be dumped without a an object id, Please provide one.");
        this.creationDate = creationDate;
        this.jsonName = jsonName;
        this.jsonContent = jsonContent;
        this.dumpId = aipId;
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

    public String getDumpId() {
        return dumpId;
    }

    @Override
    public String toString() {
        return "ObjectDump{" + "creationDate=" + creationDate + ", jsonContent=" + jsonContent + ", jsonName='"
                + jsonName + '\'' + ", aipId='" + dumpId + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ObjectDump objectDump = (ObjectDump) o;
        return this.creationDate.equals(objectDump.creationDate) && this.jsonName.equals(objectDump.jsonName)
                && this.dumpId.equals(dumpId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDate, jsonName, dumpId);
    }

    @Override
    public int compareTo(ObjectDump d) {
        if (Objects.equals(this, d)) {
            return 0;
        }
        return getCreationDate().compareTo(d.getCreationDate());
    }
}
