package unimelb.bitbox;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class UDPServerMain extends Thread implements FileSystemObserver  {
    private static Logger log = Logger.getLogger(ServerMain.class.getName());
    protected FileSystemManager fileSystemManager;
    private int port;
    List<String> peers;
    List<String> connectedPeers;
    List<String> commingPeers;
    List<Socket> Sockets;
    Map<String,Socket> map;
    Queue<String> peerQue;
    List<String> extraPeers;
    String mode;
    DatagramSocket socket;
    List<String> Message;
    public UDPServerMain(DatagramSocket socket) throws NumberFormatException, IOException, NoSuchAlgorithmException {
        this.fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
        this.peers= new ArrayList<String>();
        this.connectedPeers= new ArrayList<String>();
        this.Sockets= new ArrayList<Socket>();
        this.map= new HashMap<String,Socket>();
        this.commingPeers=new ArrayList<String>();
        this.peerQue= new LinkedList<String>();
        this.Message= new ArrayList<String>();
        this.mode= Configuration.getConfigurationValue("Mode");
        this.socket = socket;
        this.extraPeers = new ArrayList<>();
    }
    public synchronized int peerQsize(){return this.peerQue.size();}
    public synchronized void addPeerQue(String peer){
        this.peerQue.offer(peer);
    }
    public synchronized List<String> CopyMessages(){
        List<String> copyMessage = new ArrayList<String>(this.Message);
        return copyMessage;
    }
    public synchronized List<String> CopyExtraPeers(){
        List<String> CopyExtraPeers = new ArrayList<String>(this.extraPeers);
        return CopyExtraPeers;
    }
    public synchronized List<String> CopyPeers(){
        List<String> copyPeer = new ArrayList<String>(this.peers);
        return copyPeer;
    }
    public synchronized int Messagesize(){return this.Message.size();}
    public synchronized boolean containsMessage(String msg){return this.Message.contains(msg);}
    public synchronized void addMessage(String msg){this.Message.add(msg);}
    public synchronized void delMessage(String msg){this.Message.remove(msg);}
    public synchronized boolean containsPeer(String peer){return this.peers.contains(peer);}
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
    public synchronized void addPeers(String peer){
        this.peers.add(peer);
    }
    public boolean containsComPeer(String peer){
        return this.commingPeers.contains(peer);
    }
    public void delPeers(String peer){
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
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent){
        log.info("Got event: " + fileSystemEvent.event);
        if (fileSystemEvent.event == FileSystemManager.EVENT.FILE_CREATE) {
            log.info("Processing File create event...");
            log.info("Available Peers: " + connectedPeers.size());
            for (String peer : connectedPeers) {
                try {
                    String remoteHost = peer.substring(0, peer.indexOf(":"));
                    InetAddress address = InetAddress.getByName(remoteHost);
                    int remotePort = Integer.parseInt(peer.substring(peer.indexOf(":") + 1));
                    UDPProtocols.createAndSendFileCreateRequest(socket, fileSystemEvent,address,remotePort,Message);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    log.info(e.getMessage());
                }
            }
        }

        if (fileSystemEvent.event == FileSystemManager.EVENT.FILE_DELETE) {
            log.info("Processing File Delete event...");
            for (String peer : connectedPeers) {
                try {
                    String remoteHost = peer.substring(0, peer.indexOf(":"));
                    InetAddress address = InetAddress.getByName(remoteHost);
                    int remotePort = Integer.parseInt(peer.substring(peer.indexOf(":") + 1));
                    UDPProtocols.createAndSendFileDeleteRequest(socket, fileSystemEvent,address,remotePort,Message);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    log.info(e.getMessage());
                }
            }
        }

        if (fileSystemEvent.event == FileSystemManager.EVENT.FILE_MODIFY) {
            log.info("Processing File modify event...");
            for (String peer : connectedPeers) {
                try {
                    String remoteHost = peer.substring(0, peer.indexOf(":"));
                    InetAddress address = InetAddress.getByName(remoteHost);
                    int remotePort = Integer.parseInt(peer.substring(peer.indexOf(":") + 1));
                    UDPProtocols.createAndSendFileModifyRequest(socket, fileSystemEvent,address,remotePort,Message);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    log.info(e.getMessage());
                }
            }
        }

        if (fileSystemEvent.event == FileSystemManager.EVENT.DIRECTORY_CREATE) {
            log.info("Processing Directory create event...");
            for (String peer : connectedPeers) {
                try {
                    String remoteHost = peer.substring(0, peer.indexOf(":"));
                    InetAddress address = InetAddress.getByName(remoteHost);
                    int remotePort = Integer.parseInt(peer.substring(peer.indexOf(":") + 1));
                    UDPProtocols.createAndSendDirectoryCreateRequest(socket, fileSystemEvent,address,remotePort,Message);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    log.info(e.getMessage());
                }
            }
        }
        if (fileSystemEvent.event == FileSystemManager.EVENT.DIRECTORY_DELETE) {
            log.info("Processing Directory delete event...");
            for (String peer : connectedPeers) {
                try {
                    String remoteHost = peer.substring(0, peer.indexOf(":"));
                    InetAddress address = InetAddress.getByName(remoteHost);
                    int remotePort = Integer.parseInt(peer.substring(peer.indexOf(":") + 1));
                    UDPProtocols.createAndSendDirectoryDeleteRequest(socket, fileSystemEvent,address,remotePort,Message);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    log.info(e.getMessage());
                }
            }
        }

    }
}



