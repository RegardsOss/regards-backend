package fr.cnes.regards.microservices.jobs;

public interface IJob<T> {

    T execute();

}
