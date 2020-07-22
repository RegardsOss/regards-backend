package fr.cnes.regards.modules.processing.domain.constraints;

import io.vavr.Function1;
import io.vavr.collection.Seq;

import static fr.cnes.regards.modules.processing.domain.constraints.IExecutionCollectionConstraint.CollectionConstraintType.IN;

public class ExecutionRights implements IExecutionCollectionConstraint<String> {

    private final CollectionConstraintType type;
    private final Seq<String> values;
    private final Function1<String, String> messageFormat;

    private ExecutionRights(
            CollectionConstraintType type,
            Seq<String> values,
            Function1<String, String> messageFormat
    ) {
        this.type = type;
        this.values = values;
        this.messageFormat = messageFormat;
    }

    @Override public CollectionConstraintType type() {
        return type;
    }

    @Override public Seq<String> values() {
        return values;
    }

    @Override public ExecutionConstraintViolation violationFor(String actualValue) {
        return new ExecutionConstraintViolation(this, messageFormat.apply(actualValue));
    }

    public static ExecutionRights allowedTenants(Seq<String> allowedTenants) {
        return new ExecutionRights(
                IN, allowedTenants,
                absent -> String.format("The tenant '%s' is absent from configured allowed tenants for this process", absent)
        );
    }

    public static ExecutionRights allowedDatasets(Seq<String> allowedDatasets) {
        return new ExecutionRights(
                IN, allowedDatasets,
                absent -> String.format("The dataset '%s' is absent from configured allowed datasets for this process", absent)
        );
    }

    public static ExecutionRights allowedUserRoles(Seq<String> allowedUserRoles) {
        return new ExecutionRights(
                IN, allowedUserRoles,
                absent -> String.format("The user role '%s' is absent from configured allowed user roles for this process", absent)
        );
    }
}
