package fr.cnes.regards.modules.processing.plugins.impl;

import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterType;
import fr.cnes.regards.modules.processing.order.*;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdir;
import io.vavr.Function2;
import io.vavr.Tuple;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static fr.cnes.regards.modules.processing.domain.PStep.failure;
import static fr.cnes.regards.modules.processing.domain.PStep.running;
import static fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent.event;
import static fr.cnes.regards.modules.processing.domain.engine.IExecutable.sendEvent;
import static io.vavr.collection.List.ofAll;

@Plugin(
        id = SimpleShellProcessPlugin.SIMPLE_SHELL_PROCESS_PLUGIN,
        version = "1.0.0-SNAPSHOT",
        description = "Launch a shell script",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss",
        markdown =
            "This plugin provides a fully customizable way to launch shell scripts.\n" +
            "\n" +
            "However, the shell scripts must conform to the following conventions:\n" +
            "\n" +
            "- the script must be executable and available in the PATH of the rs-processing instance,\n" +
            "  or be given as an absolute path (in which case the full path must be accessible by the\n" +
            "  java process launching rs-processing)\n" +
            "- the script is invoked directly, with no command line arguments\n" +
            "- all script parameters are set through environment variables, whose names are defined\n" +
            "  in the plugin configuration, and set once and for all at the batch creation\n" +
            "- the script is executed from a specific workdir for each execution, containing:\n" +
            "    + an `input` folder with all the input files for the execution\n" +
            "    + an empty `output` folder where the script must create all the output files\n" +
            "- the script terminates with code 0 in case of success, any other code in case of failure\n" +
            "- the script does not use the standard input\n" +
            "- the script outputs its logs in the standard output\n" +
            "- if the script uses executables, they must be installed, reachable and executable by the process" +
            "  launching the rs-processing instance."
)
public class SimpleShellProcessPlugin extends AbstractBaseForecastedStorageAwareProcessPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleShellProcessPlugin.class);

    public static final String SIMPLE_SHELL_PROCESS_PLUGIN = "SimpleShellProcessPlugin";

    @PluginParameter(
            name = "shellScript",
            label = "Shell script name or absolute path",
            description = "The script must be executable and reachable by rs-processing."
    )
    private String shellScriptName;

    @PluginParameter(
            name = "envVarNames",
            label = "Environment variable names",
            description = "List of the names of the environment variables needed to be set by the user when creating the batch.",
            optional = true
    )
    private List<String> envVariableNames;

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
        return "JOBS";
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

    @Override public Map<String, String> processInfo() {
        OrderProcessInfo pi = new OrderProcessInfo(
            Scope.ITEM,
            Cardinality.ONE_PER_INPUT_FILE,
            io.vavr.collection.List.of(DataType.RAWDATA),
            new SizeLimit(SizeLimit.Type.NO_LIMIT, 0L)
        );
        return new OrderProcessInfoMapper().toMap(pi);
    }

    @Override public IExecutable executable() {
        return sendEvent(prepareEvent())
            .andThen(prepareWorkdir())
            .andThen(sendEvent(runningEvent()))
            .andThen(new SimpleShellProcessExecutable())
            .andThen(storeOutputFiles())
            .onError(failureEvent());
    }

    private Function2<ExecutionContext, Throwable, Mono<ExecutionContext>> failureEvent() {
        return (ctx, t) -> ctx.sendEvent(() -> event(failure(t.getMessage())));
    }

    private Supplier<ExecutionEvent> runningEvent() {
        return () -> event(running("Launch script"));
    }

    private Supplier<ExecutionEvent> prepareEvent() {
        return () -> event(PStep.prepare("Load input file to workdir"));
    }

    private <T> io.vavr.collection.List<T> empty() {
        return io.vavr.collection.List.empty();
    }



    class ShellScriptNuProcessHandler extends NuAbstractProcessHandler {

        private final ExecutionContext ctx;
        private final MonoSink<ExecutionContext> sink;

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

        private void onFailure(int i) {
            String message = String.format(
                "correlationId=%s exec=%s process=%s : Exited with status code %d",
                    ctx.getBatch().getCorrelationId(),
                    ctx.getExec().getId(),
                    shellScriptName,
                    i
            );
            sink.error(new SimpleShellProcessExecutionException(message));
        }

        private void onSuccess() {
            LOGGER.info("batch={} exec={} process={} : Exited with status code 0",
                ctx.getBatch().getId(),
                ctx.getExec().getId(),
                shellScriptName
            );
            sink.success(ctx);
        }

        @Override public void onStdout(ByteBuffer byteBuffer, boolean b) {
            String msg = readBytesToString(byteBuffer);
            LOGGER.info("batch={} exec={} process={} : {}",
                ctx.getBatch().getId(),
                ctx.getExec().getId(),
                shellScriptName,
                msg
            );
        }
        @Override public void onStderr(ByteBuffer byteBuffer, boolean b) {
            String msg = readBytesToString(byteBuffer);
            LOGGER.error("batch={} exec={} process={} : {}",
                ctx.getBatch().getId(),
                ctx.getExec().getId(),
                shellScriptName,
                msg
            );
        }
        private String readBytesToString(ByteBuffer byteBuffer) {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return new String(bytes);
        }
    }

    class SimpleShellProcessExecutable implements IExecutable {

        @Override
        public Mono<ExecutionContext> execute(ExecutionContext ctx) {
            return getWorkdirService().makeWorkdir(ctx.getExec())
                .flatMap(workdir -> executeInWorkdir(ctx, workdir));
        }

        public Mono<? extends ExecutionContext> executeInWorkdir(ExecutionContext ctx, ExecutionLocalWorkdir workdir) {
            NuProcessBuilder pb = new NuProcessBuilder(Arrays.asList(shellScriptName));
            pb.environment().putAll(ctx.getBatch().getUserSuppliedParameters()
                                            .toJavaMap(v -> Tuple.of(v.getName(), v.getValue())));
            AtomicReference<NuProcess> nuProcessRef = new AtomicReference<>();

            IExecutionEventNotifier eventNotifier = ctx.getEventNotifier();

            return Mono.create(sink -> {
                try {
                    ShellScriptNuProcessHandler handler = new ShellScriptNuProcessHandler(ctx, sink);
                    pb.setProcessListener(handler);
                    pb.setCwd(workdir.getBasePath());

                    NuProcess process = pb.start();
                    nuProcessRef.set(process);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    eventNotifier.apply(event(failure(e.getMessage())));
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
