package com.appspot.trent.denis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;

public class DnsResponse {
	private short txnId;
	private short flags;
	private short numQuestions;
	private short numRR;
	private short numAuthRR;
	private short numAuxRR;
	ArrayList<DnsQuestion> questions = new ArrayList<DnsQuestion>();
	ArrayList<DnsAnswer> answers = new ArrayList<DnsAnswer>();

	public DnsResponse(byte buffer[], int dataSize) throws Exception {
		ByteArrayInputStream is = new ByteArrayInputStream(buffer, 0, dataSize);
		DataInputStream dis = new DataInputStream(is);
		
		parseHead(dis);
		parseQuestions(dis);
		parseAnswers(dis);
		dis.close();
	}
	
	public void parseHead(DataInputStream dis) throws Exception {
		txnId = dis.readShort();
		flags = dis.readShort();
		numQuestions = dis.readShort();
		numRR = dis.readShort();
		numAuthRR = dis.readShort();
		numAuxRR = dis.readShort();
	}
	
	public void parseQuestions(DataInputStream dis) throws Exception {
		for (int i = 0; i < numQuestions; i++) {
			DnsQuestion question = new DnsQuestion(dis);
			questions.add(question);
		}
	}
	
	public void parseAnswers(DataInputStream dis) throws Exception {
		for (int i = 0; i < numRR; i++) {
			DnsAnswer answer = new DnsAnswer(dis);
			answers.add(answer);
		}
	}
	
	public short getTxnId() {
		return txnId;
	}

	public short getFlags() {
		return flags;
	}

	public short getNumQuestions() {
		return numQuestions;
	}

	public short getNumRR() {
		return numRR;
	}

	public short getNumAuthRR() {
		return numAuthRR;
	}

	public short getNumAuxRR() {
		return numAuxRR;
	}

	public ArrayList<DnsQuestion> getQuestions() {
		return questions;
	}

	public ArrayList<DnsAnswer> getAnswers() {
		return answers;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(questions.toString());
		sb.append(answers.toString());
		return sb.toString();
	}
	
	public static byte[] constructPacket(HostRecord hostRecord, int txnId) throws Exception {
		// calculate packet length
		String host = hostRecord.getDomainName();
		ArrayList<IPAddress> ipAddresses = hostRecord.getIpAddresses();
		int headLen = 12;
		String hostParts[] = host.split("\\.");
		int questionLen = 5;
		for (String s: hostParts) {
			questionLen += s.length();
			questionLen += 1;
		}
		int answerLen = 0;
		answerLen += 16 * ipAddresses.size();
			// 2 bytes domain name pointer
			// 8 bytes type, class, ttl
			// 6 bytes ip address
		int totalLen = headLen + questionLen + answerLen;
		// push stuff in
		ByteArrayOutputStream bos = new ByteArrayOutputStream(totalLen);
		DataOutputStream dos = new DataOutputStream(bos);
		
		// head
		dos.writeShort(txnId);		// transaction id
		short flags = (short) 0x8180;
		if (hostRecord.getIpAddresses().size() == 0)
			flags |= 0x03;			// name error
		dos.writeShort(flags);		// flags
		dos.writeShort(1);			// number of questions
		dos.writeShort(ipAddresses.size());
		dos.writeShort(0);			// authority RRs
		dos.writeShort(0);			// additional RRs
		// questions
		for (String s: hostParts) {
			dos.writeByte(s.length());
			for (char c: s.toCharArray()) {
				dos.writeByte((byte)c);
			}
		}
		dos.writeByte(0);		// ending 0
		dos.writeShort(Utils.QUERY_TYPE_A);	// query type
		dos.writeShort(1);		// query class
		
		// answers
		for (IPAddress address: ipAddresses) {
			dos.writeByte(0xC0);	// the domain name is a pointer
			dos.writeByte(0x0C);	// the pointer location
			dos.writeShort(Utils.QUERY_TYPE_A);	// query type
			dos.writeShort(1);		// query class
			dos.writeInt(360);		// TTL = 6 minutes
			dos.writeShort(4);		// RR data length
			dos.write(address.getBuffer(), 0, 4);	// IP address
		}
		
		dos.close();
		return bos.toByteArray();
	}
}
