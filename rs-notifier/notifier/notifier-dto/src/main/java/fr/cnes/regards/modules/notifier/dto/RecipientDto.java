/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * DTO to handle the list of recipients for rest controllers.
 *
 * @author Stephane Cortine
 */
public class RecipientDto {

    @Schema(description = "Recipient business identifier")
    private final String businessId;

    @Schema(description = "Recipient label")
    private final String recipientLabel;

    @Schema(description = "Recipient description")
    private final String description;

    public RecipientDto(String businessId, String recipientLabel, String description) {
        this.businessId = businessId;
        this.recipientLabel = recipientLabel;
        this.description = description;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getRecipientLabel() {
        return recipientLabel;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RecipientDto that = (RecipientDto) o;

        if (!Objects.equals(businessId, that.businessId)) {
            return false;
        }
        if (!Objects.equals(recipientLabel, that.recipientLabel)) {
            return false;
        }
        return Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        int result = businessId != null ? businessId.hashCode() : 0;
        result = 31 * result + (recipientLabel != null ? recipientLabel.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RecipientDTO{"
               + "businessId='"
               + businessId
               + '\''
               + ", recipientLabel='"
               + recipientLabel
               + '\''
               + ", description='"
               + description
               + '\''
               + '}';
    }
}
