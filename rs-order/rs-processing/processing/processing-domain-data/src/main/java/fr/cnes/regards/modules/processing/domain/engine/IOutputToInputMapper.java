package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.collection.List;

import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.io.FilenameUtils.removeExtension;

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
    static IOutputToInputMapper noMapping() { return (ctx, out) -> List.empty(); }

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

}
