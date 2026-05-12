package se.sundsvall.financialaid.openapi;

import static java.nio.file.Files.writeString;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.dept44.util.ResourceUtils.asString;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.financialaid.Application;
import tools.jackson.dataformat.yaml.YAMLMapper;

@ActiveProfiles("junit")
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = Application.class)
class OpenApiSpecificationIT {

	private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

	@Value("${openapi.name}")
	private String openApiName;
	@Value("${openapi.version}")
	private String openApiVersion;

	@Value("classpath:/api/openapi.yaml")
	private Resource openApiResource;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void compareOpenApiSpecifications() throws IOException {
		final var existingOpenApiSpecification = asString(openApiResource);
		final var currentOpenApiSpecification = getCurrentOpenApiSpecification();

		writeString(Path.of("target/openapi.yaml"), currentOpenApiSpecification);

		assertThatJson(toJson(currentOpenApiSpecification))
			.withOptions(IGNORING_ARRAY_ORDER)
			.whenIgnoringPaths("servers")
			.isEqualTo(toJson(existingOpenApiSpecification));
	}

	/**
	 * Fetches and returns the current OpenAPI specification in YAML format.
	 *
	 * @return the current OpenAPI specification
	 */
	private String getCurrentOpenApiSpecification() {
		final var uri = fromPath("/api-docs.yaml")
			.buildAndExpand(openApiName, openApiVersion)
			.toUri();

		return restTemplate.getForObject(uri, String.class);
	}

	/**
	 * Attempts to convert the given YAML (no YAML-check...) to JSON.
	 *
	 * @param yaml the YAML to convert
	 * @return a JSON string
	 */
	private String toJson(final String yaml) {
		return YAML_MAPPER.readTree(yaml).toString();
	}
}
