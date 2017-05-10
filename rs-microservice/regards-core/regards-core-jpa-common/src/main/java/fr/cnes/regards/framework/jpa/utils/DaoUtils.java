/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import javax.persistence.Entity;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;

/**
 *
 * Class DaoUtils
 *
 * Tools class for DAO
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public final class DaoUtils {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DaoUtils.class);

    /**
     * Root package
     */
    public static final String ROOT_PACKAGE = "fr.cnes.regards";

    /**
     * Framework root package
     */
    public static final String FRAMEWORK_PACKAGE = "fr.cnes.regards.framework.modules";

    /**
     * Modules root package
     */
    public static final String MODULES_PACKAGE = "fr.cnes.regards.modules";

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    private DaoUtils() {
    }

    /**
     *
     * This method check that the classPatch is valid. That the scan packages for instance database and the projects
     * database are not in conflict.
     *
     * @param pPackageToScan
     *            package name to scan for JPA entities and repositories
     * @throws MultiDataBasesException
     *
     * @since 1.0-SNAPSHOT
     */
    public static void checkClassPath(final String pPackageToScan) throws MultiDataBasesException {

        LOGGER.info("Checking classpath for conflicts between instance and projects databases ...");

        final Set<String> packagesToScan = findPackagesForJpa(pPackageToScan);
        final List<Class<?>> instanceClasses = DaoUtils.scanPackagesForJpa(InstanceEntity.class, null, packagesToScan);
        final List<Class<?>> projectsClasses = DaoUtils
                .scanPackagesForJpa(Entity.class, InstanceEntity.class, packagesToScan);
        final List<String> instancePackages = new ArrayList<>();
        instanceClasses.forEach(instanceClass -> instancePackages.add(instanceClass.getPackage().getName()));
        final List<String> projectPackages = new ArrayList<>();
        projectsClasses.forEach(projectClass -> projectPackages.add(projectClass.getPackage().getName()));
        for (final String instancePackage : instancePackages) {
            for (final String pack : projectPackages) {
                if (pack.contains(instancePackage) || instancePackage.contains(pack)) {
                    LOGGER.error(String.format(
                            "Invalid classpath. Package %s is used for instance DAO Entities and multitenant DAO Entities",
                            instancePackage));
                    throw new MultiDataBasesException(
                            "Invalid classpath for JPA multitenant and JPA instance databases.");
                }
            }
        }

        LOGGER.info("Classpath is valid !");

    }

    /**
     * Find the packages for the JPA. Find the class who used Jpa and extracts their package name.
     * <p>
     * Find all the class annotated with {@link org.springframework.data.repository.Repository}
     * <p>
     * Find all the class who extends {@link CrudRepository}
     * <p>
     * Find all the class who extends {@link JpaRepository}
     *
     * @param pRootPackage the base package
     * @return the {@link Set} of package
     */
    @SuppressWarnings("rawtypes")
    public static Set<String> findPackagesForJpa(String pRootPackage) {
        Set<String> packagesToScan = new HashSet<>();
        final Reflections reflections = new Reflections(pRootPackage);

        // Add the packages that contains a class annotated with org.springframework.stereotype.Repository
        final Set<Class<?>> annotatedRepository = reflections.getTypesAnnotatedWith(Repository.class);
        for (Class<?> aClass : annotatedRepository) {
            packagesToScan.add(getPackageToScan(aClass.getCanonicalName()));
        }

        // Add the packages that contains a class annotated with org.springframework.data.jpa.repository.JpaRepository
        final Set<Class<? extends JpaRepository>> subTypeJpaRepository = reflections.getSubTypesOf(JpaRepository.class);
        for (Class<?> aClass : subTypeJpaRepository) {
            packagesToScan.add(getPackageToScan(aClass.getCanonicalName()));
        }

        // Add the packages that contains a class annotated with org.springframework.data.repository.CrudRepository
        final Set<Class<? extends CrudRepository>> subTypeRepository = reflections.getSubTypesOf(CrudRepository.class);
        for (Class<?> aClass : subTypeRepository) {
            packagesToScan.add(getPackageToScan(aClass.getCanonicalName()));
        }

        return packagesToScan;
    }

    /**
     * package name has format : fr.cnes.regards.modules.(name)....
     * or fr.cnes.regards.framework.modules.(name)...
     *
     * We must only take fr.cnes.regards[.framework].modules.(name)
     *
     * @param pPackageName
     * @return
     */
    public static String getPackageToScan(String pPackageName) {
        if (pPackageName.startsWith(DaoUtils.FRAMEWORK_PACKAGE)) {
            String packageEnd = pPackageName.substring(DaoUtils.FRAMEWORK_PACKAGE.length() + 1);
            return DaoUtils.FRAMEWORK_PACKAGE + "." + packageEnd.substring(0, packageEnd.indexOf('.'));
        } else {
            String packageEnd = pPackageName.substring(DaoUtils.MODULES_PACKAGE.length() + 1);
            return DaoUtils.MODULES_PACKAGE + "." + packageEnd.substring(0, packageEnd.indexOf('.'));
        }
    }

    public static List<Class<?>> scanPackagesForJpa(Class<? extends Annotation> pIncludeAnnotation,
            Class<? extends Annotation> pExcludeAnnotation, String... pPackages) {
        return Arrays.stream(pPackages)
                .flatMap(pPackage -> scanPackageForJpa(pPackage, pIncludeAnnotation, pExcludeAnnotation).stream())
                .collect(Collectors.toList());
    }

    public static List<Class<?>> scanPackagesForJpa(Class<? extends Annotation> pIncludeAnnotation,
            Class<? extends Annotation> pExcludeAnnotation, Collection<String> pPackages) {
        return pPackages.stream()
                .flatMap(pPackage -> scanPackageForJpa(pPackage, pIncludeAnnotation, pExcludeAnnotation).stream())
                .collect(Collectors.toList());
    }

    /**
     * Scan classpath into given package with given filters and return matching classes
     *
     * @param pPackageToScan
     *            Package to scan
     * @param pIncludeAnnotation
     *            Include filter
     * @param pExcludeAnnotation
     *            Exclude filter
     * @return matching classes
     */
    public static List<Class<?>> scanPackageForJpa(final String pPackageToScan,
            final Class<? extends Annotation> pIncludeAnnotation,
            final Class<? extends Annotation> pExcludeAnnotation) {
        final List<Class<?>> packages = new ArrayList<>();
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false);
        if (pExcludeAnnotation != null) {
            LOGGER.debug("Excluding JPA entities with {} annotation", pExcludeAnnotation.getName());
            scanner.addExcludeFilter(new AnnotationTypeFilter(pExcludeAnnotation));
        }
        if (pIncludeAnnotation != null) {
            LOGGER.debug("Including JPA entities with {} annotation", pIncludeAnnotation.getName());
            scanner.addIncludeFilter(new AnnotationTypeFilter(pIncludeAnnotation));
        }
        for (final BeanDefinition def : scanner.findCandidateComponents(pPackageToScan)) {
            try {
                LOGGER.debug("Package {} selected for scanning", def.getBeanClassName());
                packages.add(Class.forName(def.getBeanClassName()));
            } catch (final ClassNotFoundException e) {
                LOGGER.error("Error adding entity " + def.getBeanClassName() + " for hibernate database update");
                LOGGER.error(e.getMessage(), e);
            }
        }
        return packages;
    }

}
