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
package fr.cnes.regards.modules.order.domain.dto;

import fr.cnes.regards.modules.order.domain.basket.Basket;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class BasketDto {

    private Long id;

    private String owner;

    private SortedSet<BasketDatasetSelectionDto> datasetSelections;

    private Long quota;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public SortedSet<BasketDatasetSelectionDto> getDatasetSelections() {
        return this.datasetSelections;
    }

    public void setDatasetSelections(SortedSet<BasketDatasetSelectionDto> datasetSelections) {
        this.datasetSelections = datasetSelections;
    }

    public Long getQuota() {
        return quota;
    }

    public void setQuota(Long quota) {
        this.quota = quota;
    }

    public static BasketDto makeBasketDto(Basket basket) {
        BasketDto dto = new BasketDto();
        dto.setId(basket.getId());
        dto.setOwner(basket.getOwner());
        dto.setDatasetSelections(basket.getDatasetSelections()
                                       .stream()
                                       .map(BasketDatasetSelectionDto::makeBasketDatasetSelectionDto)
                                       .collect(TreeSet::new, Set::add, Set::addAll));
        dto.setQuota(dto.getDatasetSelections().stream().mapToLong(BasketDatasetSelectionDto::getQuota).sum());
        return dto;
    }
}
