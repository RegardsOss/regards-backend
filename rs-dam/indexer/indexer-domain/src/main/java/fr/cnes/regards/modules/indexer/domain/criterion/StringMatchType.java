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
package fr.cnes.regards.modules.indexer.domain.criterion;

/**
 * Programmatic string matching behavior<br/>
 * Only client knows how to match string values.
 * <p>
 * For String type fields, Elasticsearch is configured with two Lucene indexes:
 * - Full text search CASE INSENSITIVE index
 * - Keyword search CASE SENSITIVE index (field suffixed with .keyword according to our elastic field mapping)
 *
 * @author Marc SORDI
 */
public enum StringMatchType {

    // String matching relies on text type index (look at elastic standard analyser) : tokenized text, case insensitive
    FULL_TEXT_SEARCH("text"), // String matching relies on keyword type index : whole text, case sensitive
    KEYWORD("keyword");

    /**
     * This value is used to detect and extract string match type per field from input query parameters
     */
    private final String matchTypeValue;

    StringMatchType(String matchTypeValue) {
        this.matchTypeValue = matchTypeValue;
    }

    public String getMatchTypeValue() {
        return matchTypeValue;
    }
}
