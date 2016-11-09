/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

/**
 * @author svissier
 *
 */
public class Couple<T, V> {

    private T first;

    private V second;

    public Couple() {
        // default constructor
    }

    public Couple(T t, V v) {
        this.first = t;
        this.second = v;
    }

    public T getFirst() {
        return first;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public V getSecond() {
        return second;
    }

    public void setSecond(V second) {
        this.second = second;
    }

}
