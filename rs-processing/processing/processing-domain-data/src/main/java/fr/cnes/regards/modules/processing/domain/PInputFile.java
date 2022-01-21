/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain;

import io.vavr.collection.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.net.URL;

/**
 * This class represents an execution input file.
 *
 * @author gandrieu
 */
@Data @With
@AllArgsConstructor
@NoArgsConstructor
public class PInputFile {

    /** Parameter name in the dynamic execution parameters for this file. */
    @Nullable String parameterName;

    /** Where to put the file once downloaded so that the execution finds it.
     * The path is relative to the 'input' workdir folder. */
    @NonNull String localRelativePath;

    /** Optional content type of the file, may determine what to do with it. */
    @Nullable String contentType;

    /** If the descriptor is a file, this is its location. */
    @NonNull URL url;

    /** File content length in bytes */
    @NonNull Long bytes;

    /** The file checksum */
    @NonNull String checksum;

    /** The original file name (can be different to the real stored file **/
    @NonNull String fileName;

    /** Free metadata corresponding to the input */
    @NonNull Map<String,String> metadata;

    /** Allows to provide some correlationId for this input file. Output files can refer to this correlationId. */
    @Nullable String inputCorrelationId;

}
