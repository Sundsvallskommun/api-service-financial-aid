package se.sundsvall.financialaid.integration.ssbtek.configuration;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.zalando.logbook.Logbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SSBTEKConfigurationTest {

	private static final String PASSWORD = "changeit";
	private static final String URL = "http://localhost/ssbtek";

	private final SSBTEKConfiguration configuration = new SSBTEKConfiguration();
	private final Logbook logbook = Logbook.create();

	@Test
	void ssbtekWebServiceTemplate_withValidKeystore_returnsTemplate() throws Exception {
		final var keystoreBase64 = createEmptyPkcs12KeystoreAsBase64();
		final var properties = new SSBTEKProperties(URL, 5, 30, keystoreBase64, PASSWORD);

		final var template = configuration.ssbtekWebServiceTemplate(properties, logbook);

		assertThat(template).isNotNull();
		assertThat(template.getDefaultUri()).isEqualTo(URL);
	}

	@Test
	void ssbtekWebServiceTemplate_withInvalidKeystoreData_throws() {
		final var badBase64 = Base64.getEncoder().encodeToString(new byte[] {
			1, 2, 3, 4, 5
		});
		final var properties = new SSBTEKProperties(URL, 5, 30, badBase64, "x");

		assertThatThrownBy(() -> configuration.ssbtekWebServiceTemplate(properties, logbook))
			.isInstanceOf(RuntimeException.class);
	}

	private static String createEmptyPkcs12KeystoreAsBase64() throws Exception {
		final var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, PASSWORD.toCharArray());
		final var out = new ByteArrayOutputStream();
		keyStore.store(out, PASSWORD.toCharArray());
		return Base64.getEncoder().encodeToString(out.toByteArray());
	}
}
