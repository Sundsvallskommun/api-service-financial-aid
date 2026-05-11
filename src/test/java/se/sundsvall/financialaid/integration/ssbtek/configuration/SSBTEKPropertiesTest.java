package se.sundsvall.financialaid.integration.ssbtek.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SSBTEKPropertiesTest {

	@Test
	void record_exposesAllFields() {
		final var properties = new SSBTEKProperties(5, 30, "base64data", "secret");

		assertThat(properties.connectTimeout()).isEqualTo(5);
		assertThat(properties.readTimeout()).isEqualTo(30);
		assertThat(properties.keyStoreAsBase64()).isEqualTo("base64data");
		assertThat(properties.keyStorePassword()).isEqualTo("secret");
	}

	@Test
	void record_allowsNullKeystoreFields() {
		final var properties = new SSBTEKProperties(5, 30, null, null);

		assertThat(properties.keyStoreAsBase64()).isNull();
		assertThat(properties.keyStorePassword()).isNull();
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
