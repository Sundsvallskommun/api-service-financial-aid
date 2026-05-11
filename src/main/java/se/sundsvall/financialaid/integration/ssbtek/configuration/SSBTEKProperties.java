package se.sundsvall.financialaid.integration.ssbtek.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.ssbtek")
public record SSBTEKProperties(int connectTimeout, int readTimeout, String keyStoreAsBase64, String keyStorePassword) {

}
