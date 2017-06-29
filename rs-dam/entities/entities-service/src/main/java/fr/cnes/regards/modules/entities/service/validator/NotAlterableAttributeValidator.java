/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.entities.service.validator;

import java.util.Objects;
import java.util.Optional;

import org.springframework.validation.Errors;

import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;

/**
 * Validate not alterable attribute
 *
 * @author Marc Sordi
 *
 */
public class NotAlterableAttributeValidator extends AbstractAttributeValidator {

    private AttributeModel attribute;

    private Optional<AbstractAttribute<?>> oldValue;

    private Optional<AbstractAttribute<?>> newValue;

    public NotAlterableAttributeValidator(String pAttributeKey, AttributeModel attribute, Optional<AbstractAttribute<?>> oldValue,
            Optional<AbstractAttribute<?>> newValue) {
        super(pAttributeKey);
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.attribute = attribute;
    }

    @Override
    public void validate(Object pTarget, Errors pErrors) {
        //if the attribute should be present then lets reject if any of them doesn't exist
        if(!attribute.isOptional() && (!newValue.isPresent())) {
            pErrors.reject("error.attribute.not.alterable.not.present.message", String.format("Attribute \"%s\" is required", attributeKey));
        } else {
            if(newValue.isPresent()) {
                //if the attribute is optional and there was no value before, lets accept the new one
                if(attribute.isOptional() && !oldValue.isPresent()) {
                    return;
                }
                if(!Objects.equals(oldValue.get().getValue(), newValue.get().getValue())) {
                    pErrors.reject("error.attribute.not.alterable.message", String.format("Attribute \"%s\" is not alterable.", attributeKey));
                }
            }
        }
    }

}
