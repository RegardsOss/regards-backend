/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.templates.test;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants and data for testing all different templates business layers.
 * @author Xavier-Alexandre Brochard
 */
public final class TemplateTestConstants {

    /**
     * The templates endpoint
     */
    public static final String API_TEMPLATES = "/templates";

    /**
     * The specific template endpoint
     */
    public static final String API_TEMPLATES_TEMPLATE_ID = "/templates/{template_id}";

    /**
     * Code
     */
    public static final String CODE = "DEFAULT";

    /**
     * Content
     */
    public static final String CONTENT = "Hello ${name}. You are ${age} years old and ${height} m tall.";

    /**
     * Content with values replaced
     */
    public static String CONTENT_TEMPLATED = "Hello Defaultname. You are 26 years old and 1.79 m tall.";

    /**
     * Subject
     */
    public static final String SUBJECT = "Subject of a templated mail";

    /**
     * Data
     */
    // @formatter:off
    @SuppressWarnings("serial")
    public static final Map<String, String> DATA = new HashMap<String, String>() {{ put("name", "Defaultname");put("age", DATA_VALUE_1);put("height", DATA_VALUE_2); }};
    // @formatter:on

    /**
     * Key for a value to store in the data map
     */
    public static final String DATA_KEY_0 = "name";

    /**
     * Key for a value to store in the data map
     */
    public static final String DATA_KEY_1 = "age";

    /**
     * Key for a value to store in the data map
     */
    public static final String DATA_KEY_2 = "height";

    /**
     * A value stored in the data map
     */
    public static final String DATA_VALUE_0 = "Defaultname";

    /**
     * A value stored in the data map
     */
    public static final String DATA_VALUE_1 = "26";

    /**
     * A value stored in the data map
     */
    public static final String DATA_VALUE_2 = "1.79";

    /**
     * Description
     */
    public static final String DESCRIPTON = "I'm describing what this template is good for";

    /**
     * A recipient for the mail
     */
    public static final String RECIPIENT_0 = "email@test.com";

    /**
     * A recipient for the mail
     */
    public static final String RECIPIENT_1 = "otheremail@test.com";

    /**
     * The recipients as an array
     */
    public static final String[] RECIPIENTS = { RECIPIENT_0, RECIPIENT_1 };

    /**
     * Id
     */
    public static final Long ID = 0L;

    /**
     * Use this field to represent an inexistent id in db
     */
    public static final Long WRONG_ID = 99L;

    private TemplateTestConstants() {

    }

}
