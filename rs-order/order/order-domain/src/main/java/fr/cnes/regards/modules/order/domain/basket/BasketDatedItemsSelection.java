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
package fr.cnes.regards.modules.order.domain.basket;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Dated items selection
 *
 * @author oroussel
 * @author Sébastien Binda
 */
@Embeddable
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class BasketDatedItemsSelection implements Comparable<BasketDatedItemsSelection> {

    @Column(nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime date;

    /**
     * Selection request
     */
    @Valid
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "selection_request")
    private BasketSelectionRequest selectionRequest;

    @Column(name = "objects_count")
    private int objectsCount = 0;

    @Column(name = "files_count")
    private final long filesCount = 0;

    @Column(name = "files_size")
    private final long filesSize = 0;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "file_types_sizes")
    private StringToLongMap fileTypesSizes;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "file_types_count")
    private StringToLongMap fileTypesCount;

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public BasketSelectionRequest getSelectionRequest() {
        return selectionRequest;
    }

    public void setSelectionRequest(BasketSelectionRequest selectionRequest) {
        this.selectionRequest = selectionRequest;
    }

    public int getObjectsCount() {
        return objectsCount;
    }

    public void setObjectsCount(int objectsCount) {
        this.objectsCount = objectsCount;
    }

    public Long getFileTypeSize(String fileType) {
        return Optional.ofNullable(fileTypesSizes).map(m -> m.getOrDefault(fileType, 0L)).orElse(0L);
    }

    public void setFileTypeSize(String fileType, Long filesSize) {
        if (fileTypesSizes == null) {
            fileTypesSizes = new StringToLongMap();
        }
        this.fileTypesSizes.put(fileType, filesSize);
    }

    public Long getFileTypeCount(String fileType) {
        return Optional.ofNullable(fileTypesCount).map(m -> m.getOrDefault(fileType, 0L)).orElse(0L);
    }

    public void setFileTypeCount(String fileType, Long filesCount) {
        if (fileTypesCount == null) {
            fileTypesCount = new StringToLongMap();
        }
        this.fileTypesCount.put(fileType, filesCount);
    }

    public StringToLongMap getFileTypesSizes() {
        return fileTypesSizes;
    }

    public StringToLongMap getFileTypesCount() {
        return fileTypesCount;
    }

    public BasketDatedItemsSelection setFileTypesSizes(StringToLongMap fileTypesSizes) {
        this.fileTypesSizes = fileTypesSizes;
        return this;
    }

    public BasketDatedItemsSelection setFileTypesCount(StringToLongMap fileTypesCount) {
        this.fileTypesCount = fileTypesCount;
        return this;
    }

    @Override
    public int compareTo(BasketDatedItemsSelection o) {
        return date.compareTo(o.date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        BasketDatedItemsSelection that = (BasketDatedItemsSelection) o;

        return date != null ? date.equals(that.date) : that.date == null;
    }

    @Override
    public int hashCode() {
        return date != null ? date.hashCode() : 0;
    }
}
