package se.sundsvall.financialaid.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import ssbtek.AkassornasSamorganisationSvar;
import ssbtek.ArbetsformedlingenSvar;
import ssbtek.CsnSvar;
import ssbtek.ForsakringskassanSvar;
import ssbtek.MigrationsverketSvar;
import ssbtek.SkatteverketSvar;
import ssbtek.TransportstyrelsenSvar;

public final class ResponseMapper {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private ResponseMapper() {}

	/**
	 * CSN — Centrala Studiestödsnämnden (Swedish Board of Student Finance). Raw XML bytes in {@code data.svar} carrying
	 * study allowance, study grant, home equipment loan, and study start grant per person. Parsed directly into a generic
	 * Map structure.
	 */
	public static Map<String, Object> mapCsn(final CsnSvar csnResponse) {
		if (csnResponse == null || csnResponse.getData() == null || csnResponse.getData().getSvar() == null) {
			return Map.of();
		}
		return XmlToJsonUtil.convert(csnResponse.getData().getSvar());
	}

	/**
	 * FK — Försäkringskassan (Swedish Social Insurance Agency). JSON bytes in {@code data.lefiJsonSvar} (LEFI format) with
	 * benefit information, claims, decisions, and payments. Decoded directly into a Map via Jackson.
	 */
	public static Map<String, Object> mapFk(final ForsakringskassanSvar fkResponse) {
		if (fkResponse == null || fkResponse.getData() == null || fkResponse.getData().getLefiJsonSvar() == null) {
			return Map.of();
		}
		try {
			return OBJECT_MAPPER.readValue(
				fkResponse.getData().getLefiJsonSvar(),
				new TypeReference<>() {
				});
		} catch (final IOException exception) {
			throw new IllegalStateException("Failed to parse FK JSON response: " + exception.getClass().getSimpleName());
		}
	}

	/**
	 * AF — Arbetsförmedlingen (Swedish Public Employment Service). JAXB {@code SvarMeddelande} object with jobseeker
	 * status, unemployment-fund affiliation, and reference number. Marshaled to XML and converted to a Map.
	 */
	public static Map<String, Object> mapAf(final ArbetsformedlingenSvar afResponse) {
		if (afResponse == null || afResponse.getData() == null) {
			return Map.of();
		}
		return XmlToJsonUtil.convertJaxb(afResponse.getData());
	}

	/**
	 * SKV — Skatteverket (Swedish Tax Agency). XML string in {@code data.svar} (CDATA-wrapped) with personal data and
	 * capital information per assessment year. Parsed directly into a Map.
	 */
	public static Map<String, Object> mapSkv(final SkatteverketSvar skvResponse) {
		if (skvResponse == null || skvResponse.getData() == null || skvResponse.getData().getSvar() == null) {
			return Map.of();
		}
		return XmlToJsonUtil.convert(skvResponse.getData().getSvar());
	}

	/**
	 * SO — Sveriges A-kassors Samorganisation (Swedish Unemployment Insurance Funds joint organization). JAXB objects to
	 * unemployment benefit and payments from the unemployment fund. Marshaled to XML and converted to a Map.
	 */
	public static Map<String, Object> mapSo(final AkassornasSamorganisationSvar soResponse) {
		if (soResponse == null || soResponse.getData() == null) {
			return Map.of();
		}
		return XmlToJsonUtil.convertJaxb(soResponse.getData());
	}

	/**
	 * TNS — Transportstyrelsen (Swedish Transport Agency). JAXB object with vehicle ownership (registration number, make,
	 * vehicle type, etc.). Marshaled to XML and converted to a Map.
	 */
	public static Map<String, Object> mapTns(final TransportstyrelsenSvar tnsResponse) {
		if (tnsResponse == null || tnsResponse.getData() == null) {
			return Map.of();
		}
		return XmlToJsonUtil.convertJaxb(tnsResponse.getData());
	}

	/**
	 * MIV — Migrationsverket (Swedish Migration Agency). JAXB object with residence permit decisions (case type, decision
	 * code, validity period). Marshaled to XML and converted to a Map.
	 */
	public static Map<String, Object> mapMiv(final MigrationsverketSvar mivResponse) {
		if (mivResponse == null || mivResponse.getData() == null) {
			return Map.of();
		}
		return XmlToJsonUtil.convertJaxb(mivResponse.getData());
	}
}
