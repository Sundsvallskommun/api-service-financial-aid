package se.sundsvall.financialaid.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import ssbtek.AkassornasSamorganisationSvar;
import ssbtek.ArbetsformedlingenSvar;
import ssbtek.CsnSvar;
import ssbtek.CsnSvarsData;
import ssbtek.FindByPersonnummerResponse;
import ssbtek.FkSvarsData;
import ssbtek.ForsakringskassanSvar;
import ssbtek.GetUnemploymentBenefitPaymentResponseType;
import ssbtek.HamtaUppgifterResponseType;
import ssbtek.MigrationsverketSvar;
import ssbtek.SammansattBastjanstSvar;
import ssbtek.SkatteverketSvar;
import ssbtek.SvarMeddelande;
import ssbtek.TransportstyrelsenSvar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseMapperTest {

	private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();
	private static final String FIXTURE_PATH = "classpath:fixture/";

	@Nested
	class CsnTest {

		@Test
		void mapCsn_withNullResponse_shouldReturnNull() {
			assertThat(ResponseMapper.mapCsn(null)).isEqualTo(Map.of());
		}

		@Test
		void mapCsn_withNullData_shouldReturnNull() {
			assertThat(ResponseMapper.mapCsn(new CsnSvar())).isEqualTo(Map.of());
		}

		@Test
		void mapCsn_withInvalidXml_shouldThrowException() {
			final var csnSvar = createCsnSvar("not xml at all");

			assertThatThrownBy(() -> ResponseMapper.mapCsn(csnSvar))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Failed to parse XML");
		}

		@Test
		void mapCsn_withFixture_shouldMapResultatAndPerson() {
			final var csnResponse = loadFixture("csn-response.xml").getSvarsdata().getCSN();

			final var result = ResponseMapper.mapCsn(csnResponse);

			final var resultat = nested(result, "GemensamtSvarEkbistand", "Resultat");
			assertThat(resultat.get("ResultatKod")).isEqualTo("0");
			assertThat(resultat.get("ResultatText")).isEqualTo("Allt OK, inget att rapportera");

			final var person = nested(result, "Personer", "Person");
			assertThat(person.get("personnummer")).isEqualTo("9001011234");
			assertThat(person.get("sekel")).isEqualTo("19");
		}
	}

	@Nested
	class FkTest {

		@Test
		void mapFk_withNullResponse_shouldReturnNull() {
			assertThat(ResponseMapper.mapFk(null)).isEqualTo(Map.of());
		}

		@Test
		void mapFk_withNullData_shouldReturnNull() {
			assertThat(ResponseMapper.mapFk(new ForsakringskassanSvar())).isEqualTo(Map.of());
		}

		@Test
		void mapFk_withValidJson_shouldReturnParsedMap() {
			final var json = "{\"forman\":\"AP\",\"belopp\":1234}".getBytes(StandardCharsets.UTF_8);
			final var fkResponse = new ForsakringskassanSvar()
				.withData(new FkSvarsData().withLefiJsonSvar(json));

			final var result = ResponseMapper.mapFk(fkResponse);

			assertThat(result).containsEntry("forman", "AP").containsEntry("belopp", 1234);
		}

		@Test
		void mapFk_withFixture_shouldMapDecodedLefiJson() {
			final var fkResponse = loadFixture("fk-response.xml").getSvarsdata().getFK();

			final var result = ResponseMapper.mapFk(fkResponse);

			final var utlamnare = asList(result.get("utlamnare"));
			assertThat(utlamnare).hasSize(1);
			final var first = asMap(utlamnare.getFirst());
			final var inner = nested(first, "utlamnare");
			assertThat(inner.get("kod")).isEqualTo("FK");
			assertThat(inner.get("namn")).isEqualTo("Försäkringskassan");
			final var tillstand = nested(first, "tillstand");
			assertThat(tillstand.get("kod")).isEqualTo("OK");
			assertThat(tillstand.get("beskrivning")).isEqualTo("Tillgänglig");
		}

		@Test
		void mapFk_withInvalidJson_shouldThrowIllegalState() {
			final var fkResponse = new ForsakringskassanSvar()
				.withData(new FkSvarsData().withLefiJsonSvar("not json".getBytes(StandardCharsets.UTF_8)));

			assertThatThrownBy(() -> ResponseMapper.mapFk(fkResponse))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Failed to parse FK JSON response");
		}
	}

	@Nested
	class AfTest {

		@Test
		void mapAf_withNullResponse_shouldReturnNull() {
			assertThat(ResponseMapper.mapAf(null)).isEqualTo(Map.of());
		}

		@Test
		void mapAf_withNullData_shouldReturnNull() {
			assertThat(ResponseMapper.mapAf(new ArbetsformedlingenSvar())).isEqualTo(Map.of());
		}

		@Test
		void mapAf_withEmptyData_shouldReturnMap() {
			final var afResponse = new ArbetsformedlingenSvar().withData(new SvarMeddelande());

			assertThat(ResponseMapper.mapAf(afResponse)).isNotNull();
		}

		@Test
		void mapAf_withFixture_shouldMapPersonAndArbetssokande() {
			final var afResponse = loadFixture("af-response.xml").getSvarsdata().getAF();

			final var result = ResponseMapper.mapAf(afResponse);

			final var svar = nested(result, "Svar");
			assertThat(svar.get("Schemaversion")).isEqualTo("4.0");
			assertThat(svar.get("Referensnummer")).isEqualTo("AID00000001");
			assertThat(svar.get("IdentitetsbeteckningFysiskPerson")).isEqualTo("199001011234");
			assertThat(svar.get("Fornamn")).isEqualTo("Tolvan");
			assertThat(svar.get("Efternamn")).isEqualTo("Tolvansson");
			assertThat(svar.get("Akassetillhorighet")).isEqualTo("Test ak");

			final var arbetssokande = nested(svar, "ArbetssokandeInfo");
			assertThat(arbetssokande.get("Arbetssokande")).isEqualTo("true");
			assertThat(arbetssokande.get("ArbetssokandeSKAT")).isEqualTo("Anställd");
			assertThat(arbetssokande.get("ArbetssokandeOmfattning")).isEqualTo("100");
		}
	}

	@Nested
	class SkvTest {

		@Test
		void mapSkv_withNullResponse_shouldReturnNull() {
			assertThat(ResponseMapper.mapSkv(null)).isEqualTo(Map.of());
		}

		@Test
		void mapSkv_withNullData_shouldReturnNull() {
			assertThat(ResponseMapper.mapSkv(new SkatteverketSvar())).isEqualTo(Map.of());
		}

		@Test
		void mapSkv_withFixture_shouldMapPersonuppgift() {
			final var skvResponse = loadFixture("skv-response.xml").getSvarsdata().getSKV();

			final var result = ResponseMapper.mapSkv(skvResponse);

			final var personuppgift = nested(result, "PersonuppgiftLista", "Personuppgift");
			assertThat(personuppgift.get("IdentitetsbeteckningFysiskPerson")).isEqualTo("199001011234");
			assertThat(personuppgift.get("Fornamn")).isEqualTo("Berit");
			assertThat(personuppgift.get("Efternamn")).isEqualTo("Berg");
			assertThat(personuppgift.get("StatusPersonuppgiftKod")).isEqualTo("999");

			final var kapital = nested(personuppgift, "Kapitaluppgift");
			assertThat(kapital.get("Taxeringsar")).isEqualTo("2001");
			assertThat(kapital.get("SummaIntakterPaKapital")).isEqualTo("12345");
			assertThat(kapital.get("OverskottPaKapital")).isEqualTo("666");
		}
	}

	@Nested
	class SoTest {

		@Test
		void mapSo_withNullResponse_shouldReturnNull() {
			assertThat(ResponseMapper.mapSo(null)).isEqualTo(Map.of());
		}

		@Test
		void mapSo_withNullData_shouldReturnNull() {
			assertThat(ResponseMapper.mapSo(new AkassornasSamorganisationSvar())).isEqualTo(Map.of());
		}

		@Test
		void mapSo_withEmptyData_shouldReturnMap() {
			final var soResponse = new AkassornasSamorganisationSvar().withData(new GetUnemploymentBenefitPaymentResponseType());

			assertThat(ResponseMapper.mapSo(soResponse)).isNotNull();
		}

		@Test
		void mapSo_withFixture_shouldMapUtbetalningar() {
			final var soResponse = loadFixture("so-response.xml").getSvarsdata().getSO();

			final var result = ResponseMapper.mapSo(soResponse);

			assertThat(result.get("OrganisationsnummerIngivare")).isEqualTo("162000000001");
			assertThat(result.get("NamnIngivare")).isEqualTo("Testkommun");
			assertThat(result.get("IdentificationNumber")).isEqualTo("199001011234");

			final var ersattning = nested(result, "ArbetsloshetsersattningLista", "Arbetsloshetsersattning");
			assertThat(ersattning.get("SvarandeOrganisation")).isEqualTo("Testorganisation");
			assertThat(ersattning.get("StatusSvarandeOrganisation")).isEqualTo("all clear");

			final var utbetalningar = nested(ersattning, "Utbetalningar");
			assertThat(utbetalningar.get("NettoEfterSkatt")).isEqualTo("1250.50");
			assertThat(utbetalningar.get("Ersattningsdagar")).isEqualTo("2");
		}
	}

	@Nested
	class TnsTest {

		@Test
		void mapTns_withNullResponse_shouldReturnNull() {
			assertThat(ResponseMapper.mapTns(null)).isEqualTo(Map.of());
		}

		@Test
		void mapTns_withNullData_shouldReturnNull() {
			assertThat(ResponseMapper.mapTns(new TransportstyrelsenSvar())).isEqualTo(Map.of());
		}

		@Test
		void mapTns_withEmptyData_shouldReturnMap() {
			final var tnsResponse = new TransportstyrelsenSvar().withData(new HamtaUppgifterResponseType());

			assertThat(ResponseMapper.mapTns(tnsResponse)).isNotNull();
		}

		@Test
		void mapTns_withFixture_shouldMapFordon() {
			final var tnsResponse = loadFixture("tns-response.xml").getSvarsdata().getTNS();

			final var result = ResponseMapper.mapTns(tnsResponse);

			assertThat(result.get("resultat")).isEqualTo("OK");
			final var fordonsList = asList(nested(result, "fordonsinnehav").get("fordon"));
			assertThat(fordonsList).hasSize(2);

			final var first = asMap(fordonsList.getFirst());
			assertThat(first.get("regnr")).isEqualTo("ABC001");
			assertThat(first.get("fabrikat")).isEqualTo("Volvo");
			assertThat(first.get("fordonsslag")).isEqualTo("PERSONBIL");

			final var second = asMap(fordonsList.get(1));
			assertThat(second.get("regnr")).isEqualTo("ABC002");
			assertThat(second.get("fabrikat")).isEqualTo("HONDA");
			assertThat(second.get("fordonsslag")).isEqualTo("MOTORCYKEL");
		}
	}

	@Nested
	class MivTest {

		@Test
		void mapMiv_withNullResponse_shouldReturnNull() {
			assertThat(ResponseMapper.mapMiv(null)).isEqualTo(Map.of());
		}

		@Test
		void mapMiv_withNullData_shouldReturnNull() {
			assertThat(ResponseMapper.mapMiv(new MigrationsverketSvar())).isEqualTo(Map.of());
		}

		@Test
		void mapMiv_withEmptyData_shouldReturnMap() {
			final var mivResponse = new MigrationsverketSvar().withData(new FindByPersonnummerResponse());

			assertThat(ResponseMapper.mapMiv(mivResponse)).isNotNull();
		}

		@Test
		void mapMiv_withFixture_shouldMapBeslut() {
			final var mivResponse = loadFixture("miv-response.xml").getSvarsdata().getMIV();

			final var result = ResponseMapper.mapMiv(mivResponse);

			final var response = nested(result, "response");
			assertThat(response.get("personnummer")).isEqualTo("199001011234");
			assertThat(response.get("fornamn")).isEqualTo("Tolvan");
			assertThat(response.get("efternamn")).isEqualTo("Tolvansson");

			final var svar = nested(response, "svarCollection", "svar");
			assertThat(svar.get("arendeTyp")).isEqualTo("UAT");
			assertThat(svar.get("arendeTypNamn")).isEqualTo("UPPEHÅLLS- OCH ARBETSTILLSTÅND");
			assertThat(svar.get("beslutsKod")).isEqualTo("BEV");
			assertThat(svar.get("beslutsTyp")).isEqualTo("PUT");
			assertThat(svar.get("giltighetstid")).isEqualTo("PERMANENT");
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> nested(final Map<String, Object> root, final String... keys) {
		Object current = root;
		for (final String key : keys) {
			current = ((Map<String, Object>) current).get(key);
		}
		return (Map<String, Object>) current;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(final Object obj) {
		return (Map<String, Object>) obj;
	}

	@SuppressWarnings("unchecked")
	private static List<Object> asList(final Object obj) {
		return (List<Object>) obj;
	}

	private static CsnSvar createCsnSvar(final String xml) {
		return new CsnSvar()
			.withData(new CsnSvarsData()
				.withSvar(xml.getBytes(StandardCharsets.UTF_8)));
	}

	private static SammansattBastjanstSvar loadFixture(final String filename) {
		try (InputStream input = RESOURCE_LOADER.getResource(FIXTURE_PATH + filename).getInputStream()) {
			final var soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage(null, input);
			final var document = soapMessage.getSOAPBody().extractContentAsDocument();
			final var context = JAXBContext.newInstance(SammansattBastjanstSvar.class);
			return context.createUnmarshaller().unmarshal(document, SammansattBastjanstSvar.class).getValue();
		} catch (final Exception exception) {
			throw new IllegalStateException("Failed to load fixture: " + filename, exception);
		}
	}

}
