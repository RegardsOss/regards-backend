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
package fr.cnes.regards.modules.processing.demo.process;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.processing.demo.engine.event.StartWithProfileEvent;
import fr.cnes.regards.modules.processing.demo.engine.event.StepEvent;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fr.cnes.regards.modules.processing.demo.DemoConstants.FORCE_FAILURE;

/**
 * This class is the demo async launch of a process.
 *
 * @author gandrieu
 */
public class DemoSimulatedAsyncProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoSimulatedAsyncProcess.class);

    private final IPublisher publisher;

    public DemoSimulatedAsyncProcess(IPublisher publisher) {
        this.publisher = publisher;
    }

    public void send(ExecutionContext ctx, StartWithProfileEvent event) {
        new Thread(() -> {
            String profile = event.getProfile();
            try {
                Thread.sleep(500L);
                publisher.publish(new StepEvent(ctx.getExec().getId(),
                        new ExecutionEvent.IntermediaryEvent(PStep.prepare("preparing..."))));
                Thread.sleep(500L);
                publisher.publish(new StepEvent(ctx.getExec().getId(), new ExecutionEvent.IntermediaryEvent(
                        PStep.running("running with profile " + profile + "..."))));
                Thread.sleep(2000L);
                if (profile.equals(FORCE_FAILURE)) {
                    publisher.publish(new StepEvent(ctx.getExec().getId(), new ExecutionEvent.FinalEvent(
                            PStep.failure("failure for profile " + profile + "..."), List.empty())));
                } else {
                    publisher.publish(new StepEvent(ctx.getExec().getId(), new ExecutionEvent.FinalEvent(
                            PStep.success("success for profile " + profile + "..."), List.empty())));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Something unexpected happened during the simulation.", e);
            }
        }).start();
    }
}
