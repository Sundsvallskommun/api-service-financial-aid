package se.sundsvall.financialaid.integration.ssbtek.configuration;

import jakarta.validation.Valid;
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

	@NotBlank String keyStorePassword,

	@Valid DataProcessor dataProcessor) {

	/**
	 * The personuppgiftsbitrade - the agent the ingivare engages to process the request - that SSBTEK expects in
	 * {@code generellaFrageparametrar}. It names Forsakringskassan's own environment, so the value differs between their
	 * test and production environments and belongs in configuration rather than in the code.
	 *
	 * <p>
	 * The element is optional in the contract. Leaving the whole block unconfigured omits it from the request, which is
	 * what the production endpoint has accepted so far.
	 */
	public record DataProcessor(

		Short sequenceNumber,

		@NotBlank String organisationNumber,

		@NotBlank String name) {
	}
}
