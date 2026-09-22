package se.sundsvall.financialaid.apptest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.financialaid.Application;

@WireMockAppTestSuite(
	files = "classpath:/FinancialAidIT/",
	classes = Application.class)
class FinancialAidIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String SERVICE_PATH = "/" + MUNICIPALITY_ID + "/financial-aid?personalNumber=199001011234&fromDate=2025-01-01&toDate=2025-06-30";
	private static final String EXPECTED_RESPONSE = "expected-response.json";
	private static final String CONNECTION_PATH = "/" + MUNICIPALITY_ID + "/financial-aid/connection";

	@Test
	void test01_getFinancialAid_csn() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED_RESPONSE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_getFinancialAid_af() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED_RESPONSE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_getFinancialAid_fk() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED_RESPONSE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_getFinancialAid_skv() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED_RESPONSE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_getFinancialAid_so() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED_RESPONSE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_getFinancialAid_tns() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED_RESPONSE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_getFinancialAid_miv() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED_RESPONSE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_getFinancialAid_allAgencies() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED_RESPONSE)
			.sendRequestAndVerifyResponse();
	}

	/**
	 * The connectivity probe: an empty testaBastjanstInformation carrying no personidentitet, and a response in which
	 * FK failed while the rest answered. TNS and MIV are absent from the response entirely - they have no test service
	 * of their own - and must come back as anropad:false rather than being dropped.
	 */
	@Test
	void test09_testConnection() {
		setupCall()
			.withServicePath(CONNECTION_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(EXPECTED_RESPONSE)
			.sendRequestAndVerifyResponse();
	}
}
