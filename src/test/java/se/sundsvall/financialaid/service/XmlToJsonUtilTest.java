package se.sundsvall.financialaid.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ssbtek.SvarMeddelande;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlToJsonUtilTest {

	@Test
	void convert_withNestedElements_shouldReturnNestedMap() {
		final var xml = """
			<root>
				<parent>
					<child>value</child>
				</parent>
			</root>
			""";

		final var result = XmlToJsonUtil.convert(xml);

		final var parent = asMap(result.get("parent"));
		assertThat(parent.get("child")).isEqualTo("value");
	}

	@Test
	void convert_withAttributes_shouldIncludeThemAsEntries() {
		final var xml = "<root><item id=\"42\" name=\"foo\">text</item></root>";

		final var result = XmlToJsonUtil.convert(xml);

		final var item = asMap(result.get("item"));
		assertThat(item.get("id")).isEqualTo("42");
		assertThat(item.get("name")).isEqualTo("foo");
		assertThat(item.get("#text")).isEqualTo("text");
	}

	@Test
	void convert_withAttributesOnlyAndNoText_shouldReturnAttributeMap() {
		final var xml = "<root><item id=\"42\"/></root>";

		final var result = XmlToJsonUtil.convert(xml);

		final var item = asMap(result.get("item"));
		assertThat(item.get("id")).isEqualTo("42");
		assertThat(item).doesNotContainKey("#text");
	}

	@Test
	void convert_withRepeatedChildren_shouldReturnList() {
		final var xml = """
			<root>
				<item>a</item>
				<item>b</item>
				<item>c</item>
			</root>
			""";

		final var result = XmlToJsonUtil.convert(xml);

		final var items = asList(result.get("item"));
		assertThat(items).containsExactly("a", "b", "c");
	}

	@Test
	void convert_withEmptyElement_shouldReturnNullValue() {
		final var xml = "<root><empty/></root>";

		final var result = XmlToJsonUtil.convert(xml);

		assertThat(result).containsEntry("empty", null);
	}

	@Test
	void convert_withByteArray_shouldDelegateToStringOverload() {
		final var xml = "<root><value>hello</value></root>".getBytes(StandardCharsets.UTF_8);

		final var result = XmlToJsonUtil.convert(xml);

		assertThat(result.get("value")).isEqualTo("hello");
	}

	@Test
	void convert_withNamespacedXml_shouldStripNamespacesAndReturnLocalNames() {
		final var xml = """
			<r:root xmlns:r="urn:test" xmlns:a="urn:a">
				<a:item>value</a:item>
			</r:root>
			""";

		final var result = XmlToJsonUtil.convert(xml);

		assertThat(result.get("item")).isEqualTo("value");
	}

	@Test
	void convert_withTextOnlyRoot_shouldReturnEmptyMap() {
		final var xml = "<root>just text</root>";

		final var result = XmlToJsonUtil.convert(xml);

		assertThat(result).isEmpty();
	}

	@Test
	void convert_withInvalidXml_shouldThrowIllegalState() {
		assertThatThrownBy(() -> XmlToJsonUtil.convert("not xml"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Failed to parse XML");
	}

	@Test
	void convertJaxb_withNull_shouldReturnEmptyMap() {
		assertThat(XmlToJsonUtil.convertJaxb(null)).isEmpty();
	}

	@Test
	void convertJaxb_withJaxbObject_shouldReturnMap() {
		final var result = XmlToJsonUtil.convertJaxb(new SvarMeddelande());

		assertThat(result).isNotNull();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(final Object obj) {
		return (Map<String, Object>) obj;
	}

	@SuppressWarnings("unchecked")
	private static List<Object> asList(final Object obj) {
		return (List<Object>) obj;
	}
}
