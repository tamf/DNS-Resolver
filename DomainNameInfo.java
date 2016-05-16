import java.net.InetAddress;

	/**
	 * Represents information for a domain name.
	 */
	public class DomainNameInfo {
		private String fqdn;
		private final InetAddress ipAddress;
		private int ttl;
		
		public DomainNameInfo(String fqdn, InetAddress ipAddress, int ttl) {
			this.ttl = ttl;
			this.fqdn = fqdn;
			this.ipAddress = ipAddress;
		}
		
		@Override
		public String toString() {
			return fqdn + " " + ttl + " " + ipAddress.toString().substring(1);
		}
		
		public void setFQDN(String fqdn) {
			this.fqdn = fqdn;
		}
		
		public void setTTL(int ttl) {
			this.ttl = ttl;
		}
		
		public InetAddress getIPaddr() {
			return ipAddress;
		}
		
		public String getFQDN() {
			return fqdn;
		}
		
		public int getTTL() {
			return ttl;
		}
		
	}