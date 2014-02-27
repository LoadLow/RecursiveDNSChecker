package com.appspot.trent.denis;

public abstract class Resolver {
	public Resolver() {
		
	}
	
	public abstract HostRecord addressForHost(String domainName) throws Exception;
}
