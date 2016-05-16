import java.net.InetAddress;
import java.util.Random;

public class DNSQuery {
	private String fqdn;
	private byte[] queryParams;
	private int queryID;
	private InetAddress dnsServer;

	/**
	 * Constructor: Sets nameServer, fqdn, queryParams fields
	 * @param rootNameServer 
	 * 
	 * @param nameServer
	 * @param fqdn
	 */
	public DNSQuery(InetAddress dnsServer, String fqdn) {
		this.dnsServer = dnsServer;
		this.fqdn = fqdn;
		setQueryParams(); 
	}

	/**
	 * Generates random bytes for queryID and updates the first two bytes in
	 * queryParams.
	 */
	private void generateQueryID() {
		Random rand = new Random();
		byte[] queryID_bytes = new byte[2];
		rand.nextBytes(queryID_bytes);

		queryID = convertToInt(queryID_bytes);

		for (int i = 0; i < queryID_bytes.length; i++) {
			queryParams[i] = queryID_bytes[i];
		}
	}

	/**
	 * Converts byte array into an int. Byte array is interpreted as being in
	 * Big Endian.
	 * 
	 * @param queryID_bytes
	 * @return int representation of byte array
	 */
	private int convertToInt(byte[] bytes) {
		// byte array must be initialized and have at most four elements
		assert (bytes != null && bytes.length <= 4);

		int answer = 0;

		// for each byte, apply bitmask and shift left to "append" to the answer
		for (int i = bytes.length - 1; i >= 0; i--) {
			answer = answer | ((bytes[i] & 0xFF) << ((bytes.length - 1 - i) * 8));
		}

		return answer;
	}

	/**
	 * Set bytes in queryParams. Specifically, we will set the QueryID, Query Count, QName, QType, QClass.
	 * The size of queryParams will be set to the size of the query.
	 */
	private void setQueryParams() {
		queryParams = new byte[512]; // size changed later
		
		generateQueryID();
		setQueryCount();
		int maxQueryParamsIndex = setQNameQTypeQClass();
		
		// Change size of queryParams to be size of query (get rid of trailing zeros)
		byte[] temp = new byte[maxQueryParamsIndex + 1];
		for (int i = 0; i <= maxQueryParamsIndex; i++) {
			temp[i] = queryParams[i];
		}
		queryParams = temp;
	}

	
	/**
	 * Set QName, QType, QClass in queryParams
	 * @return the first empty index in queryParams
	 */
	private int setQNameQTypeQClass() {
		// set QName and get current (empty) index for queryParams
		int paramsIndex = setQName();
		
		// set QType and QClass in queryParams corresponding to Internet
		byte[] QTypeQClass = {0, 1, 0, 1}; // TODO: Not sure if QType, QClass are ever changed?
		for (byte b : QTypeQClass) {
			queryParams[paramsIndex] = b;
			paramsIndex++;
		}
		
		return paramsIndex;
	}

	/**
	 * Set QName in queryParams. 
	 * @return the first empty index in queryParams
	 */
	private int setQName() {
		// QName starts at the 12th byte in queryParams array
		int currQueryParamsIndex = 12;

		String[] labels = fqdn.split("\\.");
		// For each label separated by period, add length of label plus byte
		// representation of each char in the label to queryParams
		for (String label : labels) {
			queryParams[currQueryParamsIndex++] = (byte) label.length();
			for (char ch : label.toCharArray()) {
				queryParams[currQueryParamsIndex++] = (byte) ch;
			}
		}
		// 0 byte indicates the end of QNAME
		queryParams[currQueryParamsIndex++] = 0;

		return currQueryParamsIndex;
	}

	/**
	 * Set query count (QDCOUNT) to 1. This is done in the (zero-based) 5th byte.
	 */
	private void setQueryCount() {
		int indexOfQDCountLowerByte = 5;
		queryParams[indexOfQDCountLowerByte] = 1;
	}

	public byte[] getQueryParams() {
		return queryParams;
	}
	
	public int getQueryID() {
		return queryID;
	}
	

	public InetAddress getServerAddress() {
		return dnsServer;
	}

	public String getQueryAsString() {
		return fqdn;
	}

}
