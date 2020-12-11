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
import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.ProcessingConstants;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdir;
import io.vavr.Function1;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    protected static final Pattern KEYVALUE_PATTERN = Pattern.compile("^\\s*(?<name>[^=]+?)\\s*=(?<value>.*?)$");

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSimpleShellProcessPlugin.class);

    @PluginParameter(
            name = "shellScript",
            label = "Shell script name or absolute path",
            description = "The script must be executable and reachable by rs-processing."
    )
    protected String shellScriptName;

    @PluginParameter(
            name = "envVariables",
            label = "Environment variables to give to the shell script",
            description = "List of environment variables needed by the shell script." +
                    " Format as KEy=VALUE separated by '&', for instance:" +
                    " KEY1=value1&KEY2=value2 ",
            optional = true
    )
    protected String envVariables;

    @PluginParameter(
            name = "maxFilesInInput",
            label = "Maximum number of files as input for one execution",
            description = "This parameter allows to limit the number of files given as input." +
                    " Must be positive or null." +
                    " Set to 0 for no limit.",
            optional = true,
            defaultValue = "0"
    )
    protected long maxFilesInInput = 0;

    @PluginParameter(
            name = "requiredDataTypes",
            label = "Comma-separated list of required DataTypes",
            description = "This parameter allows to change the feature files sent as input for executions. " +
                    "By default, only RAWDATA are sent, but changing this parameter to 'RAWDATA,THUMBNAIL,AIP' " +
                    "for instance would provide RAWDATA, THUMBNAIL and AIP files.",
            optional = true,
            defaultValue = "RAWDATA"
    )
    protected String requiredDataTypes = "RAWDATA";

    public void setShellScriptName(String shellScriptName) {
        this.shellScriptName = shellScriptName;
    }

    public void setEnvVariables(String envVariables) {
        this.envVariables = envVariables;
    }

    public void setMaxFilesInInput(int maxFilesInInput) {
        this.maxFilesInInput = maxFilesInInput;
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
        return io.vavr.collection.List.empty();
    }

    @Override public IExecutable executable() {
        return sendEvent(prepareEvent())
            .andThen("prepare workdir", prepareWorkdir())
            .andThen("send running", sendEvent(runningEvent()))
            .andThen("simple shell", new SimpleShellProcessExecutable())
            .andThen("store output", storeOutputFiles())
            .andThen("clean workdir", cleanWorkdir())
            .onErrorThen(sendFailureEventThenClean());
    }

    protected Seq<DataType> requiredDataTypes() {
        return io.vavr.collection.List.of(requiredDataTypes.split(","))
            .map(String::trim)
            .flatMap(str -> Try.of(() -> DataType.valueOf(str)));
    }

    protected Function1<Throwable, IExecutable> sendFailureEvent() {
        return t -> (ctx -> ctx.sendEvent(event(failure(t.getMessage()))));
    }

    protected IExecutable failureEvent(Throwable t) {
        return sendFailureEvent().apply(t);
    }

    protected Function1<Throwable, IExecutable> sendFailureEventThenClean() {
        return t -> failureEvent(t).andThen("clean workdir", cleanWorkdir());
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
            LOGGER.error(message);
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
            if (!StringUtils.isBlank(msg)) {
                LOGGER.debug("batch={} exec={} process={} :\nstdout: {}",
                        ctx.getBatch().getId(),
                        ctx.getExec().getId(),
                        shellScriptName,
                        msg
                );
            }
        }
        @Override public void onStderr(ByteBuffer byteBuffer, boolean b) {
            String msg = readBytesToString(byteBuffer);
            if (!StringUtils.isBlank(msg)) {
                LOGGER.error("batch={} exec={} process={} :\nstderr: {}",
                        ctx.getBatch().getId(),
                        ctx.getExec().getId(),
                        shellScriptName,
                        msg
                );
            }
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
            return ctx.getParam(ExecutionLocalWorkdir.class)
                .flatMap(workdir -> executeInWorkdir(ctx, workdir));
        }

        public Mono<? extends ExecutionContext> executeInWorkdir(ExecutionContext ctx, ExecutionLocalWorkdir workdir) {

            NuProcessBuilder pb = new NuProcessBuilder(Arrays.asList(shellScriptName));
            pb.environment().putAll(parseEnvVars());

            return Mono.create(sink -> {
                try {
                    ShellScriptNuProcessHandler handler = new ShellScriptNuProcessHandler(ctx, sink);
                    pb.setProcessListener(handler);
                    pb.setCwd(workdir.getBasePath());
                    startProcess(pb);
                } catch(IOException e) {
                    LOGGER.error("The shell script appears to be missing: {}", shellScriptName, e);
                    sink.error(e);
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                    sink.error(e);
                }
            });
        }

        /**
         * This method is a wrapped around {@link NuProcessBuilder#start()}, which declares
         * the throwing of FileNotFoundException.
         * @param pb the process to start
         * @throws IOException may be thrown by the used method even though it is not declared.
         */
        private void startProcess(NuProcessBuilder pb) throws IOException {
            NuProcess start = pb.start();
            if (start == null) {
                throw new IOException("NuProcess start failed");
            }
        }
    }

    private Map<String, String> parseEnvVars() {
        return Option.of(envVariables)
            .map(s -> io.vavr.collection.List.of(s.split("\\&"))
                .map(KEYVALUE_PATTERN::matcher)
                .filter(Matcher::matches)
                .toMap(matcher -> Tuple.of(matcher.group("name"), matcher.group("value"))))
            .getOrElse(HashMap.empty())
            .toJavaMap();
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
