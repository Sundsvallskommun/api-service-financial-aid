package se.sundsvall.financialaid.integration.ssbtek.configuration;

import feign.Client;
import feign.FeignException;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Base64;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.dept44.util.KeyStoreUtils;
import ssbtek.ObjectFactory;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvar;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Import(FeignConfiguration.class)
public class SSBTEKConfiguration {

	public static final String CLIENT_ID = "ssbtek";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final SSBTEKProperties properties) {
		return FeignMultiCustomizer.create()
			.withEncoder(new SSBTEKSoapEncoder())
			.withDecoder(new SSBTEKSoapDecoder())
			.withErrorDecoder(new SOAPFaultErrorDecoder())
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.withCustomizer(builder -> builder.client(createMtlsClient(properties)))
			.composeCustomizersToOne();
	}

	private static Client createMtlsClient(final SSBTEKProperties properties) {
		try {
			var keystoreBytes = Base64.getDecoder().decode(properties.keyStoreAsBase64());
			var keyStore = KeyStoreUtils.loadKeyStore(keystoreBytes, properties.keyStorePassword());
			var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, properties.keyStorePassword().toCharArray());
			var sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), null, null);
			return new Client.Default(sslContext.getSocketFactory(), null);
		} catch (final NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | KeyManagementException exception) {
			throw new IllegalStateException("Failed to create mTLS client for SSBTEK", exception);
		}
	}

	static ThrowableProblem soapProblem(String message) {
		return Problem.valueOf(BAD_GATEWAY, "Unknown problem in communication with SSBTEK: " + message);
	}

	static class SOAPFaultErrorDecoder implements ErrorDecoder {

		@Override
		public Exception decode(String methodKey, Response response) {
			if (response.body() != null) {
				try (var body = response.body().asInputStream()) {
					SOAPMessage message = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage(null, body);
					if (message.getSOAPBody() != null && message.getSOAPBody().hasFault()) {
						return soapProblem(message.getSOAPBody().getFault().getFaultString());
					}
				} catch (Exception exception) {
					return soapProblem(exception.getMessage());
				}
			}
			return soapProblem(response.reason());
		}
	}

	static class SSBTEKSoapDecoder implements Decoder {

		@Override
		public Object decode(Response response, Type type) throws FeignException {
			try (var body = response.body().asInputStream()) {
				var soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage(null, body);
				var soapBody = soapMessage.getSOAPBody();

				if (soapBody != null && soapBody.hasFault()) {
					throw soapProblem(soapBody.getFault().getFaultString());
				}

				var context = JAXBContext.newInstance(SammansattBastjanstSvar.class);
				var unmarshaller = context.createUnmarshaller();
				return unmarshaller.unmarshal(soapBody.extractContentAsDocument(), SammansattBastjanstSvar.class).getValue();
			} catch (Exception exception) {
				if (exception instanceof RuntimeException runtime) {
					throw runtime;
				}
				throw new DecodeException(response.status(), "Failed to decode SOAP response: " + exception.getMessage(), response.request(), exception);
			}
		}
	}

	static class SSBTEKSoapEncoder implements Encoder {

		private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

		@Override
		public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
			try {
				var soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage();
				var soapBody = soapMessage.getSOAPBody();

				var element = OBJECT_FACTORY.createHamtaBastjanstInformation((SammansattBastjanstFraga) object);
				var marshaller = JAXBContext.newInstance(SammansattBastjanstFraga.class).createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
				marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
				marshaller.marshal(element, soapBody);

				soapMessage.setProperty("javax.xml.soap.write-xml-declaration", "true");
				soapMessage.setProperty("javax.xml.soap.character-set-encoding", StandardCharsets.UTF_8.name());

				var outputStream = new ByteArrayOutputStream();
				soapMessage.writeTo(outputStream);
				template.body(outputStream.toString(StandardCharsets.UTF_8));
			} catch (Exception exception) {
				throw new EncodeException("Failed to encode SOAP message", exception);
			}
		}
	}
}
