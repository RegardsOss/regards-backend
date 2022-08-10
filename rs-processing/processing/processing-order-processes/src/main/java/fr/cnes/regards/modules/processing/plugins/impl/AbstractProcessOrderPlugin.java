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
package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.ProcessingConstants;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IOutputToInputMapper;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.order.Cardinality;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;
import fr.cnes.regards.modules.processing.order.Scope;
import fr.cnes.regards.modules.processing.order.SizeLimit;
import io.vavr.Function1;
import io.vavr.collection.Seq;
import io.vavr.control.Try;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static fr.cnes.regards.modules.processing.domain.PStep.*;
import static fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent.event;

/**
 * Common plugin for processes specific to rs-order
 *
 * @author gandrieu
 * @author Iliana Ghazali
 **/
public abstract class AbstractProcessOrderPlugin extends AbstractBaseForecastedStorageAwareProcessPlugin {

    private static final String EVENT_FORMATTING = "[%s] ExecId=%s | %s";

    // @formatter:off
    @PluginParameter(
        name = "requiredDataTypes",
        label = "Comma-separated list of required DataTypes",
        description = """
                      This parameter allows changing the product files sent as input for executions.
                      By default, only RAWDATA are sent, but changing this parameter to 'RAWDATA,THUMBNAIL,AIP'
                      for instance would provide RAWDATA, THUMBNAIL and AIP files.
                      """,
        optional = true,
        defaultValue = "RAWDATA")
    protected String requiredDataTypes = "RAWDATA";

    @PluginParameter(
        name = "scope",
        label = "Scope",
        description = """
                      This parameter defines how many executions are launched per suborder.
                      The possible values are: SUBORDER, FEATURE.
                      If the value is SUBORDER, there is only one execution per suborder,
                      allowing to group several features in the same execution.
                      If the value is FEATURE, there is one execution per feature in the suborder,
                      allowing to isolate each feature in its own execution context.
                      """,
        optional = true,
        defaultValue = "SUBORDER")
    protected String scope = "SUBORDER";

    @PluginParameter(
        name = "cardinality",
        label = "Cardinality of output files",
        description = """
                      This parameter defines how many output files are created.
                      The possible values are: ONE_PER_EXECUTION, ONE_PER_FEATURE, ONE_PER_INPUT_FILE.
                      If the value is ONE_PER_EXECUTION, only one output file will be generated.
                      If the value is ONE_PER_FEATURE, one output file will be generated for each corresponding feature given as input.
                      If the value is ONE_PER_INPUT_FILE, one output file will be generated for each corresponding file given as input.
                      """,
        optional = true,
        defaultValue = "ONE_PER_FEATURE")
    protected String cardinality = "ONE_PER_FEATURE";


    @PluginParameter(
        name = "maxFeaturesInInput",
        label = "Maximum number of features in input for one execution",
        description = """
                      This parameter allows to limit the number of features given as input.
                      Must be positive or null. Set to 0 for no limit.
                      Warning: It is not recommended to put no limitation when the parameter "forbidSplitInSuborders"
                      is activated to avoid any memory overload.
                      """,
        optional = true,
        defaultValue = "0")
    protected long maxFeaturesInInput = 0L;

    @PluginParameter(
        name = "forbidSplitInSubOrders",
        label = "Forbid an order to be split into multiple orders.",
        description = """
                      This parameter is used to keep the consistency of a processing by forbidding the split of the order into multiple suborders.
                      ALL the ordered products will be processed in the same batch.
                      It is recommended to configure thoroughly "maxFeaturesAsInput" to avoid any memory overload.
                      """,
        optional = true,
        defaultValue = "false")
    protected boolean forbidSplitInSuborders;
    // @formatter:on

    @Override
    public IOutputToInputMapper inputOutputMapper() {
        return switch (Cardinality.valueOf(cardinality)) {
            case ONE_PER_INPUT_FILE -> IOutputToInputMapper.sameNameWithoutExt();
            case ONE_PER_FEATURE -> IOutputToInputMapper.sameParent();
            case ONE_PER_EXECUTION -> IOutputToInputMapper.allMappings();
        };
    }

    @Override
    public OrderProcessInfo processInfo() {
        return new OrderProcessInfo(Scope.valueOf(scope),
                                    Cardinality.valueOf(cardinality),
                                    getRequiredDataTypes().toList(),
                                    new SizeLimit(SizeLimit.Type.FEATURES, this.maxFeaturesInInput),
                                    this.sizeForecast().get(),
                                    this.forbidSplitInSuborders);
    }

