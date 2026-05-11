package se.sundsvall.financialaid.integration.ssbtek.configuration;

import feign.Feign;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.EncodeException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Base64;
import java.util.HashMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKConfiguration.SOAPFaultErrorDecoder;
import se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKConfiguration.SSBTEKSoapDecoder;
import se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKConfiguration.SSBTEKSoapEncoder;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

class SSBTEKConfigurationTest {

	private static final String SOAP_FAULT_BODY = """
		<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
			<soap:Body>
				<soap:Fault>
					<faultcode>soap:Server</faultcode>
					<faultstring>Backend exploded</faultstring>
				</soap:Fault>
			</soap:Body>
		</soap:Envelope>
		""";

	private static final String SOAP_SUCCESS_BODY = """
		<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
			<soap:Body>
				<ssbt:hamtaBastjanstInformationResponse xmlns:ssbt="http://schema.forsakringskassan.se/integration/ssbt/10"/>
			</soap:Body>
		</soap:Envelope>
		""";

	@Nested
	class FeignBuilderCustomizerTest {

		private final SSBTEKConfiguration config = new SSBTEKConfiguration();

		@Test
		void feignBuilderCustomizer_withoutKeystore_returnsCustomizer() {
			final var properties = new SSBTEKProperties(5, 30, null, null);

			final var customizer = config.feignBuilderCustomizer(properties);

			assertThat(customizer).isNotNull();
			customizer.customize(Feign.builder());
		}

		@Test
		void feignBuilderCustomizer_withBlankKeystore_returnsCustomizer() {
			final var properties = new SSBTEKProperties(5, 30, "  ", "");

			final var customizer = config.feignBuilderCustomizer(properties);

			assertThat(customizer).isNotNull();
		}

		@Test
		void feignBuilderCustomizer_withValidKeystore_returnsCustomizerAndAppliesMtlsClient() throws Exception {
			final var password = "changeit";
			final var keystoreBase64 = createEmptyPkcs12KeystoreAsBase64(password);
			final var properties = new SSBTEKProperties(5, 30, keystoreBase64, password);

			final var customizer = config.feignBuilderCustomizer(properties);

			assertThat(customizer).isNotNull();
			final var builder = Feign.builder();
			customizer.customize(builder);
		}

		@Test
		void feignBuilderCustomizer_withInvalidKeystoreData_throwsOnCustomize() {
			final var badBase64 = Base64.getEncoder().encodeToString(new byte[] {
				1, 2, 3, 4, 5
			});
			final var properties = new SSBTEKProperties(5, 30, badBase64, "x");

			final var customizer = config.feignBuilderCustomizer(properties);

			assertThatThrownBy(() -> customizer.customize(Feign.builder()))
				.isInstanceOf(IllegalStateException.class);
		}

