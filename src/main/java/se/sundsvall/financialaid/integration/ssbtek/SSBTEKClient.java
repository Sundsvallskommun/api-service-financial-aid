package se.sundsvall.financialaid.integration.ssbtek;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.xml.bind.JAXBElement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import ssbtek.ObjectFactory;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvar;
import ssbtek.SammansattBastjanstTestFraga;
import ssbtek.SammansattBastjanstTestSvar;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKConfiguration.CLIENT_ID;

@Component
public class SSBTEKClient {

	static final String SANITIZED_DETAIL = "Communication failure with SSBTEK";
	private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

	private final WebServiceTemplate template;

	public SSBTEKClient(@Qualifier("ssbtekWebServiceTemplate") final WebServiceTemplate template) {
		this.template = template;
	}

	@CircuitBreaker(name = CLIENT_ID)
	public SammansattBastjanstSvar getBaseServiceInformation(final SammansattBastjanstFraga request) {
		try {
			final var requestElement = OBJECT_FACTORY.createHamtaBastjanstInformation(request);
			final var response = template.marshalSendAndReceive(requestElement);
			return extractResponse(response, SammansattBastjanstSvar.class);
		} catch (final SoapFaultClientException exception) {
			throw soapProblem();
		} catch (final WebServiceIOException exception) {
			// A transport failure (TLS handshake, connection reset, timeout) is an upstream problem like a SOAP fault
			// is: report it as 502 and keep the raw message out of the response, which would otherwise expose SSBTEK
			// connection internals.
			throw soapProblem();
		}
	}

	/**
	 * The contract's own connectivity probe. {@code SammansattBastjanstTestFraga} is an empty sequence - no
	 * personidentitet, no tidsperiod, no syfte - and the response says, per agency, whether its backend test service was
	 * reached and what went wrong if it was not. It therefore answers "does mTLS work and which of the seven backends
	 * respond" without processing anyone's personal data, and without the 11 kap. 11 a § SoL basis that
	 * {@code hamtaBastjanstInformation} asserts and an actual ärende is required for.
	 */
	@CircuitBreaker(name = CLIENT_ID)
	public SammansattBastjanstTestSvar testBaseServiceInformation() {
		try {
			final var requestElement = OBJECT_FACTORY.createTestaBastjanstInformation(new SammansattBastjanstTestFraga());
			final var response = template.marshalSendAndReceive(requestElement);
			return extractResponse(response, SammansattBastjanstTestSvar.class);
		} catch (final SoapFaultClientException exception) {
			throw soapProblem();
		} catch (final WebServiceIOException exception) {
			throw soapProblem();
		}
	}

	/**
	 * Unwrap the response and check it is the operation's own response type before casting. An endpoint that answers one
	 * operation with another's response - a mock matching too broadly, or a contract-version mismatch at the far end - is
	 * an upstream problem like a SOAP fault is, and is reported as one. Casting blind turned that case into a
	 * ClassCastException surfacing as a 500 with a stack trace, rather than a clean 502.
	 */
	private static <T> T extractResponse(final Object response, final Class<T> expectedType) {
		final var value = unwrap(response);
		if (expectedType.isInstance(value)) {
			return expectedType.cast(value);
		}
		throw soapProblem();
	}

	private static Object unwrap(final Object response) {
		if (response instanceof JAXBElement<?> element) {
			return element.getValue();
		}
		return response;
	}

	static ThrowableProblem soapProblem() {
		return Problem.valueOf(BAD_GATEWAY, SANITIZED_DETAIL);
	}
}
