package se.sundsvall.financialaid.integration.ssbtek;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import se.sundsvall.dept44.problem.ThrowableProblem;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class SSBTEKClientTest {

	@Mock
	private WebServiceTemplate template;

	@InjectMocks
	private SSBTEKClient client;

	@Test
	void getBaseServiceInformation_withJaxbElementResponse_returnsUnwrappedValue() {
		final var expected = new SammansattBastjanstSvar();
		final var wrapped = new JAXBElement<>(new QName("ns", "hamtaBastjanstInformationResponse"), SammansattBastjanstSvar.class, expected);
		when(template.marshalSendAndReceive(any(Object.class))).thenReturn(wrapped);

		final var result = client.getBaseServiceInformation(new SammansattBastjanstFraga());

		assertThat(result).isSameAs(expected);
	}

	@Test
	void getBaseServiceInformation_withRawResponse_returnsAsIs() {
		final var expected = new SammansattBastjanstSvar();
		when(template.marshalSendAndReceive(any(Object.class))).thenReturn(expected);

		final var result = client.getBaseServiceInformation(new SammansattBastjanstFraga());

		assertThat(result).isSameAs(expected);
	}

	@Test
	void getBaseServiceInformation_onSoapFault_throwsSanitizedBadGatewayProblem() {
		when(template.marshalSendAndReceive(any(Object.class))).thenThrow(mock(SoapFaultClientException.class));

		assertThatThrownBy(() -> client.getBaseServiceInformation(new SammansattBastjanstFraga()))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining(SSBTEKClient.SANITIZED_DETAIL)
			.satisfies(thrown -> assertThat(((ThrowableProblem) thrown).getStatus().value()).isEqualTo(BAD_GATEWAY.value()));
	}
}
