package unimelb.bitbox;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain extends Thread implements FileSystemObserver  {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	private int port;
	List<String> peers;
	List<String> connectedPeers;
	List<String> commingPeers;
	List<Socket> Sockets;
	List<String> extraPeers;

	Map<String,Socket> map;
	Queue<String> peerQue;
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		this.fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		this.peers= new ArrayList<String>();
		this.connectedPeers= new ArrayList<String>();
		this.Sockets= new ArrayList<Socket>();
		this.map= new HashMap<String,Socket>();
		this.commingPeers=new ArrayList<String>();
		this.peerQue= new LinkedList<String>();
		this.extraPeers = new ArrayList<>();
	}
	public synchronized void addComPeer(String peer){this.commingPeers.add(peer);}
	public synchronized void delComPeer(String peer){this.commingPeers.remove(peer);}
	public synchronized void addMap(String peer, Socket socket){
		this.map.put(peer,socket);
	}
	public synchronized void addSockets(Socket socket){
		this.Sockets.add(socket);
	}
	public synchronized void delSockets(Socket socket){
		this.Sockets.remove(socket);
	}
	public synchronized Socket getMapvalue(String peer){
		return this.map.get(peer);
	}
	public synchronized void removeMapKV(String peer){
		this.map.remove(peer);
	}
	public synchronized void addMapKV(String peer,Socket socket){
		this.map.put(peer,socket);
	}
	public synchronized int getLimitNumber(){
		return this.commingPeers.size();
	}
	public synchronized void addPeerQue(String peer){
		this.peerQue.offer(peer);
	}
	public void addPeers(String peer){
		this.peers.add(peer);
	}
	public boolean containsComPeer(String peer){
		return this.commingPeers.contains(peer);
	}
	public void delPeer(String peer){
		peers.remove(peer);
	}
	public synchronized void addconnectedPeers(String peer){
		this.connectedPeers.add(peer);
	}
	public synchronized boolean containsconnectedPeer(String peer){
		return this.connectedPeers.contains(peer);
	}
	public synchronized void delconnectedPeers(String peer){
		connectedPeers.remove(peer);
	}



	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		log.info("Got event: " + fileSystemEvent.event);
		if (fileSystemEvent.event == FileSystemManager.EVENT.FILE_CREATE) {
			log.info("Processing File create event...");
			log.info("Available sockets: " + Sockets.size());
			for (Socket socket : Sockets) {
				try {
					if (socket.isClosed()) {
						continue;
					}
					Protocols.createAndSendFileCreateRequest(socket, fileSystemEvent);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.info(e.getMessage());
				}
			}
		}

		if (fileSystemEvent.event == FileSystemManager.EVENT.FILE_DELETE) {
			log.info("Processing File Delete event...");
			for (Socket socket : Sockets) {
				try {
					if (socket.isClosed()) {
						continue;
					}
					Protocols.createAndSendFileDeleteRequest(socket, fileSystemEvent);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.info(e.getMessage());
				}
			}
		}

		if (fileSystemEvent.event == FileSystemManager.EVENT.FILE_MODIFY) {
			log.info("Processing File modify event...");
			for (Socket socket : Sockets) {
				try {
					if (socket.isClosed()) {
						continue;
					}
					Protocols.createAndSendFileModifyRequest(socket, fileSystemEvent);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.info(e.getMessage());
				}
			}
		}

		if (fileSystemEvent.event == FileSystemManager.EVENT.DIRECTORY_CREATE) {
			log.info("Processing Directory create event...");
			for (Socket socket : Sockets) {
				try {
					if (socket.isClosed()) {
						continue;
					}
					Protocols.createAndSendDirectoryCreateRequest(socket, fileSystemEvent);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.info(e.getMessage());
				}
			}
		}
		if (fileSystemEvent.event == FileSystemManager.EVENT.DIRECTORY_DELETE) {
			log.info("Processing Directory delete event...");
			for (Socket socket : Sockets) {
				try {
					if (socket.isClosed()) {
						continue;
					}
					Protocols.createAndSendDirectoryDeleteRequest(socket, fileSystemEvent);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.info(e.getMessage());
				}
			}
		}

	}

}

