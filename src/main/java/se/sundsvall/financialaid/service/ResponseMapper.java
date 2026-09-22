package se.sundsvall.financialaid.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import ssbtek.AkassornasSamorganisationSvar;
import ssbtek.ArbetsformedlingenSvar;
import ssbtek.CsnSvar;
import ssbtek.Error;
import ssbtek.ForsakringskassanSvar;
import ssbtek.KallaEnum;
import ssbtek.MigrationsverketSvar;
import ssbtek.SkatteverketSvar;
import ssbtek.TransportstyrelsenSvar;

import static java.util.Optional.ofNullable;

public final class ResponseMapper {

	/**
	 * Key under which a failed agency's {@code <error>} is surfaced. Every {@code *Svar} in the SSBTEK contract is an
	 * {@code xsd:choice} of {@code data} or {@code error}, so an agency that could not answer carries no data at all.
	 * Returning an empty map for it - as this mapper did until now - makes "the agency failed" indistinguishable from
	 * "the agency answered, this person has nothing with us", which is the difference between a case that needs a retry
	 * and a normberäkning that is simply missing an income.
	 */
	public static final String KEY_ERROR = "error";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private ResponseMapper() {}

	/**
	 * CSN — Centrala Studiestödsnämnden (Swedish Board of Student Finance). Raw XML bytes in {@code data.svar} carrying
	 * study allowance, study grant, home equipment loan, and study start grant per person. Parsed directly into a generic
	 * Map structure.
	 */
	public static Map<String, Object> mapCsn(final CsnSvar csnResponse) {
		if (csnResponse == null) {
			return Map.of();
		}
		return mapAgency(csnResponse.getError(), () -> {
			if (csnResponse.getData() == null || csnResponse.getData().getSvar() == null) {
				return Map.of();
			}
			return XmlToJsonUtil.convert(csnResponse.getData().getSvar());
		});
	}

	/**
	 * FK — Försäkringskassan (Swedish Social Insurance Agency). JSON bytes in {@code data.lefiJsonSvar} (LEFI format) with
	 * benefit information, claims, decisions, and payments. Decoded directly into a Map via Jackson.
	 */
	public static Map<String, Object> mapFk(final ForsakringskassanSvar fkResponse) {
		if (fkResponse == null) {
			return Map.of();
		}
		return mapAgency(fkResponse.getError(), () -> {
			if (fkResponse.getData() == null || fkResponse.getData().getLefiJsonSvar() == null) {
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
		});
	}

	/**
	 * AF — Arbetsförmedlingen (Swedish Public Employment Service). JAXB {@code SvarMeddelande} object with jobseeker
	 * status, unemployment-fund affiliation, and reference number. Marshaled to XML and converted to a Map.
	 */
	public static Map<String, Object> mapAf(final ArbetsformedlingenSvar afResponse) {
		if (afResponse == null) {
			return Map.of();
		}
		return mapAgency(afResponse.getError(), () -> XmlToJsonUtil.convertJaxb(afResponse.getData()));
	}

	/**
	 * SKV — Skatteverket (Swedish Tax Agency). XML string in {@code data.svar} (CDATA-wrapped) with personal data and
	 * capital information per assessment year. Parsed directly into a Map.
	 */
	public static Map<String, Object> mapSkv(final SkatteverketSvar skvResponse) {
		if (skvResponse == null) {
			return Map.of();
		}
		return mapAgency(skvResponse.getError(), () -> {
			if (skvResponse.getData() == null || skvResponse.getData().getSvar() == null) {
				return Map.of();
			}
			return XmlToJsonUtil.convert(skvResponse.getData().getSvar());
		});
	}

	/**
	 * SO — Sveriges A-kassors Samorganisation (Swedish Unemployment Insurance Funds joint organization). JAXB objects to
	 * unemployment benefit and payments from the unemployment fund. Marshaled to XML and converted to a Map.
	 */
	public static Map<String, Object> mapSo(final AkassornasSamorganisationSvar soResponse) {
		if (soResponse == null) {
			return Map.of();
		}
		return mapAgency(soResponse.getError(), () -> XmlToJsonUtil.convertJaxb(soResponse.getData()));
	}

	/**
	 * TNS — Transportstyrelsen (Swedish Transport Agency). JAXB object with vehicle ownership (registration number, make,
	 * vehicle type, etc.). Marshaled to XML and converted to a Map.
	 */
	public static Map<String, Object> mapTns(final TransportstyrelsenSvar tnsResponse) {
		if (tnsResponse == null) {
			return Map.of();
		}
		return mapAgency(tnsResponse.getError(), () -> XmlToJsonUtil.convertJaxb(tnsResponse.getData()));
	}

	/**
	 * MIV — Migrationsverket (Swedish Migration Agency). JAXB object with residence permit decisions (case type, decision
	 * code, validity period). Marshaled to XML and converted to a Map.
	 */
	public static Map<String, Object> mapMiv(final MigrationsverketSvar mivResponse) {
		if (mivResponse == null) {
			return Map.of();
		}
		return mapAgency(mivResponse.getError(), () -> XmlToJsonUtil.convertJaxb(mivResponse.getData()));
	}

	/**
	 * The {@code data}/{@code error} choice, resolved: a present {@code error} wins, because an agency that reported one
	 * carries no data to read anyway.
	 */
	private static Map<String, Object> mapAgency(final Error error, final Supplier<Map<String, Object>> dataMapper) {
		return ofNullable(error)
			.map(ResponseMapper::toErrorMap)
			.orElseGet(dataMapper);
	}

	/**
	 * The agency's {@code error} as {@code {"error": {"kalla", "felkod", "felmeddelande"}}}. Nested under
	 * {@link #KEY_ERROR} rather than flattened so it can never collide with an agency's own payload keys, and so a
	 * consumer can test for failure with a single key lookup.
	 */
	private static Map<String, Object> toErrorMap(final Error error) {
		return Map.of(KEY_ERROR, toErrorDetails(error));
	}

	/**
	 * The {@code kalla}/{@code felkod}/{@code felmeddelande} triple on its own, for callers that nest it themselves -
	 * the {@code testaBastjanstInformation} probe reports it alongside {@code anropad}. Absent fields are omitted rather
	 * than mapped to null, so a consumer can read the presence of a key as "the agency told us this".
	 */
	public static Map<String, Object> toErrorDetails(final Error error) {
		final Map<String, Object> details = new LinkedHashMap<>();
		ofNullable(error.getKalla()).map(KallaEnum::value).ifPresent(kalla -> details.put("kalla", kalla));
		ofNullable(error.getFelkod()).ifPresent(felkod -> details.put("felkod", felkod));
		if (!error.getFelmeddelande().isEmpty()) {
			details.put("felmeddelande", List.copyOf(error.getFelmeddelande()));
		}
		return Map.copyOf(details);
	}
}
