package com.appspot.trent.denis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpResolver extends Resolver {

	public HttpResolver() {
		
	}
	@Override
	public HostRecord addressForHost(String domainName) throws Exception {
		String fullUrl = requestUrlStub + domainName;
		URL url = new URL(fullUrl);
		
		HttpURLConnection connection = null;
        //set up out communications stuff
        connection = null;
      
        //Set up the initial connection
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setReadTimeout(10000);
                  
        connection.connect();
      
        //get the output stream writer and write the output to the server
        //not needed in this example
        
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        
        String inputLine;
        HostRecord result = new HostRecord(domainName);
        byte parts[] = new byte[4];
        // parse text
        while ((inputLine = in.readLine()) != null) {
            String pat1 = "<span class='orange'>";
            String pat2 = "</span>";
            int index1 = inputLine.indexOf(pat1);
            int index2 = inputLine.indexOf(pat2);
            
            if ((index1 > 0) && (index2 > 0)) {
            	String ipStr = inputLine.substring(index1 + pat1.length(), index2);
            	//System.out.println("IP str: " + ipStr);
            	String[] s = ipStr.split("\\.");
            	for (int i = 0; i < s.length; i++)
            		parts[i] = (byte) Integer.parseInt(s[i]);
            }
        }
        IPAddress ipAddress = new IPAddress(parts); 
        result.addIpAddress(ipAddress);
        in.close();

		return result;
	}
	
	String requestUrlStub = "http://www.ip.cn/getip.php?action=queryip&ip_url=";
}
