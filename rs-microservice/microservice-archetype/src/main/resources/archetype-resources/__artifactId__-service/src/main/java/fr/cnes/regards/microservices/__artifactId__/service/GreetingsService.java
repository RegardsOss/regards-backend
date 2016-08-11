package fr.cnes.regards.microservices.${artifactId}.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import fr.cnes.regards.microservices.myMicroServicePlugin.domain.Greeting;

@Service
public class GreetingsService implements IGreetingsService {

    private static final String template = "Hello, %s!";

    private final AtomicLong counter = new AtomicLong();

    @Override
    public Greeting getGreeting(String pName) {
        return new Greeting(counter.incrementAndGet(), String.format(template, pName));
    }

}