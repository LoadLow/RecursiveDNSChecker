package com.appspot.trent.denis;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class DnsAnswer {
	public DnsAnswer(DataInputStream dis) throws Exception {
		// read domain name
		byte firstCount = dis.readByte();
		if (0 != (firstCount & 0xC0)) {
			// this is a pointer
			pointerPos = firstCount & 0x3F;
			// read an ending 0
			byte ending = dis.readByte();
			pointerPos = (pointerPos << 8) | ending; 
		} else {
			// this is a normal name
			domainName = Utils.readDomainName(dis, firstCount); 
		}
		
		// read query type and query class
		queryType = dis.readShort();
		queryClass = dis.readShort();
		ttl = dis.readInt();
		rrDataLen = dis.readShort();
		if (rrDataLen > 0) {
			rrData = new byte[rrDataLen];
			dis.read(rrData, 0, rrData.length);
		}
		
		if (queryType == Utils.QUERY_TYPE_CNAME) {
			// ignore
		}
	}
	
	public IPAddress getIpAddress() throws Exception {
		return new IPAddress(rrData);
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		// domain name
		if (isPointer()) {
			sb.append("Ptr ");
			sb.append(getPointerPos());
		} else {
			sb.append(getDomainName());
		}
		sb.append(':');
		// ip address
		sb.append((int)rrData[0]&0xFF);sb.append('.');
		sb.append((int)rrData[1]&0xFF);sb.append('.');
		sb.append((int)rrData[2]&0xFF);sb.append('.');
		sb.append((int)rrData[3]&0xFF);
		
		return sb.toString();
	}
	public boolean isPointer() {
		return domainName == null;
	}
	
	public int getPointerPos() {
		return pointerPos;
	}

	public String getDomainName() {
		return domainName;
	}

	public int getQueryType() {
		return queryType;
	}

	public int getQueryClass() {
		return queryClass;
	}

	public int getTtl() {
		return ttl;
	}

	public short getRrDataLen() {
		return rrDataLen;
	}

	public byte[] getRrData() {
		return rrData;
	}

	int pointerPos = 0;
	String domainName;
	int queryType = Utils.QUERY_TYPE_A;
	int queryClass = 1;
	int ttl = 0;
	short rrDataLen = 0;
	byte rrData[] = null;
}
