package se.sundsvall.financialaid.integration.ssbtek;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.xml.bind.JAXBElement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import ssbtek.ObjectFactory;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvar;

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
			return extractResponse(response);
		} catch (final SoapFaultClientException exception) {
			throw soapProblem();
		}
	}

	private static SammansattBastjanstSvar extractResponse(final Object response) {
		if (response instanceof JAXBElement<?> element) {
			return (SammansattBastjanstSvar) element.getValue();
		}
		return (SammansattBastjanstSvar) response;
	}

	static ThrowableProblem soapProblem() {
		return Problem.valueOf(BAD_GATEWAY, SANITIZED_DETAIL);
	}
}
