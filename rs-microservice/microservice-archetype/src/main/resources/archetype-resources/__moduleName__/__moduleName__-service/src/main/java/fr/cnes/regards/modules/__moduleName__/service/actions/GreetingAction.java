package fr.cnes.regards.modules.${moduleName}.service.actions;

import java.util.concurrent.atomic.AtomicLong;

import fr.cnes.regards.microservices.jobs.IJob;
import fr.cnes.regards.modules.${moduleName}.domain.Greeting;

public class GreetingAction implements IJob<Greeting> {

    private static final String template = "Hello, %s!";

    private final AtomicLong counter = new AtomicLong();

    private final String name_;

    public GreetingAction(String pName) {
        super();
        name_ = pName;
    }

    @Override
    public Greeting execute() {
        return new Greeting(counter.incrementAndGet(), String.format(template, name_));
    }

}
