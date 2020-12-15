/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.collection.List;

import java.nio.file.Paths;

import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.io.FilenameUtils.removeExtension;

/**
 * This interface defines how to find the input files corresponding to a
 * given output file.
 *
 * {@link POutputFile} instances may refer to the correlation ID of one or more {@link PInputFile}
 * that are thus supposed to have contributed to generate the output. (There is no
 * generic way to link output back to inputs, so the list of input correlation IDs may be empty.)
 *
 * @author gandrieu
 */
public interface IOutputToInputMapper {

    /**
     * Given an output, provide all the inputs that were related to it.
     * @param ctx the execution context
     * @param out the output file
     * @return the inputs related to the output
     */
    List<PInputFile> mapInputs(ExecutionContext ctx, POutputFile out);

    default POutputFile mapInputCorrelationIds(ExecutionContext ctx, POutputFile out) {
        List<String> newCids = mapInputs(ctx, out).map(PInputFile::getInputCorrelationId);
        List<String> cids = out.getInputCorrelationIds().appendAll(newCids);
        return out.withInputCorrelationIds(cids);
    }

    /** Provides an input/output mapper that never maps to anything.
     * Some processes may not need to map outputs to inputs. */
    static IOutputToInputMapper noMapping() {
        return (ctx, out) -> List.empty();
    }


    /** Provides an input/output mapper that always maps to everything.
     * Some processes take all inputs and generate outputs from all of them. */
    static IOutputToInputMapper allMappings() {
        return (ctx, out) -> List.ofAll(ctx.getExec().getInputFiles());
    }


    /** Provides an input/output mapper which looks at the file names without extension. */
    static IOutputToInputMapper sameNameWithoutExt() {
        return (ctx, out) -> {
            String outputClean = removeExtension(out.getName());
            return ctx.getExec().getInputFiles().filter(input -> {
                String inputClean = removeExtension(getName(input.getLocalRelativePath()));
                return inputClean.equals(outputClean);
            }).toList();
        };
    }

    /** Provides an input/output mapper which looks at the parent folder. */
    static IOutputToInputMapper sameParent() {
        return (ctx, out) -> {
            String outputParent = parentName(out.getName());
            return ctx.getExec().getInputFiles().filter(input -> {
                String inputParent = parentName(input.getLocalRelativePath());
                return inputParent.equals(outputParent);
            }).toList();
        };
    }

    static String parentName(String name) {
        return Paths.get(name).getParent().getFileName().toString();
    }


}
