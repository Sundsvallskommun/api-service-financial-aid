package se.sundsvall.financialaid.integration.ssbtek.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("integration.ssbtek")
public record SSBTEKProperties(

	@NotBlank String url,

	int connectTimeout,

	int readTimeout,

	@NotBlank String keyStoreAsBase64,

	@NotBlank String keyStorePassword) {

}
