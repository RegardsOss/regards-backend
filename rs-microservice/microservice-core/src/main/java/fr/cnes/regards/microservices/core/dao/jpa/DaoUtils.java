/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.jpa;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import fr.cnes.regards.microservices.core.dao.annotation.InstanceEntity;
import fr.cnes.regards.microservices.core.dao.exceptions.MultiDataBasesException;

/**
 *
 * Class DaoUtils
 *
 * Tools class for DAO
 *
 * @author CS
 * @since TODO
 */
@Component()
public class DaoUtils {

    /**
     * Package to scan for DAO Entities and Repositories
     */
    public static final String PACKAGES_TO_SCAN = "fr.cnes.regards";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DaoUtils.class);

    /**
     *
     * This method check that the classPatch is valid. That the scan packages for instance database and the projects
     * database are not in conflict.
     *
     * @since 1.0-SNAPSHOT
     */
    @PostConstruct
    public void checkClassPath() {

        LOG.info("Checking classpath for conflicts between instance and projects databases ...");

        final List<Class<?>> instanceClasses = DaoUtils.scanForJpaPackages(PACKAGES_TO_SCAN, InstanceEntity.class,
                                                                           null);
        final List<Class<?>> projectsClasses = DaoUtils.scanForJpaPackages(DaoUtils.PACKAGES_TO_SCAN, Entity.class,
                                                                           InstanceEntity.class);
        final List<String> instancePackages = new ArrayList<>();
        instanceClasses.forEach(instanceClass -> instancePackages.add(instanceClass.getPackage().getName()));
        final List<String> projectPackages = new ArrayList<>();
        projectsClasses.forEach(projectClass -> projectPackages.add(projectClass.getPackage().getName()));
        instancePackages.forEach(instancePackage -> {
            if (projectPackages.contains(instancePackage)) {
                final String message = "Invalid classpath. Package " + instancePackage
                        + " is used for instance DAO Entities and projects DAO Entities";
                throw new MultiDataBasesException(message);
            }
        });

        LOG.info("Check complete !");

    }

    /**
     *
     * Scan classpath into given package with given filters and return matching classes
     *
     * @param pPackageToScan
     *            Package to scan
     * @param pIncludeAnnotation
     *            Include filter
     * @param pExcludeAnnotation
     *            Exclude filter
     * @return matching classes
     * @since 1.0-SNAPSHOT
     */
    public static List<Class<?>> scanForJpaPackages(String pPackageToScan,
            Class<? extends Annotation> pIncludeAnnotation, Class<? extends Annotation> pExcludeAnnotation) {
        final List<Class<?>> packages = new ArrayList<>();
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false);
        if (pExcludeAnnotation != null) {
            scanner.addExcludeFilter(new AnnotationTypeFilter(pExcludeAnnotation));
        }
        if (pIncludeAnnotation != null) {
            scanner.addIncludeFilter(new AnnotationTypeFilter(pIncludeAnnotation));
        }
        for (final BeanDefinition def : scanner.findCandidateComponents(pPackageToScan)) {
            try {
                packages.add(Class.forName(def.getBeanClassName()));
            } catch (final ClassNotFoundException e) {
                LOG.error("Error adding entity " + def.getBeanClassName() + " for hibernate database update");
                LOG.error(e.getMessage(), e);
            }
        }
        return packages;
    }

}
