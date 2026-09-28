package se.sundsvall.financialaid.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.financialaid.Application;
import se.sundsvall.financialaid.integration.ssbtek.SSBTEKIntegration;

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

	@Test
	void getFinancialAidBasisWithMissingPersonalNumber() {
		webTestClient.get()
			.uri(PATH + "?fromDate=2025-01-01&toDate=2025-06-30", MUNICIPALITY_ID)
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(ssbtekIntegration);
	}

	@Test
	void getFinancialAidBasisWithMissingToDate() {
		webTestClient.get()
			.uri(PATH + "?personalNumber=199001011234&fromDate=2025-01-01", MUNICIPALITY_ID)
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(ssbtekIntegration);
	}

	@Test
	void getFinancialAidBasisWithInvalidFromDateFormat() {
		webTestClient.get()
			.uri(PATH + "?personalNumber=199001011234&fromDate=not-a-date&toDate=2025-06-30", MUNICIPALITY_ID)
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(ssbtekIntegration);
	}
}
