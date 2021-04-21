package fr.cnes.regards.modules.authentication.domain.data.validator;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ServiceProviderNameValidatorTest {

    @Test
    public void isValid() {
        ServiceProviderNameValidator validator = new ServiceProviderNameValidator();
        assertThat(validator.isValid("test", null)).isTrue();
    }
}
