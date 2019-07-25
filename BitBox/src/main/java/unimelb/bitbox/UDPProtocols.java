package unimelb.bitbox;//

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.logging.Logger;

public class UDPProtocols {
    private static Logger log = Logger.getLogger(UDPProtocols.class.getName());
    public  static void sendMessage(DatagramSocket socket, String msg, InetAddress address, int port){
        byte[] sendBytes = msg.getBytes();
        log.info(String.valueOf(sendBytes.length));
        log.info(msg);
        DatagramPacket packet = new DatagramPacket(sendBytes,sendBytes.length,address,port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public  static String getMessage(DatagramSocket socket){
        byte[] receiveBytes = new byte[65535];
        DatagramPacket packet = new DatagramPacket(receiveBytes,receiveBytes.length);
        try {
            socket.receive(packet);
            String msg = new String(packet.getData(),0,packet.getLength());

            return msg;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static void createAndSendAck(DatagramSocket socket, InetAddress address, int Port) throws IOException {
        // TODO Auto-generated method stub
        JSONObject handshakeResponse = new JSONObject();
        JSONObject hostPort = new JSONObject();
        int port = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
        String host = InetAddress.getLocalHost().getHostAddress();
        hostPort.put("host", host);
        hostPort.put("port", port);
        handshakeResponse.put("hostPort", hostPort);
        handshakeResponse.put("command", "HANDSHAKE_RESPONSE");
        String msg = handshakeResponse.toJSONString() +"\n";
        log.info(msg);
        sendMessage(socket,msg,address,Port);
    }

    protected static void createAndSendRFS(DatagramSocket socket, InetAddress address, int port, List<String> connectedPeers) throws IOException {
        // TODO Auto-generated method stub
        JSONObject handshakeRefuse = new JSONObject();
        handshakeRefuse.put("command", "CONNECTION_REFUSED");
        handshakeRefuse.put("message", "connection limit reached");
        JSONArray peers = new JSONArray();

        for (String entry : connectedPeers) {
            JSONObject peer = new JSONObject();
            peer.put("host", entry.substring(0, entry.indexOf(":")));
            peer.put("port", entry.substring(entry.indexOf(":") + 1));
            peers.add(peer);
        }
        handshakeRefuse.put("peers", peers);
        String msg = handshakeRefuse.toJSONString()+"\n";
        sendMessage(socket,msg,address,port);
    }
    public static String createHandshakeRequest() throws UnknownHostException {
        JSONObject handshakeRequest = new JSONObject();
        handshakeRequest.put("command", "HANDSHAKE_REQUEST");
        JSONObject hostPort = new JSONObject();
        //log.info(InetAddress.getLocalHost().getHostAddress());
        hostPort.put("host", InetAddress.getLocalHost().getHostAddress());
        hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("udpPort")));
        handshakeRequest.put("hostPort", hostPort);
        //log.info(handshakeRequest.toJSONString());
        return handshakeRequest.toJSONString() + "\n";

    }

    public static void sendHandshake(DatagramSocket socket, InetAddress address, int port) throws IOException {
        // TODO Auto-generated method stub
        log.info("Sending Handshake Request...");

        // open socket's output stream and send handshake request
        sendMessage(socket,createHandshakeRequest(),address,port);

        // wait for the response - and see if its nack or ack.

    }
    public static void createAndSendFileCreateRequest(DatagramSocket socket, FileSystemManager.FileSystemEvent event, InetAddress address, int port,List<String> Message) throws IOException {
        // TODO Auto-generated method stub
        JSONObject fileCreateRequest = new JSONObject();
        fileCreateRequest.put("command", "FILE_CREATE_REQUEST");
        JSONObject fileDescriptor = new JSONObject();
        fileDescriptor.put("md5", event.fileDescriptor.md5);
        fileDescriptor.put("lastModified", event.fileDescriptor.lastModified);
        fileDescriptor.put("fileSize", event.fileDescriptor.fileSize);
        fileCreateRequest.put("fileDescriptor", fileDescriptor);
        fileCreateRequest.put("pathName", event.pathName);
        String msg = fileCreateRequest.toJSONString() + "\n";
        log.info("File create "+msg);
        sendMessage(socket,msg,address,port);
        String ip = address.getHostAddress();
        String request = msg+"+"+ip+"+"+port;
        Message.add(request);
    }

    public static void createAndSendFileDeleteRequest(DatagramSocket socket, FileSystemManager.FileSystemEvent fileSystemEvent, InetAddress address, int port,List<String> Message)
            throws IOException {
        // TODO Auto-generated method stub
        JSONObject fileDeleteRequest = new JSONObject();
        fileDeleteRequest.put("command", "FILE_DELETE_REQUEST");
        JSONObject fileDescriptor = new JSONObject();
        fileDescriptor.put("md5", fileSystemEvent.fileDescriptor.md5);
        fileDescriptor.put("lastModified", fileSystemEvent.fileDescriptor.lastModified);
        fileDescriptor.put("fileSize", fileSystemEvent.fileDescriptor.fileSize);
        fileDeleteRequest.put("fileDescriptor", fileDescriptor);
        fileDeleteRequest.put("pathName", fileSystemEvent.pathName);
        String msg = fileDeleteRequest.toJSONString() + "\n";
        sendMessage(socket,msg,address,port);
        String ip = address.getHostAddress();
        String request = msg+"+"+ip+"+"+port;
        Message.add(request);
    }

    public static void createAndSendFileModifyRequest(DatagramSocket socket, FileSystemManager.FileSystemEvent fileSystemEvent, InetAddress address, int port,List<String> Message)
            throws IOException {
        // TODO Auto-generated method stub
        JSONObject fileModifyRequest = new JSONObject();
        fileModifyRequest.put("command", "FILE_MODIFY_REQUEST");
        JSONObject fileDescriptor = new JSONObject();
        fileDescriptor.put("md5", fileSystemEvent.fileDescriptor.md5);
        fileDescriptor.put("lastModified", fileSystemEvent.fileDescriptor.lastModified);
        fileDescriptor.put("fileSize", fileSystemEvent.fileDescriptor.fileSize);
        fileModifyRequest.put("fileDescriptor", fileDescriptor);
        fileModifyRequest.put("pathName", fileSystemEvent.pathName);
        log.info("fffffff: "+fileModifyRequest.toJSONString());
        String msg = fileModifyRequest.toJSONString() + "\n";
        sendMessage(socket,msg,address,port);
        String ip = address.getHostAddress();
        String request = msg+"+"+ip+"+"+port;
        Message.add(request);
    }

    public static void createAndSendDirectoryCreateRequest(DatagramSocket socket, FileSystemManager.FileSystemEvent fileSystemEvent,InetAddress address, int port,List<String> Message)
            throws IOException {
        // TODO Auto-generated method stub
        JSONObject dirCreateRequest = new JSONObject();
        dirCreateRequest.put("command", "DIRECTORY_CREATE_REQUEST");
        dirCreateRequest.put("pathName", fileSystemEvent.pathName);
        String msg = dirCreateRequest.toJSONString() + "\n";
        //log.info("1");
        sendMessage(socket,msg,address,port);
        String ip = address.getHostAddress();
        String request = msg+"+"+ip+"+"+port;
        log.info(request);
        Message.add(request);

    }

    public static void createAndSendDirectoryDeleteRequest(DatagramSocket socket, FileSystemManager.FileSystemEvent fileSystemEvent,InetAddress address, int port,List<String> Message)
            throws IOException {
        // TODO Auto-generated method stub
        JSONObject dirDeleteRequest = new JSONObject();
        dirDeleteRequest.put("command", "DIRECTORY_DELETE_REQUEST");
        dirDeleteRequest.put("pathName", fileSystemEvent.pathName);
        String msg = dirDeleteRequest.toJSONString() + "\n";
        sendMessage(socket,msg,address,port);
        String ip = address.getHostAddress();
        String request = msg+"+"+ip+"+"+port;
        Message.add(request);
    }

    public static void createAndSendInvalidProtocol(String message, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // TODO Auto-generated method stub
        JSONObject invalidProtocol = new JSONObject();
        invalidProtocol.put("command", "INVALID_PROTOCOL");
        invalidProtocol.put("message", message);
        String msg = invalidProtocol.toJSONString() + "\n";
        sendMessage(socket,msg,address,port);
    }
}
