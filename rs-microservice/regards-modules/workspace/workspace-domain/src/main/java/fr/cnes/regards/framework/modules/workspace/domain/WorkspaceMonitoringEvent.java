package fr.cnes.regards.framework.modules.workspace.domain;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.WorkerMode;

/**
 * This event is used to indicate that workspace monitoring should be run for a tenant.
 * Workspace monitoring being required on the tenant workspace and not on the workspace of a microservice for a tenant,
 * we use this event so only one microservice will monitor execute the monitoring
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(mode = WorkerMode.UNICAST, target = Target.ALL)
public class WorkspaceMonitoringEvent implements ISubscribable {

}
