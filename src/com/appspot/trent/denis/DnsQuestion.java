package com.appspot.trent.denis;

import java.io.DataInputStream;

public class DnsQuestion {
	public DnsQuestion(DataInputStream dis) throws Exception {
		// read question name
		byte firstCount = dis.readByte();
		domainName = Utils.readDomainName(dis, firstCount);
		// read query type
		queryType = dis.readShort();
		// read query class
		queryClass = dis.readShort();
	}

	public String toString() {
		return domainName;
	}
	// getters
	public String getDomainName() {
		return domainName;
	}
	public short getQueryType() {
		return queryType;
	}
	public short getQueryClass() {
		return queryClass;
	}

	// members
	String domainName;
	short queryType;
	short queryClass;
}
