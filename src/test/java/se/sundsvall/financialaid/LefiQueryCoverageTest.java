package se.sundsvall.financialaid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the breadth of the LEFI query in {@link Constants#LEFI_JSON_TEMPLATE} against the contract it is derived from.
 * <p>
 * The caseworker rulebook reads every income the agencies hold for the period and warns about anything outside its
 * allow-list, so a query asking for a subset drops incomes before any rule can see them - and the resulting calculation
 * looks complete while being wrong. That is not a failure any assertion downstream would catch, which is why the query
 * is pinned to the contract enums here rather than to a copied list of codes.
 */
class LefiQueryCoverageTest {

	private static final String CONTRACT = "contract/v10/se/forsakringskassan/schema/lefi/lefi-formansinformation-v9.openapi.yaml";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void queryRequestsEveryBenefitCodeTheContractDefines() throws IOException {
		final var query = MAPPER.readTree(Constants.LEFI_JSON_TEMPLATE.formatted("199001011234", "2026-06-01", "2026-06-30"));

		final var fkCodes = enumOf("LefiFkFormanGruppKod");
		final var pmCodes = enumOf("LefiPmFormanGruppKod");
		final var allCodes = enumOf("LefiExternFormanGruppKod");

		// sanity: the contract was found and parsed, not silently empty
		assertThat(fkCodes).hasSize(27).contains("FK:BOB", "FK:US", "FK:ABB", "FK:SJP", "FK:FP", "FK:AS");
		assertThat(pmCodes).hasSize(3);
		assertThat(allCodes).hasSize(30).containsAll(fkCodes).containsAll(pmCodes);

		assertThat(codesIn(query, "formansinformation")).containsExactlyInAnyOrderElementsOf(fkCodes);
		assertThat(codesIn(query, "ansprak")).containsExactlyInAnyOrderElementsOf(pmCodes);
		assertThat(codesIn(query, "beslut")).containsExactlyInAnyOrderElementsOf(pmCodes);
		assertThat(codesIn(query, "preliminaraUtbetalningar")).containsExactlyInAnyOrderElementsOf(allCodes);
		assertThat(codesIn(query, "utbetalningar")).containsExactlyInAnyOrderElementsOf(allCodes);
	}

	@Test
	void queryAsksOnlyForTheGeneralPersonInformationAmongTheOtherInformationTypes() throws IOException {
		final var query = MAPPER.readTree(Constants.LEFI_JSON_TEMPLATE.formatted("199001011234", "2026-06-01", "2026-06-30"));

		// The remaining types are not incomes for the period. Collecting them would go past the legal basis the
		// request itself cites, so this one stays deliberately narrow - the opposite decision from the benefit codes.
		assertThat(enumOf("LefiExternOvrigInformationTypKod")).hasSize(8).contains("FK:SGI", "PM:PU");
		assertThat(codesIn(query, "ovrigInformation")).containsExactly("FK:GEPI");
	}

	@Test
	void queryCarriesThePersonAndPeriodItWasGiven() throws IOException {
		final var query = MAPPER.readTree(Constants.LEFI_JSON_TEMPLATE.formatted("198401032399", "2026-06-01", "2026-09-10"));

		assertThat(query.get("personnummer").asText()).isEqualTo("198401032399");
		assertThat(query.get("period").get("from").asText()).isEqualTo("2026-06-01");
		assertThat(query.get("period").get("tom").asText()).isEqualTo("2026-09-10");
	}

	private static List<String> codesIn(final JsonNode query, final String field) {
		return StreamSupport.stream(query.get(field).spliterator(), false)
			.map(JsonNode::asText)
			.toList();
	}

	/** Reads an enum straight out of the LEFI contract, so narrowing the query fails here instead of in production. */
	private static List<String> enumOf(final String schemaName) throws IOException {
		final var contract = new String(new ClassPathResource(CONTRACT).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		final var block = Pattern.compile("^\\s{4}" + schemaName + ":\\s*$.*?^\\s*enum:\\s*$(?<codes>(?:\\s*- (?:FK|PM):\\w+\\s*$)+)",
			Pattern.MULTILINE | Pattern.DOTALL).matcher(contract);

		assertThat(block.find()).as("enum %s must exist in %s", schemaName, CONTRACT).isTrue();

		return block.group("codes").lines()
			.map(String::strip)
			.filter(line -> line.startsWith("- "))
			.map(line -> line.substring(2))
			.toList();
	}
}
