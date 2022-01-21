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
package fr.cnes.regards.modules.indexer.domain.criterion;

/**
 * String specialized AbstractMatchCriterion
 *
 * @author oroussel
 * @author Marc SORDI
 */
public class StringMatchCriterion extends AbstractMatchCriterion<String> {

    /**
     * See {@link StringMatchType} for explanation
     */
    private final StringMatchType matchType;

    public StringMatchCriterion(String name, MatchType type, String value, StringMatchType matchType) {
        super(name, type, value);
        this.matchType = matchType;
    }

    @Override
    public StringMatchCriterion copy() {
        return new StringMatchCriterion(super.name, super.type, super.value, matchType);
    }

    @Override
    public <U> U accept(ICriterionVisitor<U> visitor) {
        return visitor.visitStringMatchCriterion(this);
    }

    public StringMatchType getMatchType() {
        if (matchType == null) {
            // this is to handle migration issues with former criterion in BD
            return StringMatchType.KEYWORD;
        } else {
            return matchType;
        }
    }
}
