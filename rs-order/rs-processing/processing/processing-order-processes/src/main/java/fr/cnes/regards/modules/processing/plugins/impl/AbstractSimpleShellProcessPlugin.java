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
package fr.cnes.regards.modules.processing.plugins.impl;

import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcessBuilder;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.processing.ProcessingConstants;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterType;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdir;
import io.vavr.Function2;
import io.vavr.Tuple;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static fr.cnes.regards.modules.processing.domain.PStep.*;
import static fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent.event;
import static fr.cnes.regards.modules.processing.domain.engine.IExecutable.sendEvent;
import static io.vavr.collection.List.ofAll;

/**
 * This class is a sample plugin launching a shell script.
 *
 * @author gandrieu
 */
public abstract class AbstractSimpleShellProcessPlugin extends AbstractBaseForecastedStorageAwareProcessPlugin {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSimpleShellProcessPlugin.class);

    @PluginParameter(
            name = "shellScript",
            label = "Shell script name or absolute path",
            description = "The script must be executable and reachable by rs-processing."
    )
    protected String shellScriptName;

    @PluginParameter(
            name = "envVarNames",
            label = "Environment variable names",
            description = "List of the names of the environment variables needed to be set by the user when creating the batch.",
            optional = true
    )
    protected List<String> envVariableNames;

    public void setShellScriptName(String shellScriptName) {
        this.shellScriptName = shellScriptName;
    }

    public void setEnvVariableNames(List<String> envVariableNames) {
        this.envVariableNames = envVariableNames;
    }

    @Override public ConstraintChecker<PBatch> batchChecker() {
        return ConstraintChecker.noViolation();
    }

    @Override public ConstraintChecker<PExecution> executionChecker() {
        return ConstraintChecker.noViolation();
    }

    @Override public String engineName() {
        return ProcessingConstants.Engines.JOBS;
    }

    @Override public Seq<ExecutionParameterDescriptor> parameters() {
        return emptyOrAllImmutable(envVariableNames)
            .map(name -> new ExecutionParameterDescriptor(
                name,
                ExecutionParameterType.STRING,
                "The " + name + " env variable for the script",
                false,
                false,
                true
            ));
    }

    @Override public IExecutable executable() {
        return sendEvent(prepareEvent())
            .andThen(prepareWorkdir()
                .andThen(sendEvent(runningEvent())
                    .andThen(new SimpleShellProcessExecutable()
                        .andThen(storeOutputFiles()
                            .andThen(cleanWorkdir()))))
                .onError(failureEvent())
            );
    }

    protected Function2<ExecutionContext, Throwable, Mono<ExecutionContext>> failureEvent() {
        return (ctx, t) -> ctx.sendEvent(event(failure(t.getMessage())));
    }

    protected Function<ExecutionContext, ExecutionEvent> runningEvent() {
        return ctx -> event(running(String.format("Launch script %s | execId=%s", shellScriptName, ctx.getExec().getId())));
    }

    protected Function<ExecutionContext, ExecutionEvent> prepareEvent() {
        return ctx -> event(prepare(String.format("Load input files into workdir | execId=" + ctx.getExec().getId())));
    }

    protected <T> io.vavr.collection.List<T> empty() {
        return io.vavr.collection.List.empty();
    }

    class ShellScriptNuProcessHandler extends NuAbstractProcessHandler {

        protected final ExecutionContext ctx;
        protected final MonoSink<ExecutionContext> sink;

        ShellScriptNuProcessHandler(
                ExecutionContext ctx,
                MonoSink<ExecutionContext> sink
        ) {
            this.ctx = ctx;
            this.sink = sink;
        }

        @Override public void onExit(int i) {
            if (i == 0) { this.onSuccess(); }
            else { this.onFailure(i); }
        }

        protected void onFailure(int i) {
            String message = String.format(
                "correlationId=%s exec=%s process=%s : Exited with status code %d",
                    ctx.getBatch().getCorrelationId(),
                    ctx.getExec().getId(),
                    shellScriptName,
                    i
            );
            sink.error(new SimpleShellProcessExecutionException(message));
        }

        protected void onSuccess() {
            LOGGER.info("batch={} exec={} process={} : Exited with status code 0",
                ctx.getBatch().getId(),
                ctx.getExec().getId(),
                shellScriptName
            );
            sink.success(ctx);
        }

        @Override public void onStdout(ByteBuffer byteBuffer, boolean b) {
            String msg = readBytesToString(byteBuffer);
            LOGGER.info("batch={} exec={} process={} :\n{}",
                ctx.getBatch().getId(),
                ctx.getExec().getId(),
                shellScriptName,
                msg
            );
        }
        @Override public void onStderr(ByteBuffer byteBuffer, boolean b) {
            String msg = readBytesToString(byteBuffer);
            LOGGER.error("batch={} exec={} process={} :\n{}",
                ctx.getBatch().getId(),
                ctx.getExec().getId(),
                shellScriptName,
                msg
            );
        }
        protected String readBytesToString(ByteBuffer byteBuffer) {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return new String(bytes);
        }
    }

    class SimpleShellProcessExecutable implements IExecutable {

        @Override
        public Mono<ExecutionContext> execute(ExecutionContext ctx) {
            return getWorkdirService()
                .makeWorkdir(ctx.getExec())
                .flatMap(workdir -> executeInWorkdir(ctx, workdir));
        }

        public Mono<? extends ExecutionContext> executeInWorkdir(ExecutionContext ctx, ExecutionLocalWorkdir workdir) {
            Map<String, String> envVarsMap = ctx
                    .getBatch()
                    .getUserSuppliedParameters()
                    .toJavaMap(v -> Tuple.of(v.getName(), v.getValue()));

            NuProcessBuilder pb = new NuProcessBuilder(Arrays.asList(shellScriptName));
            pb.environment().putAll(envVarsMap);

            return Mono.create(sink -> {
                try {
                    ShellScriptNuProcessHandler handler = new ShellScriptNuProcessHandler(ctx, sink);
                    pb.setProcessListener(handler);
                    pb.setCwd(workdir.getBasePath());
                    pb.start();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    sink.error(e);
                }
            });
        }
    }

    public static class SimpleShellProcessExecutionException extends Exception {
        public SimpleShellProcessExecutionException(String s) {
            super(s);
        }
    }

    protected static io.vavr.collection.List<String> emptyOrAllImmutable(List<String> strings) {
        return strings == null ? io.vavr.collection.List.empty() : ofAll(strings);
    }
}
