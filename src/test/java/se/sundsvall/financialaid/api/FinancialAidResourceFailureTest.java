package se.sundsvall.financialaid.api;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.financialaid.Application;
import se.sundsvall.financialaid.integration.ssbtek.SSBTEKIntegration;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAidResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "/{municipalityId}/financial-aid";

	@MockitoBean
	private SSBTEKIntegration ssbtekIntegration;

	@Autowired
	private WebTestClient webTestClient;

	private static Stream<Arguments> invalidQueries() {
		return Stream.of(
			arguments("missing personalNumber", "?fromDate=2025-01-01&toDate=2025-06-30"),
			arguments("missing toDate", "?personalNumber=199001011234&fromDate=2025-01-01"),
			arguments("invalid fromDate format", "?personalNumber=199001011234&fromDate=not-a-date&toDate=2025-06-30"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidQueries")
	void getFinancialAidBasisWithInvalidQuery(final String description, final String query) {
		webTestClient.get()
			.uri(PATH + query, MUNICIPALITY_ID)
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(ssbtekIntegration);
	}
}