		private static String createEmptyPkcs12KeystoreAsBase64(final String password) throws Exception {
			final var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null, password.toCharArray());
			final var out = new ByteArrayOutputStream();
			keyStore.store(out, password.toCharArray());
			return Base64.getEncoder().encodeToString(out.toByteArray());
		}
	}

	@Nested
	class SOAPFaultErrorDecoderTest {

		private final SOAPFaultErrorDecoder decoder = new SOAPFaultErrorDecoder();

		@Test
		void decode_withSoapFault_returnsBadGatewayProblem() {
			final var response = responseWithBody(SOAP_FAULT_BODY);

			final var result = decoder.decode("methodKey", response);

			assertThat(result).isInstanceOf(ThrowableProblem.class);
			assertThat(((ThrowableProblem) result).getStatus().value()).isEqualTo(BAD_GATEWAY.value());
			assertThat(result.getMessage()).contains("Backend exploded");
		}

		@Test
		void decode_withoutSoapFault_returnsProblemWithReason() {
			final var response = responseWithBody(SOAP_SUCCESS_BODY, 502, "Bad Gateway");

			final var result = decoder.decode("methodKey", response);

			assertThat(result).isInstanceOf(ThrowableProblem.class);
			assertThat(result.getMessage()).contains("Bad Gateway");
		}

		@Test
		void decode_withNullBody_returnsProblemWithReason() {
			final var response = Response.builder()
				.status(500)
				.reason("Internal Server Error")
				.request(stubRequest())
				.headers(new HashMap<>())
				.build();

			final var result = decoder.decode("methodKey", response);

			assertThat(result).isInstanceOf(ThrowableProblem.class);
			assertThat(result.getMessage()).contains("Internal Server Error");
		}

		@Test
		void decode_withMalformedXml_returnsProblemFromCatchBranch() {
			final var response = responseWithBody("not xml at all");

			final var result = decoder.decode("methodKey", response);

			assertThat(result).isInstanceOf(ThrowableProblem.class);
		}
	}

	@Nested
	class SSBTEKSoapDecoderTest {

		private final SSBTEKSoapDecoder decoder = new SSBTEKSoapDecoder();

		@Test
		void decode_withValidSoapResponse_returnsUnmarshalledObject() {
			final var response = responseWithBody(SOAP_SUCCESS_BODY);

			final var result = decoder.decode(response, SammansattBastjanstSvar.class);

			assertThat(result).isInstanceOf(SammansattBastjanstSvar.class);
		}

		@Test
		void decode_withInvalidSoapEnvelope_throwsDecodeException() {
			final var response = responseWithBody("not a soap envelope");

			assertThatThrownBy(() -> decoder.decode(response, SammansattBastjanstSvar.class))
				.isInstanceOf(DecodeException.class)
				.hasMessageContaining("Failed to decode SOAP response");
		}

		@Test
		void decode_withBrokenStream_throwsDecodeException() throws IOException {
			final var body = mock(Response.Body.class);
			when(body.asInputStream()).thenThrow(new IOException("stream broken"));
			final var response = Response.builder()
				.status(200)
				.reason("OK")
				.request(stubRequest())
				.headers(new HashMap<>())
				.body(body)
				.build();

			assertThatThrownBy(() -> decoder.decode(response, SammansattBastjanstSvar.class))
				.isInstanceOf(DecodeException.class);
		}
	}

	@Nested
	class SSBTEKSoapEncoderTest {

		private final SSBTEKSoapEncoder encoder = new SSBTEKSoapEncoder();

		@Test
		void encode_withValidRequest_writesSoapBodyToTemplate() {
			final var template = new RequestTemplate();
			final var request = new SammansattBastjanstFraga();

			encoder.encode(request, SammansattBastjanstFraga.class, template);

			final var body = new String(template.body(), StandardCharsets.UTF_8);
			assertThat(body).contains("hamtaBastjanstInformation");
			assertThat(body).containsPattern("(?i)Envelope");
		}

		@Test
		void encode_withNonJaxbObject_throwsEncodeException() {
			final var template = new RequestTemplate();

			assertThatThrownBy(() -> encoder.encode("not a JAXB object", String.class, template))
				.isInstanceOf(EncodeException.class)
				.hasMessageContaining("Failed to encode SOAP message");
		}
	}

	private static Response responseWithBody(final String body) {
		return responseWithBody(body, 200, "OK");
	}

	private static Response responseWithBody(final String body, final int status, final String reason) {
		return Response.builder()
			.status(status)
			.reason(reason)
			.request(stubRequest())
			.headers(new HashMap<>())
			.body(new InputStreamBody(body.getBytes(StandardCharsets.UTF_8)))
			.build();
	}

	private static Request stubRequest() {
		return Request.create(Request.HttpMethod.POST, "http://localhost/ssbtek", new HashMap<>(), Request.Body.empty(), null);
	}

	private static final class InputStreamBody implements Response.Body {

		private final byte[] data;
		private final ByteArrayInputStream stream;

		InputStreamBody(final byte[] data) {
			this.data = data;
			this.stream = new ByteArrayInputStream(data);
		}

		@Override
		public Integer length() {
			return data.length;
		}

		@Override
		public boolean isRepeatable() {
			return true;
		}

		@Override
		public InputStream asInputStream() {
			return stream;
		}

		@Override
		public Reader asReader(final Charset charset) {
			return new InputStreamReader(stream, charset);
		}

		@Override
		public void close() {
			// no-op
		}
	}

}
