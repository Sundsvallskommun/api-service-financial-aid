package se.sundsvall.financialaid.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class XmlToJsonUtil {

	private static final String XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/";

	private XmlToJsonUtil() {}

	public static Map<String, Object> convert(final byte[] xmlBytes) {
		return convert(new String(xmlBytes, StandardCharsets.UTF_8));
	}

	public static Map<String, Object> convert(final String xml) {
		final var root = parseXml(xml).getDocumentElement();
		final var result = convertElement(root);
		return result instanceof Map<?, ?> map ? asMap(map) : Map.of();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> convertJaxb(final Object jaxbObject) {
		if (jaxbObject == null) {
			return Map.of();
		}
		try {
			final var context = JAXBContext.newInstance(jaxbObject.getClass());
			final var marshaller = context.createMarshaller();
			final var writer = new StringWriter();
			marshaller.marshal(
				new JAXBElement<>(new QName("root"), (Class<Object>) jaxbObject.getClass(), jaxbObject),
				writer);
			return convert(writer.toString());
		} catch (final JAXBException exception) {
			throw new IllegalStateException("Failed to marshal JAXB object to XML: " + exception.getClass().getSimpleName());
		}
	}

	private static Object convertElement(final Element element) {
		final var attrs = element.getAttributes();
		final var children = childElements(element);
		final var attrCount = countNonNamespaceAttrs(attrs);

		if (children.isEmpty() && attrCount == 0) {
			return textOrNull(element);
		}

		final Map<String, Object> map = new LinkedHashMap<>();
		putAttributes(map, attrs);

		if (children.isEmpty()) {
			return finalizeLeafMap(map, element);
		}

		for (final Element child : children) {
			mergeChild(map, child.getLocalName(), convertElement(child));
		}
		return map;
	}

	private static Object textOrNull(final Element element) {
		final var text = element.getTextContent().trim();
		return text.isEmpty() ? null : text;
	}

	private static Object finalizeLeafMap(final Map<String, Object> map, final Element element) {
		final var text = element.getTextContent().trim();
		if (!text.isEmpty()) {
			map.put("#text", text);
		}
		return map.isEmpty() ? null : map;
	}

	private static void putAttributes(final Map<String, Object> map, final NamedNodeMap attrs) {
		for (int index = 0; index < attrs.getLength(); index++) {
			final var attr = attrs.item(index);
			if (!XMLNS_NAMESPACE.equals(attr.getNamespaceURI())) {
				map.put(attr.getLocalName(), attr.getNodeValue());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void mergeChild(final Map<String, Object> map, final String key, final Object value) {
		if (!map.containsKey(key)) {
			map.put(key, value);
			return;
		}
		final var existing = map.get(key);
		if (existing instanceof List<?> list) {
			((List<Object>) list).add(value);
			return;
		}
		final var newList = new ArrayList<>();
		newList.add(existing);
		newList.add(value);
		map.put(key, newList);
	}

	private static List<Element> childElements(final Element parent) {
		final List<Element> result = new ArrayList<>();
		final NodeList nodes = parent.getChildNodes();
		for (int index = 0; index < nodes.getLength(); index++) {
			if (nodes.item(index) instanceof Element element) {
				result.add(element);
			}
		}
		return result;
	}

	private static int countNonNamespaceAttrs(final NamedNodeMap attrs) {
		int count = 0;
		for (int index = 0; index < attrs.getLength(); index++) {
			if (!XMLNS_NAMESPACE.equals(attrs.item(index).getNamespaceURI())) {
				count++;
			}
		}
		return count;
	}

	private static Document parseXml(final String xml) {
		try {
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		} catch (final Exception exception) {
			// Drop cause: SAX parser messages can quote source XML containing PII.
			// Fingerprint with exception type for ops triage.
			throw new IllegalStateException("Failed to parse XML: " + exception.getClass().getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(final Map<?, ?> map) {
		return (Map<String, Object>) map;
	}
}
