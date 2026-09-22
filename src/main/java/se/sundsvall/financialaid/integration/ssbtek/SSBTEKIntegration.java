package se.sundsvall.financialaid.integration.ssbtek;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.financialaid.Constants;
import se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKProperties;
import se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKProperties.DataProcessor;
import se.sundsvall.financialaid.service.ResponseMapper;
import ssbtek.AkassornasSamorganisation;
import ssbtek.Arbetsformedlingen;
import ssbtek.Csn;
import ssbtek.Forsakringskassan;
import ssbtek.ForsakringskassanFraga;
import ssbtek.GenerellaFrageparametrar;
import ssbtek.Ingivare;
import ssbtek.Migrationsverket;
import ssbtek.ObjectFactory;
import ssbtek.Personuppgiftsbitrade;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvarData;
import ssbtek.SammansattBastjanstTestSvar;
import ssbtek.Skatteverket;
import ssbtek.SkatteverketFraga;
import ssbtek.SpecifikaFrageparametrar;
import ssbtek.TestSvar;
import ssbtek.Tidsperiod;
import ssbtek.Transportstyrelsen;

import static java.util.Optional.ofNullable;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Component
public class SSBTEKIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(SSBTEKIntegration.class);

	private static final DatatypeFactory DATATYPE_FACTORY = createDatatypeFactory();
	private static final JAXBContext JAXB_CONTEXT = createJaxbContext();
	private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
	private static final String ORGANISATION_NR = "162120002411";
	private static final String ORGANISATION_NAME = "Sundsvalls kommun";
	private static final String FK_AKTORSID = "026-51";
	private static final String FK_VERSION = "v9";
	/**
	 * CSN rejects the whole request with felkod -1005 ("Inparametrar bryter mot specifikation/kontrakt") when
	 * arendeidentitet reaches 32 characters. Measured against Forsakringskassan's test environment 2026-09-22 with
	 * everything else held fixed: 31 characters answer with data, 32 do not, and a bare UUID is 36. The other agencies
	 * accept any length, so nothing in the contract or in the SOAP fault points this out.
	 */
	private static final int MAX_ARENDEIDENTITET_LENGTH = 31;

	private static DatatypeFactory createDatatypeFactory() {
		try {
			return DatatypeFactory.newInstance();
		} catch (final Exception exception) {
			throw new IllegalStateException("Failed to init DatatypeFactory", exception);
		}
	}

	private static JAXBContext createJaxbContext() {
		try {
			return JAXBContext.newInstance(SammansattBastjanstFraga.class);
		} catch (final JAXBException exception) {
			throw new IllegalStateException("Failed to init JAXBContext", exception);
		}
	}

	private final SSBTEKClient client;
	private final SSBTEKProperties properties;

	public SSBTEKIntegration(final SSBTEKClient client, final SSBTEKProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	public Map<String, Map<String, Object>> getFinancialAid(final String personalNumber, final LocalDate fromDate, final LocalDate toDate) {
		LOG.info("Received financial aid request: personalNumber={}, fromDate={}, toDate={}",
			sanitizeForLogging(personalNumber),
			sanitizeForLogging(fromDate.toString()),
			sanitizeForLogging(toDate.toString()));
		final var request = buildRequest(personalNumber, fromDate, toDate, properties.dataProcessor());
		logRequestXml(request);
		final var response = client.getBaseServiceInformation(request);

		return mapResponse(response.getSvarsdata());
	}

	/**
	 * Probe the SSBTEK connection with the contract's {@code testaBastjanstInformation} operation: an empty request that
	 * reports, per agency, whether its backend test service was reached ({@code anropad}) and the {@code error} if it was
	 * not. No personnummer is sent and no personal data comes back, so this is the check to run when verifying
	 * certificates, endpoint URL or contract version against a live SSBTEK.
	 *
	 * @return one entry per agency, each {@code {"anropad": boolean}} plus {@code "error"} when the backend reported one
	 */
	public Map<String, Map<String, Object>> testConnection() {
		LOG.info("Probing SSBTEK connection (testaBastjanstInformation, no personal data)");
		return mapTestResponse(client.testBaseServiceInformation());
	}

	private static Map<String, Map<String, Object>> mapTestResponse(final SammansattBastjanstTestSvar response) {
		final var result = new LinkedHashMap<String, Map<String, Object>>();
		result.put(Constants.AGENCY_AF, mapTestSvar(response.getAF()));
		result.put(Constants.AGENCY_CSN, mapTestSvar(response.getCSN()));
		result.put(Constants.AGENCY_FK, mapTestSvar(response.getFK()));
		result.put(Constants.AGENCY_SKV, mapTestSvar(response.getSKV()));
		result.put(Constants.AGENCY_SO, mapTestSvar(response.getSO()));
		result.put(Constants.AGENCY_TNS, mapTestSvar(response.getTNS()));
		result.put(Constants.AGENCY_MIV, mapTestSvar(response.getMIV()));
		logAgencyErrors(result);
		return result;
	}

	/**
	 * An agency omitted from the test response has no test service of its own to call, which is neither a success nor a
	 * failure - it is reported as {@code anropad: false} with no error, exactly as the contract describes it.
	 */
	private static Map<String, Object> mapTestSvar(final TestSvar testSvar) {
		final Map<String, Object> agency = new LinkedHashMap<>();
		agency.put("anropad", ofNullable(testSvar).map(TestSvar::isAnropad).orElse(false));
		ofNullable(testSvar)
			.map(TestSvar::getError)
			.map(ResponseMapper::toErrorDetails)
			.ifPresent(error -> agency.put(ResponseMapper.KEY_ERROR, error));
		return agency;
	}

	private static void logRequestXml(final SammansattBastjanstFraga request) {
		try {
			final var writer = new StringWriter();
			final var marshaller = JAXB_CONTEXT.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(OBJECT_FACTORY.createHamtaBastjanstInformation(request), writer);
			LOG.info("SSBTEK request XML body:\n{}", sanitizeForLogging(writer.toString()));
		} catch (final JAXBException exception) {
			LOG.warn("Failed to marshal SSBTEK request for logging", exception);
		}
	}

	private static Map<String, Map<String, Object>> mapResponse(final SammansattBastjanstSvarData responseData) {
		final var result = new LinkedHashMap<String, Map<String, Object>>();
		result.put(Constants.AGENCY_AF, ResponseMapper.mapAf(responseData.getAF()));
		result.put(Constants.AGENCY_CSN, ResponseMapper.mapCsn(responseData.getCSN()));
		result.put(Constants.AGENCY_FK, ResponseMapper.mapFk(responseData.getFK()));
		result.put(Constants.AGENCY_SKV, ResponseMapper.mapSkv(responseData.getSKV()));
		result.put(Constants.AGENCY_SO, ResponseMapper.mapSo(responseData.getSO()));
		result.put(Constants.AGENCY_TNS, ResponseMapper.mapTns(responseData.getTNS()));
		result.put(Constants.AGENCY_MIV, ResponseMapper.mapMiv(responseData.getMIV()));
		logAgencyErrors(result);
		return result;
	}

	/**
	 * An agency that answered with an {@code <error>} is worth a line in the log: it is an upstream failure the caller
	 * cannot retry for us, and without it the only trace is a response section a consumer may well read as "nothing to
	 * report". Only {@code kalla} and {@code felkod} are logged - {@code felmeddelande} is free text from the agency and
	 * may quote the query, which carries the personnummer.
	 */
	private static void logAgencyErrors(final Map<String, Map<String, Object>> result) {
		result.forEach((agency, payload) -> {
			final var error = payload.get(ResponseMapper.KEY_ERROR);
			if (error instanceof final Map<?, ?> details) {
				LOG.warn("SSBTEK agency {} answered with an error: kalla={}, felkod={}",
					sanitizeForLogging(agency),
					sanitizeForLogging(String.valueOf(details.get("kalla"))),
					sanitizeForLogging(String.valueOf(details.get("felkod"))));
			}
		});
	}

	private static SammansattBastjanstFraga buildRequest(final String personalNumber, final LocalDate fromDate, final LocalDate toDate, final DataProcessor dataProcessor) {
		final var generellaFrageparametrar = new GenerellaFrageparametrar()
			.withKorrelationsid(UUID.randomUUID().toString())
			.withFraganSkallInkluderasISvaret(false)
			.withIngivare(new Ingivare()
				.withOrganisationsnummer(ORGANISATION_NR)
				.withNamn(ORGANISATION_NAME)
				.withHandlaggare("system"))
			.withPersonidentitet(personalNumber)
			.withArendeidentitet(newArendeidentitet())
			.withTidsperiod(new Tidsperiod()
				.withFromDatum(toXmlDate(fromDate))
				.withTomDatum(toXmlDate(toDate)))
			.withSyfte("Beslut om eller kontroll av Ekonomisk försörjningsstöd")
			.withLagtext("11 kap. 11 a § socialtjänstlagen (2001:453), 5 § förordning (2008:975) om uppgiftsskyldighet i vissa fall enligt socialtjänstlagen.");

		ofNullable(dataProcessor)
			.map(SSBTEKIntegration::toPersonuppgiftsbitrade)
			.ifPresent(generellaFrageparametrar::withPersonuppgiftsbitrade);

		return new SammansattBastjanstFraga()
			.withGenerellaFrageparametrar(generellaFrageparametrar)
			.withSpecifikaFrageparametrar(new SpecifikaFrageparametrar()
				.withAF(new Arbetsformedlingen().withInkludera(true))
				.withCSN(new Csn().withInkludera(true))
				.withFK(new Forsakringskassan()
					.withInkludera(true)
					.withFraga(new ForsakringskassanFraga()
						.withVersion(FK_VERSION)
						.withAktorsid(FK_AKTORSID)
						.withLefiJsonFraga(buildLefiJsonRequest(personalNumber, fromDate, toDate))))
				.withSKV(new Skatteverket()
					.withInkludera(true)
					.withFraga(buildSkatteverketQuery(toDate)))
				.withSO(new AkassornasSamorganisation().withInkludera(true))
				.withTNS(new Transportstyrelsen().withInkludera(true))
				.withMIV(new Migrationsverket().withInkludera(true)));
	}

	/**
	 * A unique identifier for this request, trimmed to the longest form CSN accepts - see
	 * {@link #MAX_ARENDEIDENTITET_LENGTH}. 124 bits of the UUID survive the trim, which is far more than uniqueness
	 * needs here.
	 */
	private static String newArendeidentitet() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, MAX_ARENDEIDENTITET_LENGTH);
	}

	/**
	 * Skatteverket's query block, mandatory in ssbt/11 whenever SKV is included - omitting it makes Forsakringskassan
	 * reject the whole request with a generic {@code Internal error} before any agency is asked.
	 *
	 * <p>
	 * The years are the two most recent completed taxation years, which is what Forsakringskassan's own example asks for
	 * (2024 + 2025 for a 2026 query). Everything else is requested, mirroring that example. Confirm the year window with
	 * verksamheten before this goes anywhere near a real case.
	 */
	private static SkatteverketFraga buildSkatteverketQuery(final LocalDate toDate) {
		return new SkatteverketFraga()
			.withAr(toDate.getYear() - 2, toDate.getYear() - 1)
			.withForetagsinformation(true)
			.withIndividuppgifter(true)
			.withSkattekonto(true)
			.withSkatteuppgifter(true)
			.withBeskattningsbilagor(true);
	}

	/**
	 * Map the configured personuppgiftsbitrade onto the contract type. {@code ordningsnummer} is optional and only
	 * meaningful when several biträden are listed, so an unset sequence number is left out rather than defaulted.
	 */
	private static Personuppgiftsbitrade toPersonuppgiftsbitrade(final DataProcessor dataProcessor) {
		return new Personuppgiftsbitrade()
			.withOrdningsnummer(dataProcessor.sequenceNumber())
			.withOrganisationsnummer(dataProcessor.organisationNumber())
			.withNamn(dataProcessor.name());
	}

	private static byte[] buildLefiJsonRequest(final String personalNumber, final LocalDate fromDate, final LocalDate toDate) {
		return Constants.LEFI_JSON_TEMPLATE.formatted(personalNumber, fromDate, toDate).getBytes(StandardCharsets.UTF_8);
	}

	private static XMLGregorianCalendar toXmlDate(final LocalDate date) {
		return DATATYPE_FACTORY.newXMLGregorianCalendar(date.toString());
	}
}
