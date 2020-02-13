# DNS-Resolver
Performs DNS lookup of a domain name into an IP address. Only resolves names to IPv4 addresses. Based off RFC 1035.

# Input

Usage:

1. `make`
2. `java ‐jar DNSlookup.jar rootDNS name [‐t]`

- rootDNS is the IP address of the DNS server at which to start the search
- name is the fully qualified domain name to look up
- -t specifies whether program is to print a trace of all queries made

# Output

1. If IP address found and -t option absent, output is: 
name TTL IP-address, 
e.g. www.cs.ubc.ca 3585 142.103.6.5

2. If IP address not found, output is: 
name -1 0.0.0.0

3. Error codes in TTL field

-1: name server reports value of 3 in RCODE of header

-2: lookup times out

-3: too many queries attempted without resolution

-4: any other error


