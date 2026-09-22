package se.sundsvall.financialaid.integration.ssbtek.configuration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SSBTEKPropertiesTest {

	private static final String URL = "http://localhost/ssbtek";
	private static final SSBTEKProperties.DataProcessor DATA_PROCESSOR = new SSBTEKProperties.DataProcessor((short) 3, "162021005521", "Försäkringskassan Test");

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void record_exposesAllFields() {
		final var properties = new SSBTEKProperties(URL, 5, 30, "base64data", "secret", DATA_PROCESSOR);

		assertThat(properties.url()).isEqualTo(URL);
		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
		assertThat(properties.keyStoreAsBase64()).isEqualTo("base64data");
		assertThat(properties.keyStorePassword()).isEqualTo("secret");
		assertThat(properties.dataProcessor()).isEqualTo(DATA_PROCESSOR);
	}

	@Test
	void record_withoutDataProcessor_isValid() {
		final var properties = new SSBTEKProperties(URL, 5, 30, "base64data", "secret", null);

		assertThat(properties.dataProcessor()).isNull();
		assertThat(validator.validate(properties)).isEmpty();
	}

	@Test
	void validate_withBlankDataProcessorFields_reportsConstraintViolations() {
		final var properties = new SSBTEKProperties(URL, 5, 30, "base64data", "secret",
			new SSBTEKProperties.DataProcessor((short) 1, " ", ""));

		final var violations = validator.validate(properties);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.containsExactlyInAnyOrder("dataProcessor.organisationNumber", "dataProcessor.name");
	}

	@Test
	void validate_withNullFields_reportsConstraintViolations() {
		final var properties = new SSBTEKProperties(null, 5, 30, null, null, null);

		final var violations = validator.validate(properties);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.containsExactlyInAnyOrder("url", "keyStoreAsBase64", "keyStorePassword");
	}

	@Test
	void validate_withBlankFields_reportsConstraintViolations() {
		final var properties = new SSBTEKProperties("  ", 5, 30, "  ", "", null);

		final var violations = validator.validate(properties);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.containsExactlyInAnyOrder("url", "keyStoreAsBase64", "keyStorePassword");
	}

	@Test
	void validate_withPopulatedFields_reportsNoViolations() {
		final var properties = new SSBTEKProperties(URL, 5, 30, "base64data", "secret", DATA_PROCESSOR);

		final var violations = validator.validate(properties);

		assertThat(violations).isEmpty();
	}

	@Test
	void record_equalsAndHashCode_followValueSemantics() {
		final var original = new SSBTEKProperties(URL, 5, 30, "keystore", "password", DATA_PROCESSOR);
		final var identical = new SSBTEKProperties(URL, 5, 30, "keystore", "password", DATA_PROCESSOR);
		final var different = new SSBTEKProperties(URL, 5, 30, "otherKeystore", "password", DATA_PROCESSOR);

		assertThat(original).isEqualTo(identical).hasSameHashCodeAs(identical);
		assertThat(original).isNotEqualTo(different);
	}
}
