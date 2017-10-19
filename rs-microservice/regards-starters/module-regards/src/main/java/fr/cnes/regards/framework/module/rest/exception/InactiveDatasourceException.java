package fr.cnes.regards.framework.module.rest.exception;

/**
 * Exception indicating that datasource is inactive
 * @author oroussel
 */
@SuppressWarnings("serial")
public class InactiveDatasourceException extends ModuleException {

    public InactiveDatasourceException() {
        super("Inactive datasource");
    }
}
