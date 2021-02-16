/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.indexer.domain;

import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.urn.DataType;

/**
 * The unique intend of this interface is to avoid calling IEsRepository.computeDataFilesSummary() on data that do not
 * specify an attribute "files" whose type is Multimap&lt;String, DataFile> (or more precisely a multimap with a key of
 * type "something that is serialized into String")
 * @author oroussel
 */
public interface IDocFiles {

    /**
     * List of related files. Use
     * {@link DataFile#build(fr.cnes.regards.framework.oais.urn.DataType, String, String, org.springframework.util.MimeType, Boolean)}
     */
    Multimap<DataType, DataFile> getFiles();
}
