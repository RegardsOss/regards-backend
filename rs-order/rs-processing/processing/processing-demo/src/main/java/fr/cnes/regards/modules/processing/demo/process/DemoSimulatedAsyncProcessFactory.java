package fr.cnes.regards.modules.processing.demo.process;

import fr.cnes.regards.framework.amqp.IPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DemoSimulatedAsyncProcessFactory {

    private final IPublisher publisher;

    @Autowired
    public DemoSimulatedAsyncProcessFactory(IPublisher publisher) {
        this.publisher = publisher;
    }

    public DemoSimulatedAsyncProcess make() {
        return new DemoSimulatedAsyncProcess(publisher);
    }
}
