package se.sundsvall.financialaid.integration.ssbtek.configuration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SSBTEKPropertiesTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void record_exposesAllFields() {
		final var properties = new SSBTEKProperties(5, 30, "base64data", "secret");

		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
		assertThat(properties.keyStoreAsBase64()).isEqualTo("base64data");
		assertThat(properties.keyStorePassword()).isEqualTo("secret");
	}

	@Test
	void validate_withNullKeystoreFields_reportsConstraintViolations() {
		final var properties = new SSBTEKProperties(5, 30, null, null);

		final var violations = validator.validate(properties);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.containsExactlyInAnyOrder("keyStoreAsBase64", "keyStorePassword");
	}

	@Test
	void validate_withBlankKeystoreFields_reportsConstraintViolations() {
		final var properties = new SSBTEKProperties(5, 30, "  ", "");

		final var violations = validator.validate(properties);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.containsExactlyInAnyOrder("keyStoreAsBase64", "keyStorePassword");
	}

	@Test
	void validate_withPopulatedKeystoreFields_reportsNoViolations() {
		final var properties = new SSBTEKProperties(5, 30, "base64data", "secret");

		final var violations = validator.validate(properties);

		assertThat(violations).isEmpty();
	}

	@Test
	void record_equalsAndHashCode_followValueSemantics() {
		final var original = new SSBTEKProperties(5, 30, "keystore", "password");
		final var identical = new SSBTEKProperties(5, 30, "keystore", "password");
		final var different = new SSBTEKProperties(5, 30, "otherKeystore", "password");

		assertThat(original).isEqualTo(identical).hasSameHashCodeAs(identical);
		assertThat(original).isNotEqualTo(different);
	}
}