    protected int correctSize(ExecutionContext ctx) {
        return switch (Cardinality.valueOf(this.cardinality)) {
            case ONE_PER_EXECUTION -> 1;
            case ONE_PER_INPUT_FILE -> ctx.getExec().getInputFiles().size();
            case ONE_PER_FEATURE -> countFeatures(ctx.getExec().getInputFiles());
        };
    }

    @Override
    public ConstraintChecker<PBatch> batchChecker() {
        return ConstraintChecker.noViolation();
    }

    @Override
    public ConstraintChecker<PExecution> executionChecker() {
        return ConstraintChecker.noViolation();
    }

    @Override
    public String engineName() {
        return ProcessingConstants.Engines.JOBS;
    }

    @Override
    public Seq<ExecutionParameterDescriptor> parameters() {
        return io.vavr.collection.List.empty();
    }

    private int countFeatures(Seq<PInputFile> inputFiles) {
        return getDistinctFeatureIds(inputFiles).size();
    }

    protected Seq<DataType> getRequiredDataTypes() {
        return io.vavr.collection.List.of(requiredDataTypes.split(","))
                                      .map(String::trim)
                                      .flatMap(str -> Try.of(() -> DataType.valueOf(str)));
    }

    protected Function1<Throwable, IExecutable> sendFailureEvent() {
        return t -> (ctx -> ctx.sendEvent(event(failure(String.format(EVENT_FORMATTING,
                                                                      getClass().getSimpleName(),
                                                                      ctx.getExec().getId(),
                                                                      "Error message: \"" + t.getMessage() + "\"")))));
    }

    protected IExecutable failureEvent(Throwable t) {
        return sendFailureEvent().apply(t);
    }

    protected Function1<Throwable, IExecutable> sendFailureEventThenClean() {
        return t -> failureEvent(t).andThen("clean workdir", cleanWorkdir());
    }

    protected Function<ExecutionContext, ExecutionEvent> prepareEvent(String prepareMsg) {
        return ctx -> event(prepare(String.format(EVENT_FORMATTING,
                                                  getClass().getSimpleName(),
                                                  ctx.getExec().getId(),
                                                  prepareMsg)));
    }

    protected Function<ExecutionContext, ExecutionEvent> runningEvent(String runMsg) {
        return ctx -> event(running(String.format(EVENT_FORMATTING,
                                                  getClass().getSimpleName(),
                                                  ctx.getExec().getId(),
                                                  runMsg)));
    }

    protected Function<ExecutionContext, ExecutionEvent> pendingEvent(String pendingMsg) {
        return ctx -> event(pending(String.format(EVENT_FORMATTING,
                                                  getClass().getSimpleName(),
                                                  ctx.getExec().getId(),
                                                  pendingMsg)));
    }

    protected IExecutable sendResultBasedOnOutputFileCount() {
        return ctx -> ctx.getParam(CumulativeOutputFiles.class)
                         .map(CumulativeOutputFiles::getOutFiles)
                         .flatMap(outFiles -> {
                             int size = outFiles.size();
                             int correctSize = correctSize(ctx);
                             if (size == correctSize) {
                                 return ctx.sendEvent(event(success(""), outFiles));
                             } else {
                                 String message = String.format("Wrong number of output files: expected %d, got %d",
                                                                correctSize,
                                                                size);
                                 return ctx.sendEvent(event(failure(message), outFiles));
                             }
                         })
                         .onErrorResume(t -> ctx.sendEvent(event(failure(t.getMessage()))))
                         .switchIfEmpty(Mono.defer(() -> ctx.sendEvent(event(failure("No output files found")))));
    }

    public void setRequiredDataTypes(String requiredDataTypes) {
        this.requiredDataTypes = requiredDataTypes;
    }

    public void setScope(Scope scope) {
        this.scope = scope.toString();
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality.toString();
    }

    public void setMaxFeaturesInInput(long maxFeaturesInInput) {
        this.maxFeaturesInInput = maxFeaturesInInput;
    }

    public void setForbidSplitInSuborders(boolean forbidSplitInSuborders) {
        this.forbidSplitInSuborders = forbidSplitInSuborders;
    }
}
