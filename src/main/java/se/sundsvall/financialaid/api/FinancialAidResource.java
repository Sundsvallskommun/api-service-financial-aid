package se.sundsvall.financialaid.api;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.financialaid.integration.ssbtek.SSBTEKIntegration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/{municipalityId}/financial-aid")
class FinancialAidResource {

	private final SSBTEKIntegration ssbtekIntegration;

	FinancialAidResource(final SSBTEKIntegration ssbtekIntegration) {
		this.ssbtekIntegration = ssbtekIntegration;
	}

	/**
	 * Connectivity probe against SSBTEK, using the contract's own {@code testaBastjanstInformation} operation. Takes no
	 * personnummer and returns no personal data: per agency, whether its backend test service answered
	 * ({@code anropad}) and the {@code error} if it did not. This is the endpoint to use when verifying the client
	 * certificate, endpoint URL or contract version against a live SSBTEK - the income endpoint below asserts a
	 * 11 kap. 11 a § SoL basis that presupposes a real ärende.
	 */
	@GetMapping(path = "/connection", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Map<String, Map<String, Object>>> testConnection(@PathVariable final String municipalityId) {
		return ResponseEntity.ok(ssbtekIntegration.testConnection());
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Map<String, Map<String, Object>>> getFinancialAidBasis(
		@PathVariable final String municipalityId,
		@RequestParam final String personalNumber,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate fromDate,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate toDate) {

		return ResponseEntity.ok(ssbtekIntegration.getFinancialAid(personalNumber, fromDate, toDate));
	}
}
