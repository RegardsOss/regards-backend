/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.standalone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.staf.domain.STAFConfiguration;

/**
 * STAF Util standalone process main.
 * @author Sébastien Binda
 */
@ComponentScan(basePackages = "fr.cnes.regards.framework.staf")
@EnableConfigurationProperties(STAFConfiguration.class)
@PropertySource("classpath:staf.properties")
public class STAFUtil {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(STAFArchiveUtil.class);

    /**
     * Main method
     * @param args
     */
    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(STAFUtil.class);
        String modeParam = System.getProperty("mode", null);
        if (modeParam == null) {
            LOG.info("Invalid STAF Mode : {}", modeParam);
        } else {
            STAFArchiveUtil archiver = ctx.getBean(STAFArchiveUtil.class);
            STAFRetrieveUtil retriever = ctx.getBean(STAFRetrieveUtil.class);
            STAFDeleteUtil deleter = ctx.getBean(STAFDeleteUtil.class);
            switch (STAFUtilModeEnum.valueOf(modeParam)) {
                case ARCHIVE:
                    archiver.archive();
                    break;
                case RESTORE:
                    retriever.retrieve(args);
                    break;
                case DELETE:
                    deleter.delete(args);
                    break;
                default:
                    break;
            }
        }
    }
}
