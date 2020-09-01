package fr.cnes.regards.modules.processing.utils;

/**
 * A class with no information, with only a single instance ({@link Unit#UNIT}).
 * <br/>
 * Whereas {@link Void} is supposed to have no instance, and thus no returned value
 * (even though null is required to be used as a substitute), Unit is supposed to have
 * a value with no information: it returns something, a "proof" that the computation
 * occurred normally.
 */
public class Unit {

    // Only instance of Unit.
    public static final Unit UNIT = new Unit();

    private Unit() {}

}
