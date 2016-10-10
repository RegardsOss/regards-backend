/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils;

/**
 * ISamplePlugin
 * 
 * @author cmertz
 *
 */
public interface ISamplePlugin {

    /**
     * constant suffix
     */
    public static final String SUFFIXE = "suffix";

    /**
     * constant is active
     */
    public static final String ACTIVE = "isActive";

    /**
     * constant coeff
     */
    public static final String COEFF = "coeff";
    
    /**
     * constant coeff
     */
    public static final String PLG = "plg";

    /**
     * method echo
     * 
     * @param pMessage
     *            message to display
     * 
     * @return the message
     */
    String echo(String pMessage);

    /**
     * method add
     * 
     * @param pFist
     *            first element
     * @param pSecond
     *            second item
     * @return the result
     */
    int add(int pFist, int pSecond);

}
