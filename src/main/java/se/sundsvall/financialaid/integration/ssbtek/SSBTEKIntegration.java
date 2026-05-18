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
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvarData;
import ssbtek.Skatteverket;
import ssbtek.SpecifikaFrageparametrar;
import ssbtek.Tidsperiod;
import ssbtek.Transportstyrelsen;

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

	public SSBTEKIntegration(final SSBTEKClient client) {
		this.client = client;
	}

	public Map<String, Map<String, Object>> getFinancialAid(final String personalNumber, final LocalDate fromDate, final LocalDate toDate) {
		LOG.info("Received financial aid request: personalNumber={}, fromDate={}, toDate={}",
			sanitizeForLogging(personalNumber),
			sanitizeForLogging(fromDate.toString()),
			sanitizeForLogging(toDate.toString()));
		final var request = buildRequest(personalNumber, fromDate, toDate);
		logRequestXml(request);
		final var response = client.getBaseServiceInformation(request);

		return mapResponse(response.getSvarsdata());
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
		return result;
	}

	private static SammansattBastjanstFraga buildRequest(final String personalNumber, final LocalDate fromDate, final LocalDate toDate) {
		return new SammansattBastjanstFraga()
			.withGenerellaFrageparametrar(new GenerellaFrageparametrar()
				.withKorrelationsid(UUID.randomUUID().toString())
				.withFraganSkallInkluderasISvaret(false)
				.withIngivare(new Ingivare()
					.withOrganisationsnummer(ORGANISATION_NR)
					.withNamn(ORGANISATION_NAME)
					.withHandlaggare("system"))
				.withPersonidentitet(personalNumber)
				.withArendeidentitet(UUID.randomUUID().toString())
				.withTidsperiod(new Tidsperiod()
					.withFromDatum(toXmlDate(fromDate))
					.withTomDatum(toXmlDate(toDate)))
				.withSyfte("Beslut om eller kontroll av Ekonomisk försörjningsstöd")
				.withLagtext("11 kap. 11 a § socialtjänstlagen (2001:453), 5 § förordning (2008:975) om uppgiftsskyldighet i vissa fall enligt socialtjänstlagen."))
			.withSpecifikaFrageparametrar(new SpecifikaFrageparametrar()
				.withAF(new Arbetsformedlingen().withInkludera(true))
				.withCSN(new Csn().withInkludera(true))
				.withFK(new Forsakringskassan()
					.withInkludera(true)
					.withFraga(new ForsakringskassanFraga()
						.withVersion(FK_VERSION)
						.withAktorsid(FK_AKTORSID)
						.withLefiJsonFraga(buildLefiJsonRequest(personalNumber, fromDate, toDate))))
				.withSKV(new Skatteverket().withInkludera(true))
				.withSO(new AkassornasSamorganisation().withInkludera(true))
				.withTNS(new Transportstyrelsen().withInkludera(true))
				.withMIV(new Migrationsverket().withInkludera(true)));
	}

	private static byte[] buildLefiJsonRequest(final String personalNumber, final LocalDate fromDate, final LocalDate toDate) {
		return Constants.LEFI_JSON_TEMPLATE.formatted(personalNumber, fromDate, toDate).getBytes(StandardCharsets.UTF_8);
	}

	private static XMLGregorianCalendar toXmlDate(final LocalDate date) {
		return DATATYPE_FACTORY.newXMLGregorianCalendar(date.toString());
	}
}
