/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.dao;

import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractScriptGeneratorTest;

/**
 *
 * @See {@link AbstractScriptGeneratorTest}
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "regards.jpa.instance.embedded=true", "regards.jpa.instance.embeddedPath=target" })
public class ScriptGeneratorTest extends AbstractScriptGeneratorTest {

}
