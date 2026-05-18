package se.sundsvall.financialaid.integration.ssbtek;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.financialaid.Constants;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvar;
import ssbtek.SammansattBastjanstSvarData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SSBTEKIntegrationTest {

	private static final String PERSONAL_NUMBER = "9001011234";
	private static final LocalDate FROM_DATE = LocalDate.of(2025, 1, 1);
	private static final LocalDate TO_DATE = LocalDate.of(2025, 6, 30);

	@Mock
	private SSBTEKClient client;

	private SSBTEKIntegration integration;

	@BeforeEach
	void setUp() {
		integration = new SSBTEKIntegration(client);
	}

	@Test
	void getFinancialAid_returnsMapWithAllAgencyKeys() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());

		final var result = integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		assertThat(result).containsOnlyKeys(
			Constants.AGENCY_AF,
			Constants.AGENCY_CSN,
			Constants.AGENCY_FK,
			Constants.AGENCY_SKV,
			Constants.AGENCY_SO,
			Constants.AGENCY_TNS,
			Constants.AGENCY_MIV);
		assertThat(result).allSatisfy((key, value) -> assertThat(value).isEqualTo(Map.of()));
	}

	@Test
	void getFinancialAid_buildsRequestWithPersonAndDates() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());

		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		final var request = captureRequest();
		final var generella = request.getGenerellaFrageparametrar();
		assertThat(generella.getPersonidentitet()).isEqualTo(PERSONAL_NUMBER);
		assertThat(generella.getTidsperiod().getFromDatum().toString()).isEqualTo(FROM_DATE.toString());
		assertThat(generella.getTidsperiod().getTomDatum().toString()).isEqualTo(TO_DATE.toString());
		assertThat(generella.getArendeidentitet()).isNotBlank();
		assertThat(generella.getSyfte()).isEqualTo("Beslut om eller kontroll av Ekonomisk försörjningsstöd");
		assertThat(generella.getLagtext()).isEqualTo("11 kap. 11 a § socialtjänstlagen (2001:453), 5 § förordning (2008:975) om uppgiftsskyldighet i vissa fall enligt socialtjänstlagen.");
		assertThat(generella.isFraganSkallInkluderasISvaret()).isFalse();
	}

	@Test
	void getFinancialAid_buildsRequestWithIngivareDetails() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());

		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		final var ingivare = captureRequest().getGenerellaFrageparametrar().getIngivare();
		assertThat(ingivare.getOrganisationsnummer()).isEqualTo("162120002411");
		assertThat(ingivare.getNamn()).isEqualTo("Sundsvalls kommun");
		assertThat(ingivare.getHandlaggare()).isEqualTo("system");
	}

	@Test
	void getFinancialAid_buildsRequestWithAllAgenciesIncluded() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());

		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		final var specifika = captureRequest().getSpecifikaFrageparametrar();
		assertThat(specifika.getAF().isInkludera()).isTrue();
		assertThat(specifika.getCSN().isInkludera()).isTrue();
		assertThat(specifika.getFK().isInkludera()).isTrue();
		assertThat(specifika.getSKV().isInkludera()).isTrue();
		assertThat(specifika.getSO().isInkludera()).isTrue();
		assertThat(specifika.getTNS().isInkludera()).isTrue();
		assertThat(specifika.getMIV().isInkludera()).isTrue();
	}

	@Test
	void getFinancialAid_buildsFkLefiJsonRequestWithPersonAndDates() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());

		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		final var fkFraga = captureRequest().getSpecifikaFrageparametrar().getFK().getFraga();
		assertThat(fkFraga.getVersion()).isEqualTo("v9");
		assertThat(fkFraga.getAktorsid()).isEqualTo("026-51");
		final var lefiJson = new String(fkFraga.getLefiJsonFraga(), StandardCharsets.UTF_8);
		assertThat(lefiJson)
			.contains("\"personnummer\": \"" + PERSONAL_NUMBER + "\"")
			.contains("\"from\": \"" + FROM_DATE + "\"")
			.contains("\"tom\": \"" + TO_DATE + "\"");
	}

	@Test
	void getFinancialAid_eachCallProducesUniqueArendeidentitet() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());

		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);
		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		final var captor = ArgumentCaptor.forClass(SammansattBastjanstFraga.class);
		verify(client, times(2)).getBaseServiceInformation(captor.capture());
		final var ids = captor.getAllValues().stream()
			.map(request -> request.getGenerellaFrageparametrar().getArendeidentitet())
			.toList();
		assertThat(ids).doesNotHaveDuplicates();
	}

	private SammansattBastjanstFraga captureRequest() {
		final var captor = ArgumentCaptor.forClass(SammansattBastjanstFraga.class);
		verify(client).getBaseServiceInformation(captor.capture());
		return captor.getValue();
	}

	private static SammansattBastjanstSvar emptyResponse() {
		return new SammansattBastjanstSvar().withSvarsdata(new SammansattBastjanstSvarData());
	}
}
