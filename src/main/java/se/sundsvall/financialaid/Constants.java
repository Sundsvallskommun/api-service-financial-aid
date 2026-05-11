package se.sundsvall.financialaid;

public final class Constants {

	private Constants() {}

	// Arbetsförmedlingen
	public static final String AGENCY_AF = "af";

	// Centrala studiestödsnämnden
	public static final String AGENCY_CSN = "csn";

	// Försäkringskassan
	public static final String AGENCY_FK = "fk";

	// Skatteverket
	public static final String AGENCY_SKV = "skv";

	// Arbetslöshetskassornas samorganisation
	public static final String AGENCY_SO = "so";

	// Transportstyrelsen
	public static final String AGENCY_TNS = "tns";

	// Migrationsverket
	public static final String AGENCY_MIV = "miv";

	// LEFI JSON-mall; placeholder: personnummer, fromDatum, tomDatum
	public static final String LEFI_JSON_TEMPLATE = """
		{
			"personnummer": "%s",
			"period": {
				"from": "%s",
				"tom": "%s"
			},
			"formansinformation": ["FK:ABB"],
			"ansprak": ["PM:AP"],
			"beslut": ["PM:AP"],
			"preliminaraUtbetalningar": ["FK:ABB"],
			"utbetalningar": ["FK:ABB"],
			"ovrigInformation": ["FK:GEPI"]
		}
		""";
}
