package se.sundsvall.financialaid.api;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import se.sundsvall.financialaid.integration.ssbtek.SSBTEKIntegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAidResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PERSONAL_NUMBER = "9001011234";
	private static final LocalDate FROM_DATE = LocalDate.of(2025, 1, 1);
	private static final LocalDate TO_DATE = LocalDate.of(2025, 6, 30);

	@Mock
	private SSBTEKIntegration ssbtekIntegration;

	@InjectMocks
	private FinancialAidResource resource;

	@Test
	void getFinancialAid_returnsOkWithIntegrationResult() {
		final Map<String, Map<String, Object>> expected = new LinkedHashMap<>();
		expected.put("csn", Map.of("foo", "bar"));
		when(ssbtekIntegration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE)).thenReturn(expected);

		final var response = resource.getFinancialAid(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(expected);
		verify(ssbtekIntegration).getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);
		verifyNoMoreInteractions(ssbtekIntegration);
	}

	@Test
	void getFinancialAid_passesParametersThroughUnchanged() {
		when(ssbtekIntegration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE)).thenReturn(Map.of());

		resource.getFinancialAid(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		verify(ssbtekIntegration).getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);
	}
}
