package fr.cnes.regards.modules.processing.plugins.impl;

import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.domain.PExecutionStep;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.exception.ExecutionFailureException;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterType;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdir;
import fr.cnes.regards.modules.processing.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import io.vavr.Tuple;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static fr.cnes.regards.modules.processing.domain.PExecutionStep.newStep;

@Plugin(
        id = "Arcad3IsoprobeDensiteProductPlugin",
        version = "1.0.0-SNAPSHOT",
        description = "Compute the product name from data and browse filenames",
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
public class SimpleShellProcessPlugin extends AbstractBaseForecastedProcessPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleShellProcessPlugin.class);

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

    private final IExecutionLocalWorkdirService workdirService;
    private final ISharedStorageService storageService;

    @Autowired
    public SimpleShellProcessPlugin(IExecutionLocalWorkdirService workdirService,
            ISharedStorageService storageService) {
        this.workdirService = workdirService;
        this.storageService = storageService;
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

    @Override public Seq<DataType> requiredDataTypes() {
        return io.vavr.collection.List.of(DataType.RAWDATA);
    }

    @Override public IExecutable executable() {
        return new IExecutable() {
            @Override
            public Mono<Seq<POutputFile>> execute(ExecutionContext ctx, FluxSink<PExecutionStep> stepSink) {
                NuProcessBuilder pb = new NuProcessBuilder(Arrays.asList(shellScriptName));
                pb.environment().putAll(ctx.getBatch().getUserSuppliedParameters()
                    .toJavaMap(v -> Tuple.of(v.getName(), v.getValue())));
                AtomicReference<NuProcess> nuProcessRef = new AtomicReference<>();

                return workdirService
                    .makeWorkdir(ctx)
                    .flatMap(workdir -> workdirService.writeInputFilesToWorkdirInput(workdir, ctx.getExec().getInputFiles())
                        .flatMap(unit -> {
                            return Mono.<Mono<Seq<POutputFile>>>create(sink -> {
                                try {
                                    ShellScriptNuProcessHandler handler = new ShellScriptNuProcessHandler(ctx, workdir,
                                                                                                          stepSink, sink);
                                    pb.setProcessListener(handler);
                                    pb.setCwd(workdir.getBasePath());

                                    NuProcess process = pb.start();
                                    nuProcessRef.set(process);
                                }
                                catch(Exception e) {
                                    LOGGER.error(e.getMessage(), e);
                                    sink.error(e);
                                }
                            })
                            .flatMap(mono -> mono)
                            .doOnCancel(() -> {
                                nuProcessRef.getAndUpdate(p -> { if (p != null) { p.destroy(true); } return null; });
                                stepSink.next(newStep(ctx.getExec(), ExecutionStatus.CANCELLED, ""));
                            })
                            .log("ShellScriptNuProcessHandler mono");
                        }));
            }
        };
    }


    class ShellScriptNuProcessHandler extends NuAbstractProcessHandler {

        private final ExecutionContext ctx;
        private final ExecutionLocalWorkdir workdir;
        private final FluxSink<PExecutionStep> stepSink;
        private final MonoSink<Mono<Seq<POutputFile>>> monoSink;

        private NuProcess nuProcess;

        ShellScriptNuProcessHandler(ExecutionContext ctx, ExecutionLocalWorkdir workdir, FluxSink<PExecutionStep> stepSink,
                MonoSink<Mono<Seq<POutputFile>>> monoSink) {
            this.ctx = ctx;
            this.workdir = workdir;
            this.stepSink = stepSink;
            this.monoSink = monoSink;
        }

        @Override public void onPreStart(NuProcess nuProcess) {
            LOGGER.debug("nu process prestart"); // TODO cleanup
            this.nuProcess = nuProcess;
        }
        @Override public void onStart(NuProcess nuProcess) {
            LOGGER.debug("nu process start"); // TODO cleanup
            this.stepSink.next(newStep(ctx.getExec(), ExecutionStatus.RUNNING));
        }
        @Override public void onExit(int i) {
            if (i == 0) { this.onSuccess(); }
            else { this.onFailure(i); }
        }

        private void onFailure(int i) {
            LOGGER.debug("nu process exit"); // TODO cleanup
            String message = String.format(
                "correlationId=%s exec=%s process=%s : Exited with status code %d",
                    ctx.getBatch().getCorrelationId(),
                    ctx.getExec().getId(),
                    processName,
                    i
            );
            stepSink.next(newStep(ctx.getExec(), ExecutionStatus.FAILURE, message));
            monoSink.error(new ExecutionFailureException(message));
        }

        private void onSuccess() {
            LOGGER.info("correlationId={} exec={} process={} : Exited with status code 0",
                         ctx.getBatch().getCorrelationId(),
                         ctx.getExec().getId(),
                         processName);
            stepSink.next(newStep(ctx.getExec(), ExecutionStatus.SUCCESS, "Exited with status code 0"));

            monoSink.success(storageService.storeResult(ctx, workdir));
        }

        @Override public void onStdout(ByteBuffer byteBuffer, boolean b) {
            String msg = readBytesToString(byteBuffer);
            LOGGER.info("correlationId={} exec={} process={} : {}",
                ctx.getBatch().getCorrelationId(),
                ctx.getExec().getId(),
                processName,
                msg
            );
        }
        @Override public void onStderr(ByteBuffer byteBuffer, boolean b) {
            String msg = readBytesToString(byteBuffer);
            LOGGER.error("correlationId={} exec={} process={} : {}",
                ctx.getBatch().getCorrelationId(),
                ctx.getExec().getId(),
                processName,
                msg
            );
        }
        private String readBytesToString(ByteBuffer byteBuffer) {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return new String(bytes);
        }
    }


    public void setShellScriptName(String shellScriptName) {
        this.shellScriptName = shellScriptName;
    }

    public void setEnvVariableNames(List<String> envVariableNames) {
        this.envVariableNames = envVariableNames;
    }
}
