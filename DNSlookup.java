import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 
 */

/**
 * @author Donald Acton This example is adapted from Kurose & Ross
 *
 */
public class DNSlookup {

	static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
	static boolean tracingOn = false;
	static InetAddress rootnameserver;
	static InetAddress currServer;
	static String originalFQDN;
	static DatagramSocket socket;
	static int numQueriesSent = 0;
	static final int MAX_PERMITTED_QUERIES = 30;
	static int numConsecTimeouts = 0;
	static int finalTTL = Integer.MAX_VALUE;
	static Set<String> cnames = new HashSet<String>();

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int argCount = args.length;

		if (argCount < 2 || argCount > 3) {
			usage();
			return;
		}
		try {
			rootnameserver = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			// server address is invalid
			usage();
			return;
		}
		currServer = rootnameserver;
		originalFQDN = args[1];
		
		cnames.add(originalFQDN);

		if (argCount == 3 && args[2].equals("-t"))
			tracingOn = true;

		DomainNameInfo finalAnswer = dnsLookup(rootnameserver, originalFQDN);
		// if address not resolved, exit program
		if (finalAnswer == null) {
			System.out.println(originalFQDN + " -4 0.0.0.0");
			System.exit(0);
		}

		// set fqdn, ttl to fqdn of original query and smallest ttl
		finalAnswer.setFQDN(originalFQDN);
		finalAnswer.setTTL(finalTTL);
		// print final answer
		System.out.println(finalAnswer.toString());

	}

	/**
	 * Looks up domain name starting from the given name server. Exits the
	 * program if number of lookups exceeds 30.
	 * 
	 * @param server
	 *            name server to start querying at
	 * @param fqdn
	 *            domain name of interest
	 * @return the information (IP address etc) of the given domain name
	 */
	private static DomainNameInfo dnsLookup(InetAddress server, String fqdn) {
		exitIfTooManyLookups();

		DNSQuery dnsQuery = sendQuery(server, fqdn);
		DomainNameInfo answer = receiveResponse(dnsQuery);

		return answer;
	}

	/**
	 * Exits program if we have passed the max permitted number of queries
	 */
	private static void exitIfTooManyLookups() {
		if (numQueriesSent >= MAX_PERMITTED_QUERIES) {
			System.out.println(originalFQDN + " -3 0.0.0.0");
			System.exit(0);
		}
	}

	/**
	 * Opens a socket, sends DNS query to rootNameServer.
	 * 
	 * @return DNSQuery for this query
	 * @throws IOException
	 */
	private static DNSQuery sendQuery(InetAddress server, String fqdn) {
		DNSQuery dnsQuery = new DNSQuery(server, fqdn);
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO: Edge cases and exception handling?
			e.printStackTrace();
		}

		byte[] queryParams = dnsQuery.getQueryParams();

		DatagramPacket queryPacket = new DatagramPacket(queryParams, queryParams.length, server, 53);
		try {
			socket.send(queryPacket);
		} catch (BindException be) {
			// Cannot assign requested address: Datagram send failed
			System.out.println(originalFQDN + " -4 0.0.0.0");
			System.exit(0);
		} catch (IOException e) {
			// TODO: Edge cases and exception handling?
			e.printStackTrace();
		}
		numQueriesSent++;

		return dnsQuery;
	}

	/**
	 * Receives DNS response and returns the IP address.
	 * 
	 * If error encountered, exit program. If an IP address is not obtained in
	 * the response, a recursive DNS lookup is done against a name server from
	 * the response and the result of that is returned.
	 * 
	 * @return the information for the domain name as the answer to the query
	 */
	private static DomainNameInfo receiveResponse(DNSQuery dnsQuery) {
		byte[] buf = new byte[512];
		DatagramPacket responsePacket = new DatagramPacket(buf, buf.length);

		// Set timeout to five seconds
		try {
			socket.setSoTimeout(5000);
		} catch (SocketException e1) {
			System.out.println("Error in underlying protocol, such as a UDP error");
			e1.printStackTrace();
		}
		try {
			socket.receive(responsePacket);
		} catch (SocketTimeoutException ste) {
			if (tracingOn) {
				// print out query
				System.out.println("\n\nQuery ID     " + dnsQuery.getQueryID() + " " + dnsQuery.getQueryAsString() + " --> "
						+ dnsQuery.getServerAddress().getHostAddress());
			}
			numConsecTimeouts++;
			// if second consecutive timeout, exit program
			if (numConsecTimeouts >= 2) {
				System.out.println(originalFQDN + " -2 0.0.0.0");
				System.exit(0);
			}
			// re-send query
			return dnsLookup(dnsQuery.getServerAddress(), dnsQuery.getQueryAsString());
		} catch (IOException e) {
			// TODO: Edge cases and exception handling?
			e.printStackTrace();
		}

		// no timeout -> reset numConsecTimeouts
		numConsecTimeouts = 0;

		DNSResponse dnsResponse = new DNSResponse(responsePacket.getData(), responsePacket.getLength(), dnsQuery);
		if (tracingOn) {
			dnsResponse.dumpResponse();
		}

		// TODO: when dealing with CNAMEs, we need to report the smallest TTL of
		// CNAME, CNAME in chain of CNAMEs leading to final result, or final
		// address result
		

		// if RCODE != 0, this indicates error. Exit the program.
		int rcode = dnsResponse.getReplyCode();
		if (rcode != 0) {
			if (rcode == 3) {
				System.out.println(originalFQDN + " -1 0.0.0.0");
			} else {
				System.out.println(originalFQDN + " -4 0.0.0.0");
			}
			System.exit(0);
		}

		// if we get an IP address in the answer section, we are done
		DomainNameInfo domainNameInfo = dnsResponse.getDomainNameInfo();
		if (domainNameInfo != null) {
			if (cnames.contains(domainNameInfo.getFQDN())) {
				finalTTL = Math.min(finalTTL, domainNameInfo.getTTL());
			}
			return domainNameInfo;
		}
		


		// if answer section empty but get the address of the next name
		// server, re-query to it
		String fqdn = dnsQuery.getQueryAsString();
		InetAddress nextServerToQuery = dnsResponse.reQueryTo();
		if (nextServerToQuery != null) {
			return dnsLookup(nextServerToQuery, fqdn);
		}

		// if we get a cname in answer section, we start the dns lookup from the
		// beginning with the actual name
		String cname = dnsResponse.getCNAME();
		if (cname != null) {
			if (cnames.contains(dnsQuery.getQueryAsString())) {
				cnames.add(cname);
				finalTTL = Math.min(finalTTL, dnsResponse.getTTL());
			}
			
			return dnsLookup(rootnameserver, cname);
		}

		// if we get a cname for a nameserver but not its IP address, we need to
		// resolve it first
		String authoritativeDNSname = dnsResponse.getAuthoritativeDNSname();
		if (authoritativeDNSname != null) {
			InetAddress addressForNS = dnsLookup(rootnameserver, authoritativeDNSname).getIPaddr();
			return dnsLookup(addressForNS, fqdn);
		}

		return null;
	}

	private static void usage() {
		System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-t]");
		System.out.println("   where");
		System.out.println("       rootDNS - the IP address (in dotted form) of the root");
		System.out.println("                 DNS server you are to start your search at");
		System.out.println("       name    - fully qualified domain name to lookup");
		System.out.println("       -t      -trace the queries made and responses received");
	}

}
