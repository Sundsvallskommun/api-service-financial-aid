package se.sundsvall.financialaid.integration.ssbtek.configuration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.zalando.logbook.Logbook;
import se.sundsvall.dept44.configuration.webservicetemplate.WebServiceTemplateBuilder;

@Configuration
public class SSBTEKConfiguration {

	public static final String CLIENT_ID = "ssbtek";

	@Bean("ssbtekWebServiceTemplate")
	WebServiceTemplate ssbtekWebServiceTemplate(final SSBTEKProperties properties, final Logbook logbook) {
		return WebServiceTemplateBuilder.create()
			.withBaseUrl(properties.url())
			.withConnectTimeout(Duration.ofSeconds(properties.connectTimeout()))
			.withReadTimeout(Duration.ofSeconds(properties.readTimeout()))
			.withPackageToScan(CLIENT_ID)
			.withLogbook(logbook)
			.withKeyStoreData(Base64.getDecoder().decode(properties.keyStoreAsBase64().getBytes(StandardCharsets.UTF_8)))
			.withKeyStorePassword(properties.keyStorePassword())
			.build();
	}
}
