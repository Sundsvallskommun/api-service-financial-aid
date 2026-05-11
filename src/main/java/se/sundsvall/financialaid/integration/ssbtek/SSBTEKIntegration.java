package se.sundsvall.financialaid.integration.ssbtek;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
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
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvarData;
import ssbtek.Skatteverket;
import ssbtek.SpecifikaFrageparametrar;
import ssbtek.Tidsperiod;
import ssbtek.Transportstyrelsen;

@Component
public class SSBTEKIntegration {

	private static final DatatypeFactory DATATYPE_FACTORY = createDatatypeFactory();
	private static final String ORGANISATION_NR = "162021005521";
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

	private final SSBTEKClient client;

	public SSBTEKIntegration(final SSBTEKClient client) {
		this.client = client;
	}

	public Map<String, Map<String, Object>> getFinancialAid(final String personalNumber, final LocalDate fromDate, final LocalDate toDate) {
		final var request = buildRequest(personalNumber, fromDate, toDate);
		final var response = client.getBaseServiceInformation(request);

		return mapResponse(response.getSvarsdata());
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
				.withSyfte("Ekonomiskt bistånd")
				.withLagtext("4 kap. 1 § SoL"))
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
