package se.sundsvall.financialaid.integration.ssbtek;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import se.sundsvall.dept44.problem.ThrowableProblem;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvar;
import ssbtek.SammansattBastjanstTestSvar;

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

	@Test
	void getBaseServiceInformation_onTransportFailure_throwsSanitizedBadGatewayProblem() {
		when(template.marshalSendAndReceive(any(Object.class)))
			.thenThrow(new WebServiceIOException("I/O error: Connection reset"));

		assertThatThrownBy(() -> client.getBaseServiceInformation(new SammansattBastjanstFraga()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessageContaining(SSBTEKClient.SANITIZED_DETAIL)
			.hasMessageNotContaining("Connection reset");
	}

	@Test
	void testBaseServiceInformation_withJaxbElementResponse_returnsUnwrappedValue() {
		final var expected = new SammansattBastjanstTestSvar();
		final var wrapped = new JAXBElement<>(new QName("ns", "testaBastjanstInformationResponse"), SammansattBastjanstTestSvar.class, expected);
		when(template.marshalSendAndReceive(any(Object.class))).thenReturn(wrapped);

		assertThat(client.testBaseServiceInformation()).isSameAs(expected);
	}

	@Test
	void testBaseServiceInformation_withRawResponse_returnsAsIs() {
		final var expected = new SammansattBastjanstTestSvar();
		when(template.marshalSendAndReceive(any(Object.class))).thenReturn(expected);

		assertThat(client.testBaseServiceInformation()).isSameAs(expected);
	}

	@Test
	void testBaseServiceInformation_onSoapFault_throwsSanitizedBadGatewayProblem() {
		when(template.marshalSendAndReceive(any(Object.class))).thenThrow(mock(SoapFaultClientException.class));

		assertThatThrownBy(() -> client.testBaseServiceInformation())
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining(SSBTEKClient.SANITIZED_DETAIL);
	}

	@Test
	void testBaseServiceInformation_onTransportFailure_throwsSanitizedBadGatewayProblem() {
		when(template.marshalSendAndReceive(any(Object.class))).thenThrow(new WebServiceIOException("connect to fmansinfo.forsakringskassan.se failed"));

		assertThatThrownBy(() -> client.testBaseServiceInformation())
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining(SSBTEKClient.SANITIZED_DETAIL)
			.hasMessageNotContaining("forsakringskassan.se");
	}

	@Test
	void getBaseServiceInformation_withWrongResponseType_throwsBadGatewayRatherThanClassCastException() {
		when(template.marshalSendAndReceive(any(Object.class))).thenReturn(new SammansattBastjanstTestSvar());

		assertThatThrownBy(() -> client.getBaseServiceInformation(new SammansattBastjanstFraga()))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining(SSBTEKClient.SANITIZED_DETAIL);
	}

	/**
	 * The realistic case: a mock (or an endpoint on another contract version) matching the probe too broadly and
	 * answering it with hamtaBastjanstInformationResponse. That must be a clean 502, not a 500 with a stack trace.
	 */
	@Test
	void testBaseServiceInformation_answeredWithTheOtherOperationsResponse_throwsBadGateway() {
		when(template.marshalSendAndReceive(any(Object.class))).thenReturn(new SammansattBastjanstSvar());

		assertThatThrownBy(() -> client.testBaseServiceInformation())
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining(SSBTEKClient.SANITIZED_DETAIL);
	}
}
