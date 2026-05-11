package se.sundsvall.financialaid.api;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.financialaid.integration.ssbtek.SSBTEKIntegration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/financial-aid")
class FinancialAidResource {

	private final SSBTEKIntegration ssbtekIntegration;

	FinancialAidResource(final SSBTEKIntegration ssbtekIntegration) {
		this.ssbtekIntegration = ssbtekIntegration;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	ResponseEntity<Map<String, Map<String, Object>>> getFinancialAid(
		@RequestParam final String personalNumber,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate fromDate,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate toDate) {

		return ResponseEntity.ok(ssbtekIntegration.getFinancialAid(personalNumber, fromDate, toDate));
	}
}
