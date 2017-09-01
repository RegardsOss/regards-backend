package fr.cnes.regards.modules.order.domain;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractReliantTask;

/**
 * @author oroussel
 */
@Entity
@Table(name = "t_order_task")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "fk_task_id"))
public class OrderTask extends AbstractReliantTask<OrderTask> {

}
