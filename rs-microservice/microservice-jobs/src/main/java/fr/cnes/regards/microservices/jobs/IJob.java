package fr.cnes.regards.microservices.jobs;

@FunctionalInterface
public interface IJob<T> {

    T execute();

}
