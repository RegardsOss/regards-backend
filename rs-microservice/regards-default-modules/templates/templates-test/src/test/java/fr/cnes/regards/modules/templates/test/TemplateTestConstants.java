/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.test;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants and data for testing all different templates business layers.
 *
 * @author Xavier-Alexandre Brochard
 */
public class TemplateTestConstants {

    /**
     * Id
     */
    public static final Long ID = 0L;

    /**
     * Code
     */
    public static final String CODE = "DEFAULT";

    /**
     * Content
     */
    public static final String CONTENT = "Hello $name.";

    /**
     * Data
     */
    // @formatter:off
    @SuppressWarnings("serial")
    public static final Map<String, String> DATA = new HashMap<String, String>() {{ put("name", "Defaultname");put("age", "26");put("height", "170"); }};
    // @formatter:on

    /**
     * Key for a value to store in the data map
     */
    public static final String DATA_KEY_0 = "name";

    /**
     * A value stored in the data map
     */
    public static final String DATA_VALUE_0 = "Defaultname";

    /**
     * Key for a value to store in the data map
     */
    public static final String DATA_KEY_1 = "age";

    /**
     * A value stored in the data map
     */
    public static final String DATA_VALUE_1 = "26";

    /**
     * Key for a value to store in the data map
     */
    public static final String DATA_KEY_2 = "height";

    /**
     * A value stored in the data map
     */
    public static final String DATA_VALUE_2 = "170";

    /**
     * Description
     */
    public static final String DESCRIPTON = "I'm describing what this template is good for";

}
