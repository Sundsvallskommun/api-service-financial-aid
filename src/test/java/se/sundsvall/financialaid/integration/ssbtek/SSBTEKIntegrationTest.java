package se.sundsvall.financialaid.integration.ssbtek;

import jakarta.xml.bind.annotation.XmlSchema;
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
import se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKProperties;
import se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKProperties.DataProcessor;
import ssbtek.ArbetsformedlingenSvar;
import ssbtek.Error;
import ssbtek.ForsakringskassanSvar;
import ssbtek.KallaEnum;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvar;
import ssbtek.SammansattBastjanstSvarData;
import ssbtek.SammansattBastjanstTestSvar;
import ssbtek.TestSvar;

import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SSBTEKIntegrationTest {

	private static final String PERSONAL_NUMBER = "9001011234";
	private static final LocalDate FROM_DATE = LocalDate.of(2025, JANUARY, 1);
	private static final LocalDate TO_DATE = LocalDate.of(2025, JUNE, 30);
	private static final DataProcessor DATA_PROCESSOR = new DataProcessor((short) 3, "162021005521", "Försäkringskassan Test");

	private static SSBTEKProperties propertiesWith(final DataProcessor dataProcessor) {
		return new SSBTEKProperties("http://localhost/ssbtek", 5, 30, "a2ln", "changeit", dataProcessor);
	}

	@Mock
	private SSBTEKClient client;

	private SSBTEKIntegration integration;

	@BeforeEach
	void setUp() {
		integration = new SSBTEKIntegration(client, propertiesWith(DATA_PROCESSOR));
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

	/**
	 * CSN rejects the request at 32 characters - see {@code MAX_ARENDEIDENTITET_LENGTH}. Nothing in the contract says so,
	 * so this test is the only thing standing between a future refactor and a silently half-empty answer.
	 */
	@Test
	void getFinancialAid_keepsArendeidentitetShortEnoughForCsn() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());

		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		assertThat(captureRequest().getGenerellaFrageparametrar().getArendeidentitet())
			.isNotBlank()
			.hasSizeLessThan(32);
	}

	@Test
	void getFinancialAid_buildsSkatteverketQueryForTheTwoLastCompletedTaxYears() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());

		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		final var skv = captureRequest().getSpecifikaFrageparametrar().getSKV();
		assertThat(skv.isInkludera()).isTrue();
		assertThat(skv.getFraga()).isNotNull().satisfies(fraga -> {
			assertThat(fraga.getAr()).containsExactly(TO_DATE.getYear() - 2, TO_DATE.getYear() - 1);
			assertThat(fraga.isForetagsinformation()).isTrue();
			assertThat(fraga.isIndividuppgifter()).isTrue();
			assertThat(fraga.isSkattekonto()).isTrue();
			assertThat(fraga.isSkatteuppgifter()).isTrue();
			assertThat(fraga.isBeskattningsbilagor()).isTrue();
		});
	}

	@Test
	void getFinancialAid_buildsRequestWithConfiguredPersonuppgiftsbitrade() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());

		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		assertThat(captureRequest().getGenerellaFrageparametrar().getPersonuppgiftsbitrade())
			.singleElement()
			.satisfies(agent -> {
				assertThat(agent.getOrdningsnummer()).isEqualTo((short) 3);
				assertThat(agent.getOrganisationsnummer()).isEqualTo("162021005521");
				assertThat(agent.getNamn()).isEqualTo("Försäkringskassan Test");
			});
	}

	@Test
	void getFinancialAid_withoutConfiguredDataProcessor_omitsPersonuppgiftsbitrade() {
		when(client.getBaseServiceInformation(any())).thenReturn(emptyResponse());
		integration = new SSBTEKIntegration(client, propertiesWith(null));

		integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		assertThat(captureRequest().getGenerellaFrageparametrar().getPersonuppgiftsbitrade()).isEmpty();
	}

	/**
	 * The envelope namespace is what Försäkringskassan routes on: the test environment answers ssbt/11 and rejects an
	 * ssbt/10 envelope with a generic SOAP fault. It is generated from the contract, so a regenerated or swapped-back
	 * schema would silently change it - hence the assertion.
	 */
	@Test
	void generatedContractUsesTheV11Namespace() {
		final var schema = SammansattBastjanstFraga.class.getPackage().getAnnotation(XmlSchema.class);

		assertThat(schema).isNotNull();
		assertThat(schema.namespace()).isEqualTo("http://schema.forsakringskassan.se/integration/ssbt/11");
	}

	private SammansattBastjanstFraga captureRequest() {
		final var captor = ArgumentCaptor.forClass(SammansattBastjanstFraga.class);
		verify(client).getBaseServiceInformation(captor.capture());
		return captor.getValue();
	}

	private static SammansattBastjanstSvar emptyResponse() {
		return new SammansattBastjanstSvar().withSvarsdata(new SammansattBastjanstSvarData());
	}

	@Test
	void getFinancialAid_withAgencyError_surfacesItInsteadOfAnEmptyMap() {
		final var svarsdata = new SammansattBastjanstSvarData()
			.withAF(new ArbetsformedlingenSvar().withError(new Error()
				.withKalla(KallaEnum.BT)
				.withFelkod("AF-503")
				.withFelmeddelande("Tjänsten är inte tillgänglig")))
			.withFK(new ForsakringskassanSvar().withError(new Error().withFelkod("LEFI-VERSION")));
		when(client.getBaseServiceInformation(any())).thenReturn(new SammansattBastjanstSvar().withSvarsdata(svarsdata));

		final var result = integration.getFinancialAid(PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		assertThat(result.get(Constants.AGENCY_AF)).containsKey("error");
		assertThat(result.get(Constants.AGENCY_FK)).containsKey("error");
		// An agency that simply had nothing to say stays an empty map - that is the distinction the error key restores.
		assertThat(result.get(Constants.AGENCY_CSN)).isEqualTo(Map.of());
	}

	@Test
	void testConnection_reportsAnropadPerAgency() {
		when(client.testBaseServiceInformation()).thenReturn(new SammansattBastjanstTestSvar()
			.withAF(new TestSvar().withAnropad(true))
			.withFK(new TestSvar().withAnropad(false).withError(new Error().withFelkod("LEFI-VERSION"))));

		final var result = integration.testConnection();

		assertThat(result).containsOnlyKeys(
			Constants.AGENCY_AF, Constants.AGENCY_CSN, Constants.AGENCY_FK,
			Constants.AGENCY_SKV, Constants.AGENCY_SO, Constants.AGENCY_TNS, Constants.AGENCY_MIV);
		assertThat(result.get(Constants.AGENCY_AF)).isEqualTo(Map.of("anropad", true));
		assertThat(result.get(Constants.AGENCY_FK))
			.containsEntry("anropad", false)
			.containsEntry("error", Map.of("felkod", "LEFI-VERSION"));
	}

	@Test
	void testConnection_withAgencyAbsentFromResponse_reportsNotCalledWithoutError() {
		when(client.testBaseServiceInformation()).thenReturn(new SammansattBastjanstTestSvar());

		final var result = integration.testConnection();

		assertThat(result).allSatisfy((agency, status) -> assertThat(status).isEqualTo(Map.of("anropad", false)));
	}
}
