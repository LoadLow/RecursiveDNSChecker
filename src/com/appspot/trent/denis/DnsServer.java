package com.appspot.trent.denis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

public class DnsServer extends Thread {
	public DnsServer(int udpPort) throws Exception {
		// create master resolver
		byte serverAddr[] = new byte[4];
		// FIXME: default dns server address. You could change it if you want
		serverAddr[0] = (byte) 192;
		serverAddr[1] = (byte) 168;
		serverAddr[2] = (byte) 200;
		serverAddr[3] = 10;
		dnsServerAddr = (Inet4Address) Inet4Address.getByAddress(serverAddr);
		badAddress = new IPAddress(new byte[]{(byte)208,67,(byte)219,(byte)130});
		masterResolver = new DnsResolver(dnsServerAddr, 53);
		hackResolver = new HttpResolver();
		listenSocket = new DatagramSocket(udpPort);
		initShellCommands();
	}
	
	private abstract class CmdHandler {
		CmdHandler(String helpMsg) {
			this.helpMsg = helpMsg;
		}
		abstract void handleCmd(String args[]) throws Exception;
		
		String helpMsg;
	}
	
	public void run() {
		// we don't expect this program to run on heavy load
		// everything runs in ONE thread!
		byte buffer[] = new byte[1024];

		while (true) {
			try {
				DatagramPacket p = new DatagramPacket(buffer, buffer.length);
				listenSocket.receive(p);
				if (p.getLength() == 0) {
					System.out.println("Exiting service thread");
					break;
				}
				DnsRequest request = new DnsRequest(buffer, p.getLength());
				int txnId = request.getTxnId();

				ArrayList<DnsQuestion> questions = request.getQuestions();
				synchronized(hostCache) {
					for (DnsQuestion question: questions) {
						String host = question.getDomainName();
						HostRecord record = resolve(host);
						// send back response, record may be null.
						byte reply[] = DnsResponse.constructPacket(record, txnId);
						DatagramPacket replyPkt = new DatagramPacket(reply, reply.length);
						replyPkt.setAddress(p.getAddress());
						replyPkt.setPort(p.getPort());
						listenSocket.send(replyPkt);

						//System.out.println("Served " + host + ": " + record);
					}
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public HostRecord resolve(String domainName) throws Exception {
		synchronized (hostCache) {
			// scan cache
			HostRecord record = hostCache.findHost(domainName);
			if (record == null) {
				// ask master resolver
				record = masterResolver.addressForHost(domainName);
			}

			// XXX: do hacking!
			record = hackRecord(record);
			// add to cache
			hostCache.addHost(record);
			return record;
		}
	}
	
	/**
	 * Hack the record if it was originally filtered by opendns
	 * 
	 * @param record
	 * @return
	 * @throws Exception 
	 */
	public HostRecord hackRecord(HostRecord record) throws Exception {		
		for (IPAddress address: record.getIpAddresses()) {
			if (address.equals(badAddress)) {
				//System.out.println("Hacking "+record.getDomainName());
				record = hackResolver.addressForHost(record.getDomainName());
			} else {
				//System.out.println("Addr " + address + " != " + filteredAddr);
			}
		}
		return record;
	}
	
	public void stopThread() throws Exception {
		// send an empty packet to listenSocket
		Inet4Address listenAddress = (Inet4Address) Inet4Address.getByAddress(new byte[]{127, 0, 0, 1});
		int listenPort = listenSocket.getLocalPort();
		byte data[] = new byte[0];
		DatagramPacket p = new DatagramPacket(data, 0, listenAddress, listenPort);
		listenSocket.send(p);
		join();
	}
	
	public void saveCfg(String fileName) throws Exception {
		// save config
		synchronized(hostCache) {
			Properties p = new Properties();
			for (HostRecord r: hostCache.getItems()) {
				p.setProperty(r.getDomainName(), r.getIpAddresses().toString());
			}
			if (fileName.equals("stdout")) {
				p.store(System.out, "Denis Cache");
			} else {
				p.store(new FileOutputStream(fileName), "Denis Cache");
			}
		}
	}
	
	public void loadCfg(String fileName) throws Exception {
		// load config
		synchronized(hostCache) {
			Properties p = new Properties();
			p.load(new FileInputStream(fileName));
			for (Object k: p.keySet()) {
				String key = (String)k;
				String value = p.getProperty(key);
				// build host record from value
				HostRecord record = new HostRecord(key);
				String ips[] = value.substring(1, value.length()-1).split(", ");
				for (String ip: ips) {
					String s[] = ip.split("\\.");
					if (s.length == 4) {
						byte parts[] = new byte[4];
						for (int i = 0; i < s.length; i++)
							parts[i] = (byte) Integer.parseInt(s[i]);
						record.addIpAddress(new IPAddress(parts));
					}
				}
				hostCache.addHost(record);
			}
		}
	}
	
	/**
	 * Print the cache on screen, ignoring empty items
	 */
	@SuppressWarnings("unchecked")
	public void showCache() {
		ArrayList<HostRecord> validRecords = new ArrayList<HostRecord>();
		synchronized(hostCache) {
			for (HostRecord hostRecord: hostCache.getItems()) {
				if (hostRecord.getIpAddresses().size() > 0) {
					validRecords.add(hostRecord);
				}
			}
		}
		Collections.sort(validRecords);
		for (HostRecord hostRecord: validRecords) {
			//System.out.println(hostRecord.getDomainName() + " " + hostRecord.getIpAddresses());
			for (IPAddress address: hostRecord.getIpAddresses()) {
				System.out.println(address.toString() + " " + hostRecord.getDomainName());
			}
		}
	}
	
	public void showShellHelp() {
		// show shell help
		for (String key: cmdTable.keySet()) {
			CmdHandler handler = cmdTable.get(key);
			System.out.println(handler.helpMsg);
		}
	}
	
	public void exitShell() {
		// exit shell
		exitShell = true;
		try {
			stopThread();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void initShellCommands() {
		cmdTable.put("save", new CmdHandler("save fileName -- save cache to fileName") {
			@Override
			void handleCmd(String[] args) throws Exception {
				if (args.length == 2) {
					String fileName = args[1];
					saveCfg(fileName);
				} else {
					showShellHelp();
				}
			}
		});
		
		cmdTable.put("load", new CmdHandler("load filename -- load cache from fileName") {
			@Override
			void handleCmd(String[] args) throws Exception {
				if (args.length == 2) {
					String fileName = args[1];
					loadCfg(fileName);
				} else {
					showShellHelp();
				}
			}
		});
		
		cmdTable.put("help", new CmdHandler("help -- show help message") {
			@Override
			void handleCmd(String[] args) throws Exception {
				showShellHelp();
			}
		});
		

		cmdTable.put("exit", new CmdHandler("exit -- exit shell") {
			@Override
			void handleCmd(String[] args) throws Exception {
				exitShell();
			}
		});
		
		cmdTable.put("cache", new CmdHandler("cache -- show cache items") {
			@Override
			void handleCmd(String[] args) throws Exception {
				showCache();
			}
		});
		
		cmdTable.put("resolve", new CmdHandler("resolve domainName -- resolve host") {
			@Override
			void handleCmd(String[] args) throws Exception {
				if (args.length == 2) {
					HostRecord hostRecord = resolve(args[1]);
					System.out.println(hostRecord);
				} else {
					showShellHelp();
				}
			}
		});
	}
	/**
	 * Start a shell
	 */
	public void runShell() {
		exitShell = false;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try {
			while (!exitShell) {
				System.out.print(">> ");
				String line = br.readLine();
				String args[] = line.split(" ");
				if (args.length < 1) {
					showShellHelp();
				} else {
					CmdHandler handler = cmdTable.get(args[0]);
					if (handler == null) {
						showShellHelp();
					} else {
						// run command
						try {
							handler.handleCmd(args);
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		} catch (Exception ex) {
			exitShell = true;
			ex.printStackTrace();
		}
	}
	
	HostCache	hostCache = new HostCache();
	Resolver	masterResolver;
	Resolver	hackResolver;
	Inet4Address	dnsServerAddr;
	IPAddress	badAddress;		// FIXME: if the returned address is badAddress, we hack it
	DatagramSocket listenSocket;
	HashMap<String, CmdHandler> cmdTable = new HashMap<String, CmdHandler>();
	boolean		exitShell;
}
