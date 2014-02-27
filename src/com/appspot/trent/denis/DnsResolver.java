package com.appspot.trent.denis;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Random;

public class DnsResolver extends Resolver {

	public DnsResolver(Inet4Address serverAddr, int port) throws Exception {
		dnsServerAddr = serverAddr;
		socket = new DatagramSocket();
		serverPort = port;
	}
	
	public void finalize() {
		socket.close();
	}
	@Override
	public HostRecord addressForHost(String domainName) throws Exception {
		// NOTE: this function may block!
		Random rand = new Random();
		byte buffer[] = DnsRequest.constructPacket(rand.nextInt(), 0x100, domainName);
		DatagramPacket p = new DatagramPacket(buffer, buffer.length);
		p.setAddress(dnsServerAddr);
		p.setPort(serverPort);
		socket.send(p);
		// receive
		byte recvBuffer[] = new byte[1024];
		p = new DatagramPacket(recvBuffer, recvBuffer.length);
		socket.receive(p);
		DnsResponse response = new DnsResponse(recvBuffer, p.getLength());
		ArrayList<DnsAnswer> answers = response.getAnswers();
		HostRecord result = new HostRecord(domainName);
		for (int i = 0; i < answers.size(); i++) {
			DnsAnswer answer = answers.get(i);
			// we only support query type A
			if (answer.getQueryType() == Utils.QUERY_TYPE_A)
				result.addIpAddress(answer.getIpAddress());
		}
		return result;
	}
	
	DatagramSocket socket;
	Inet4Address dnsServerAddr;
	int serverPort;
}
