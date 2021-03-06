package fr.cnes.regards.modules.accessrights.domain.projects.events;

/**
 * Constants used to send AMQP event to microservices. Those constants state what changement is being applied
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public enum ProjectUserAction {
    CREATE, DELETE, UPDATE
}
