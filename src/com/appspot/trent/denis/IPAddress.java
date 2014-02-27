package com.appspot.trent.denis;

public class IPAddress {
	public IPAddress(byte nums[]) throws Exception {
		// copy
		if (nums.length == 4)
			buffer = nums;
		else
			throw new Exception("Address " + nums + " is not OK");
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append((int)buffer[0]&0xFF); sb.append('.');
		sb.append((int)buffer[1]&0xFF); sb.append('.');
		sb.append((int)buffer[2]&0xFF); sb.append('.');
		sb.append((int)buffer[3]&0xFF);
		return sb.toString();
	}
	
	public boolean equals(IPAddress other) {
		if ((buffer.length == 4) &&
				(other.buffer.length == 4) &&
				(buffer[0] == other.buffer[0]) &&
				(buffer[1] == other.buffer[1]) &&
				(buffer[2] == other.buffer[2]) &&
				(buffer[3] == other.buffer[3]))
			return true;
		return false;
	}
	
	public byte[] getBuffer() {
		return buffer;
	}
	
	byte buffer[];
}
