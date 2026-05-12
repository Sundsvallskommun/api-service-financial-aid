package se.sundsvall.financialaid.integration.ssbtek;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKConfiguration;
import ssbtek.SammansattBastjanstFraga;
import ssbtek.SammansattBastjanstSvar;

import static se.sundsvall.financialaid.integration.ssbtek.configuration.SSBTEKConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.ssbtek.url}", configuration = SSBTEKConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface SSBTEKClient {

	String TEXT_XML_UTF8 = "text/xml;charset=UTF-8";

	@PostMapping(consumes = TEXT_XML_UTF8, produces = TEXT_XML_UTF8)
	SammansattBastjanstSvar getBaseServiceInformation(final SammansattBastjanstFraga request);
}
