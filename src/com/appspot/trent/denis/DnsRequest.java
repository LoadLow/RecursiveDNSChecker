package com.appspot.trent.denis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class DnsRequest {
	public DnsRequest(byte buffer[], int dataSize) throws Exception {
		// get fields from buffer
		parseBuffer(buffer, dataSize);
	}
	
	void parseBuffer(byte buffer[], int dataSize) throws Exception {
		ByteArrayInputStream is = new ByteArrayInputStream(buffer, 0, dataSize);
		DataInputStream dis = new DataInputStream(is);

		parseHead(dis);
		parseQuestions(dis);
		dis.close();
	}
	
	void parseHead(DataInputStream dis) throws IOException {
		txnId = dis.readShort();
		flags = dis.readShort();
		numQuestions = dis.readShort();
		numRR = dis.readShort();
		numAuthRR = dis.readShort();
		numAuxRR = dis.readShort();
	}
	
	void parseQuestions(DataInputStream dis) throws Exception {
		for (int i = 0; i < numQuestions; i++) {
			DnsQuestion question = new DnsQuestion(dis);
			questions.add(question);
		}
	}
	
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("txnId:");
		strbuf.append(txnId);
		strbuf.append(";hosts:");
		strbuf.append(questions.toString());
		
		return strbuf.toString();
	}
	
	public boolean isRequest() {
		return ((flags & 0x8000) == 0)? true: false;
	}
	
	public int getOpcode() {
		return (flags >> 7) & 0x0F;
	}
	
	public boolean allowRecursie() {
		return ((flags & 0x100) == 0)? false: true;
	}
	
	public short getTxnId() {
		return txnId;
	}

	public void setTxnId(short txnId) {
		this.txnId = txnId;
	}

	public short getFlags() {
		return flags;
	}

	public void setFlags(short flags) {
		this.flags = flags;
	}

	public short getNumQuestions() {
		return numQuestions;
	}

	public void setNumQuestions(short numQuestions) {
		this.numQuestions = numQuestions;
	}

	public short getNumRR() {
		return numRR;
	}

	public void setNumRR(short numRR) {
		this.numRR = numRR;
	}

	public short getNumAuthRR() {
		return numAuthRR;
	}

	public void setNumAuthRR(short numAuthRR) {
		this.numAuthRR = numAuthRR;
	}

	public short getNumAuxRR() {
		return numAuxRR;
	}

	public void setNumAuxRR(short numAuxRR) {
		this.numAuxRR = numAuxRR;
	}

	public ArrayList<DnsQuestion> getQuestions() {
		return questions;
	}

	public static byte[] constructPacket(int txnId, int flags, String host) throws Exception {
		// calculate length of the request
		int headLen = 12;
		String hostParts[] = host.split("\\.");
		int questionLen = 5;
		for (String s: hostParts) {
			questionLen += s.length();
			questionLen += 1;
		}
		
		ByteArrayOutputStream os = new ByteArrayOutputStream(headLen + questionLen);
		DataOutputStream dos = new DataOutputStream(os);
		// push head
		dos.writeShort(txnId);
		dos.writeShort(flags);
		dos.writeShort(1);
		dos.writeShort(0);
		dos.writeShort(0);
		dos.writeShort(0);
		// push name
		for (String s: hostParts) {
			// length
			dos.writeByte(s.length());
			for (byte b: s.getBytes()) {
				dos.writeByte(b);
			}
		}
		dos.writeByte(0);
		// push query type
		dos.writeShort(Utils.QUERY_TYPE_A);
		dos.writeShort(1);	// IP class
		dos.close();
		return os.toByteArray();
	}
	// members
	short txnId = 0;
	short flags = 0x100;
	short numQuestions = 0;
	short numRR = 0;
	short numAuthRR = 0;
	short numAuxRR = 0;
	ArrayList<DnsQuestion> questions = new ArrayList<DnsQuestion>();
}
