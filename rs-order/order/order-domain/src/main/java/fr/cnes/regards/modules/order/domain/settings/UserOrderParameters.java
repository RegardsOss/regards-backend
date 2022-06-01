package fr.cnes.regards.modules.order.domain.settings;

import java.util.Objects;

public class UserOrderParameters {

    private int subOrderDuration;

    private int delayBeforeEmailNotification;

    public UserOrderParameters() {
    }

    public UserOrderParameters(int subOrderDuration, int delayBeforeEmailNotification) {
        this.subOrderDuration = subOrderDuration;
        this.delayBeforeEmailNotification = delayBeforeEmailNotification;
    }

    public int getSubOrderDuration() {
        return subOrderDuration;
    }

    public UserOrderParameters setSubOrderDuration(int subOrderDuration) {
        this.subOrderDuration = subOrderDuration;
        return this;
    }

    public int getDelayBeforeEmailNotification() {
        return delayBeforeEmailNotification;
    }

    public UserOrderParameters setDelayBeforeEmailNotification(int delayBeforeEmailNotification) {
        this.delayBeforeEmailNotification = delayBeforeEmailNotification;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserOrderParameters)) {
            return false;
        }
        UserOrderParameters that = (UserOrderParameters) o;
        return subOrderDuration == that.subOrderDuration
               && delayBeforeEmailNotification == that.delayBeforeEmailNotification;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subOrderDuration, delayBeforeEmailNotification);
    }

    @Override
    public String toString() {
        return "UserOrderParameters{"
               + "subOrderDuration="
               + subOrderDuration
               + ", delayBeforeEmailNotification="
               + delayBeforeEmailNotification
               + '}';
    }

}
