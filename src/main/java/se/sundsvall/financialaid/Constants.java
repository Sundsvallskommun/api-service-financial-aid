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

	// LEFI JSON-mall; placeholder: personnummer, fromDatum, tomDatum.
	//
	// The benefit codes are the complete enums from the LEFI v9 contract
	// (contract/v10/se/forsakringskassan/schema/lefi/lefi-formansinformation-v9.openapi.yaml):
	// formansinformation takes LefiFkFormanGruppKod, ansprak and beslut take LefiPmFormanGruppKod, and
	// preliminaraUtbetalningar and utbetalningar take LefiExternFormanGruppKod, the two sets combined.
	//
	// Every code is requested deliberately. The caseworker rulebook works the opposite way round from a
	// hand-picked query: read every income the two agencies hold for the period, match them against an
	// allow-list, and warn about anything not on it. Requesting a subset drops incomes - housing allowance,
	// maintenance support, daily allowance - before any rule sees them, leaving a case that looks complete
	// while being wrong.
	//
	// ovrigInformation is deliberately not the full enum. The remaining types (sjukpenninggrundande inkomst,
	// pensionsunderlag and the like) are not incomes for the period and are not needed to build the
	// calculation, so requesting them would collect personal data beyond the legal basis cited in the
	// request itself. Only generell personinformation is asked for, to tie the response to its subject.
	public static final String LEFI_JSON_TEMPLATE = """
		{
		  "personnummer": "%s",
		  "period": {
		    "from": "%s",
		    "tom": "%s"
		  },
		  "formansinformation": ["FK:ABB", "FK:AP", "FK:AS", "FK:BOB", "FK:BST", "FK:BTI", "FK:DPE", "FK:EJO", "FK:ET",
		                       "FK:FP", "FK:FSJP", "FK:GP", "FK:HE", "FK:MABB", "FK:MEK", "FK:NP", "FK:OMV",
		                       "FK:PJOB", "FK:PROG", "FK:RBE", "FK:SA", "FK:SBP", "FK:SJP", "FK:TFP", "FK:US",
		                       "FK:VB", "FK:YL"],
		  "ansprak": ["PM:AP", "PM:BTI", "PM:EF"],
		  "beslut": ["PM:AP", "PM:BTI", "PM:EF"],
		  "preliminaraUtbetalningar": ["FK:ABB", "FK:AP", "FK:AS", "FK:BOB", "FK:BST", "FK:BTI", "FK:DPE", "FK:EJO",
		                             "FK:ET", "FK:FP", "FK:FSJP", "FK:GP", "FK:HE", "FK:MABB", "FK:MEK", "FK:NP",
		                             "FK:OMV", "FK:PJOB", "FK:PROG", "FK:RBE", "FK:SA", "FK:SBP", "FK:SJP", "FK:TFP",
		                             "FK:US", "FK:VB", "FK:YL", "PM:AP", "PM:BTI", "PM:EF"],
		  "utbetalningar": ["FK:ABB", "FK:AP", "FK:AS", "FK:BOB", "FK:BST", "FK:BTI", "FK:DPE", "FK:EJO", "FK:ET",
		                  "FK:FP", "FK:FSJP", "FK:GP", "FK:HE", "FK:MABB", "FK:MEK", "FK:NP", "FK:OMV", "FK:PJOB",
		                  "FK:PROG", "FK:RBE", "FK:SA", "FK:SBP", "FK:SJP", "FK:TFP", "FK:US", "FK:VB", "FK:YL",
		                  "PM:AP", "PM:BTI", "PM:EF"],
		  "ovrigInformation": ["FK:GEPI"]
		}
		""";
}
