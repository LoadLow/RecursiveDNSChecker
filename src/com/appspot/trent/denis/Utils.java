package com.appspot.trent.denis;

import java.io.DataInputStream;

public class Utils {
	// query types
	public static int QUERY_TYPE_A		= 1;
	public static int QUERY_TYPE_NS		= 2;
	public static int QUERY_TYPE_CNAME	= 5;
	public static int QUERY_TYPE_PTR	= 12;
	public static int QUERY_TYPE_HINFO	= 13;
	public static int QUERY_TYPE_MX		= 15;
	public static int QUERY_TYPE_AXFR	= 252;
	
	public static String readDomainName(DataInputStream dis, byte firstCount) throws Exception {
		StringBuffer strBuf = new StringBuffer();
		while (true) {
			// read character count
			byte count = 0;
			if (firstCount != 0) {
				count = firstCount;
				firstCount = 0;
			} else 
				count = dis.readByte();
			
			if (count == 0)
				break;
			if (strBuf.length() > 0)
				strBuf.append('.');
			while (count-- > 0) {
				byte c = dis.readByte();
				strBuf.append((char)c);
			}
		}
		return strBuf.toString();
	}
}
