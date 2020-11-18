package fr.cnes.regards.modules.order.service.utils;

import java.util.function.BiFunction;

public final class OrderCounts {

    // Count of files managed by Storage (internal)
    private int internalFilesCount;
    // External files count
    private int externalFilesCount;
    // Count number of subOrder created to compute expiration date
    private int subOrderCount;

    public OrderCounts(int internalFilesCount, int externalFilesCount, int subOrderCount) {
        this.internalFilesCount = internalFilesCount;
        this.externalFilesCount = externalFilesCount;
        this.subOrderCount = subOrderCount;
    }

    public OrderCounts() {
        this(0,0,0);
    }


    public OrderCounts addToInternalFilesCount(int add) {
        this.internalFilesCount += add;
        return this;
    }

    public OrderCounts addToExternalFilesCount(int add) {
        this.externalFilesCount += add;
        return this;
    }

    public OrderCounts addToSubOrderCount(int add) {
        this.subOrderCount += add;
        return this;
    }

    public void incrSubOrderCount() {
        this.addToSubOrderCount(1);
    }

    public int getInternalFilesCount() {
        return internalFilesCount;
    }

    public int getExternalFilesCount() {
        return externalFilesCount;
    }

    public int getSubOrderCount() {
        return subOrderCount;
    }

    public static OrderCounts initial() {
        return new OrderCounts();
    }

    public static OrderCounts add(OrderCounts one, OrderCounts two) {
        return new OrderCounts(
                one.internalFilesCount + two.internalFilesCount,
                one.externalFilesCount + two.externalFilesCount,
                one.subOrderCount + two.subOrderCount
        );
    }
}
