package org.torproject.jtor.socks.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.torproject.jtor.Logger;
import org.torproject.jtor.TorException;
import org.torproject.jtor.circuits.impl.StreamManagerImpl;
import org.torproject.jtor.socks.SocksPortListener;

public class SocksPortListenerImpl implements SocksPortListener {

	private final Set<Integer> listeningPorts = new HashSet<Integer>();
	private final Map<Integer, Thread> acceptThreads = new HashMap<Integer, Thread>();
	private final Logger logger;
	private final StreamManagerImpl streamManager;
	
	public SocksPortListenerImpl(Logger logger, StreamManagerImpl streamManager) {
		this.logger = logger;
		this.streamManager = streamManager;
	}

	public void addListeningPort(int port) {
		if(port <= 0 || port > 65535)
			throw new TorException("Illegal listening port: "+ port);
		
		synchronized(listeningPorts) {
			if(listeningPorts.contains(port))
				return;
			listeningPorts.add(port);
			try {
				startListening(port);
				logger.debug("Listening for SOCKS connections on port "+ port);
			} catch (IOException e) {
				listeningPorts.remove(port);
				throw new TorException("Failed to listen on port "+ port +" : "+ e.getMessage());
			}
		}
		
	}
	
	private void startListening(int port) throws IOException {
		final ServerSocket ss = new ServerSocket(port);
		final Thread listeningThread = createAcceptThread(ss, port);
		acceptThreads.put(port, listeningThread);
		listeningThread.start();
	}
	
	private Thread createAcceptThread(final ServerSocket ss, final int port) {
		return new Thread(new Runnable() { public void run() {
			try {
				runAcceptLoop(ss);
			} catch (IOException e) {
				logger.error("System error accepting SOCKS socket connections: "+ e.getMessage());
				synchronized(listeningPorts) {
					listeningPorts.remove(port);
					acceptThreads.remove(port);
				}
			}				
		}});
	}
	
	private void runAcceptLoop(ServerSocket ss) throws IOException {
		while(true) {
			newClientSocket(ss.accept());
		}
	}
	
	private void newClientSocket(final Socket s) {
		final Thread t = new Thread(new Runnable() { public void run() {
			try {
				SocksClientSocket.runClient(s, logger, streamManager);
			} catch(IOException e) {
				logger.warn("System error processing client connection: "+ e.getMessage());
			} finally {
				try {
					s.close();
				} catch (IOException e) {}
			}
		}});
		t.start();
	}
}
