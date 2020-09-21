package fr.cnes.regards.modules.processing.demo.process;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.processing.demo.engine.event.StartWithProfileEvent;
import fr.cnes.regards.modules.processing.demo.engine.event.StepEvent;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.collection.List;

import static fr.cnes.regards.modules.processing.demo.DemoConstants.FORCE_FAILURE;

public class DemoSimulatedAsyncProcess {

    private final IPublisher publisher;

    public DemoSimulatedAsyncProcess(IPublisher publisher) {
        this.publisher = publisher;
    }

    public void send(ExecutionContext ctx, StartWithProfileEvent event) {
        String profile = event.getProfile();
        new Thread(() -> {
            try {
                Thread.sleep(500L);
                publisher.publish(new StepEvent(
                        ctx.getExec().getId(),
                        new ExecutionEvent.IntermediaryEvent(PStep.prepare("preparing...")))
                );
                Thread.sleep(500L);
                publisher.publish(new StepEvent(
                        ctx.getExec().getId(),
                        new ExecutionEvent.IntermediaryEvent(PStep.running("running with profile " + profile + "...")))
                );
                Thread.sleep(2000L);
                if (profile.equals(FORCE_FAILURE)) {
                    publisher.publish(new StepEvent(
                        ctx.getExec().getId(),
                        new ExecutionEvent.FinalEvent(
                            PStep.failure("failure for profile " + profile + "..."),
                          List.empty()
                        )
                    ));
                }
                else {
                    publisher.publish(new StepEvent(
                        ctx.getExec().getId(),
                        new ExecutionEvent.FinalEvent(
                            PStep.success("success for profile " + profile + "..."),
                            List.empty()
                        )
                    ));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
