package unimelb.bitbox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import javax.print.Doc;
import javax.swing.event.DocumentEvent;
import javax.xml.crypto.dom.DOMCryptoContext;
import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

//if the manager ask for connection this thread start working

public class ServerForManager implements Runnable {
    private static Logger log= Logger.getLogger(serverThread.class.getName());
    private ServerSocket serverSocket = null;
    private ServerMain serverMain;
    private int TCP_port;
    private UDPServerMain udpServerMain;
    private DatagramSocket udpSocket = null;


    public ServerForManager(int port,ServerMain serverMain,UDPServerMain udpServernMain){
        this.TCP_port=port;
        this.serverMain=serverMain;
        this.udpServerMain=udpServernMain;
        try {
            this.serverSocket=new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true){
            try {
                String SecretKey = Protocols.GenerateKey();
                //log.info("Listening for manager client connection request...");
                Socket clientSocket = this.serverSocket.accept();
                log.info("Connection received.");
                String msg = Protocols.getMessage(clientSocket);
                if (msg.contains("AUTH_REQUEST")) {
                    log.info("SessionKey of this session is generated :" + SecretKey);
                    Document request = Document.parse(msg);
                    String identity = request.getString("identity");
                    log.info("The identity of manager client is " + identity);
                    boolean isValid = Protocols.checkClientValidation(identity);
                    byte[] key = new byte[256];
                    if (isValid) {
                        //produce the encrypted key by RSA
                        key = Protocols.EncodeSecretKey(identity, SecretKey);
                        System.out.println("lenght of key after encrypted " + key.length);
                    }
                    Protocols.AuthResponse(isValid, key, clientSocket);
                    //进入thread
                    TCPmanagerHandler tcpManagerHandler = new TCPmanagerHandler(clientSocket, serverMain, SecretKey, udpServerMain);
                    new Thread(tcpManagerHandler).start();


                    /*BufferedReader br = null;
                    log.info("Start listening the manager request!");
                    try {
                        br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    } catch (IOException e) {
                        log.info(e.getMessage());
                    }
                    String req = null;
                    do {
                        try{
                            req = br.readLine();
                            log.info("Received Message: " + req);
                            String msg_ = handleReceivedReq(req,SecretKey.getBytes());
                            Document message = Document.parse(msg_);
                            String command = message.getString("command");
                            if (Configuration.getConfigurationValue("mode").equals("TCP")){
                                log.info("The peer run on the TCP mode");
                                if (command.contains("REQUEST")) {
                                    switch (command) {
                                        case "LIST_PEERS_REQUEST":
                                            try {
                                                SendPeerListResponse(clientSocket, SecretKey, serverMain.connectedPeers);
                                            } catch (IOException e) {
                                                log.info("Problem in PeerListResponse: " + e.getMessage());
                                            }
                                            break;
                                        case "CONNECT_PEER_REQUEST":
                                            String hostport = message.get("host") + ":" + message.get("port");
                                            log.info("Try to connect peer:"+hostport);
                                            if (!serverMain.containsconnectedPeer(hostport)) {
                                                serverMain.addPeerQue(hostport);
                                                try {
                                                    Thread.sleep(2*1000);
                                                    if (serverMain.containsconnectedPeer(hostport)){
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, true, "connected to peer");
                                                    }
                                                    else{
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, false, "connection failed - not successfully connect");
                                                    }
                                                } catch (IOException e) {
                                                    log.info(e.getMessage());
                                                }
                                            } else {
                                                try {
                                                    SendPeerCommandResponse(clientSocket, message, SecretKey, false, "connection failed - it already exists");
                                                } catch (IOException e) {
                                                    log.info(e.getMessage());
                                                }
                                            }
                                            break;
                                        case "DISCONNECT_PEER_REQUEST":
                                            String hostport_ = message.get("host") + ":" + message.get("port");
                                            try {
                                                if (serverMain.containsconnectedPeer(hostport_)) {
                                                    if (!serverMain.getMapvalue(hostport_).isClosed()) {
                                                        serverMain.getMapvalue(hostport_).close();
                                                    }
                                                    serverMain.delSockets(serverMain.getMapvalue(hostport_));
                                                    serverMain.delconnectedPeers(hostport_);
                                                    Thread.sleep(5*1000);
                                                    if (!serverMain.containsconnectedPeer(hostport_)) {
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, true, "disconnected from peer");
                                                    } else
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, false, "Something wrong on disconnection");
                                                } else {
                                                    try {
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, false, "connection not active");
                                                    } catch (IOException e) {
                                                        log.info(e.getMessage());
                                                    }
                                                }
                                            } catch (IOException e) {
                                                log.info(e.getMessage());
                                            }
                                            break;
                                        default:
                                            log.info("Wrong request from the Client!");
                                    }
                                }
                                else{
                                    log.info("Invalid format of request message !");
                                }
                            }
                            //------------------------------------------------------------------------------------------
                            else if (Configuration.getConfigurationValue("mode").equals("UDP")){
                                log.info("The peer run on the UDP mode  "+command);
                                if (command.contains("REQUEST")) {
                                    switch (command) {
                                        case "LIST_PEERS_REQUEST":
                                            try {
                                                SendPeerListResponse(clientSocket, SecretKey, udpServerMain.connectedPeers);
                                            } catch (IOException e) {
                                                log.info("Problem in PeerListResponse: " + e.getMessage());
                                            }
                                            break;
                                        case "CONNECT_PEER_REQUEST":
                                            String hostport = message.get("host") + ":" + message.get("port");
                                            log.info("Try to connect peer:"+hostport);
                                            if (!udpServerMain.containsPeer(hostport)) {
                                                udpServerMain.addPeers(hostport);
                                                udpServerMain.addPeerQue(hostport);

                                                try {
                                                    Thread.sleep(5*1000);
                                                    if (udpServerMain.containsconnectedPeer(hostport)){
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, true, "connected to peer");
                                                    }
                                                    else{
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, false, "connection failed - not successfully connect");
                                                    }
                                                } catch (IOException e) {
                                                    log.info(e.getMessage());
                                                }
                                            } else {
                                                try {
                                                    SendPeerCommandResponse(clientSocket, message, SecretKey, false, "connection failed - it already exists");
                                                } catch (IOException e) {
                                                    log.info(e.getMessage());
                                                }
                                            }
                                            break;
                                        case "DISCONNECT_PEER_REQUEST":
                                            String hostport_ = message.get("host") + ":" + message.get("port");
                                            log.info("Try to connect peer:"+hostport_);
                                            try {
                                                log.info("Size is "+udpServerMain.connectedPeers.size());
                                                if (udpServerMain.containsconnectedPeer(hostport_)) {
                                                    udpServerMain.delconnectedPeers(hostport_);
                                                    //Thread.sleep(5*1000);
                                                    if (!udpServerMain.containsconnectedPeer(hostport_)) {
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, true, "disconnected from peer");
                                                    } else
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, false, "Something wrong on disconnection");
                                                } else {
                                                    try {
                                                        SendPeerCommandResponse(clientSocket, message, SecretKey, false, "connection not active");
                                                    } catch (IOException e) {
                                                        log.info(e.getMessage());
                                                    }
                                                }
                                            } catch (IOException e) {
                                                log.info(e.getMessage());
                                            }
                                            break;
                                        default:
                                            log.info("Wrong request from the Client!");
                                    }
                                }
                                else{
                                    log.info("Invalid format of request message !");
                                }
                            }
                            else {
                                log.info("Unexpected mode! Please set as TCP or UDP \n");
                            }
                        }catch (IOException e){
                            log.info(e.getMessage());
                        }



                    }while (req!=null);
                    */
                }
                else{
                    log.info(" Incoming client request is not a valid authorized request!");
                    clientSocket.close();
                }
            }catch (SocketException e){
                log.info(e.getMessage());
                continue;
            }
            catch (IOException e) {
                log.info(e.getMessage());
                continue;
            }
            catch (Exception e) {
                log.info(e.getMessage());
                continue;
            }
        }
    }

    private static String handleReceivedReq(String msg,byte[] SecretKey){
        String message_Str = "";
        if (msg.contains("payload")) {
            //取出payload部分->Base64解码->AES解密->Parse为一个Document（正常的明文信息）
            String msg_ = new String(Base64.getDecoder().decode(Document.parse(msg).getString("payload")));
            message_Str = Protocols.AESdecryption(msg_, SecretKey);
        }
        else{
            log.info("Message format error !");
        }
        log.info("Received message is :" + message_Str);
        return message_Str;
    }

    private static void SendPeerListResponse(Socket socket, String SecretKey, List<String> connectedPeers)throws IOException{
        JSONArray Peers = new JSONArray();
        for (String p:connectedPeers){
            JSONObject peer = new JSONObject();
            peer.put("host", p.substring(0, p.indexOf(":")));
            peer.put("port", p.substring(p.indexOf(":") + 1));
            Peers.add(peer);
        }
        //TODO:获取peerlist
        JSONObject request = new JSONObject();
        request.put("command","LIST_PEERS_RESPONSE");
        request.put("peers",Peers);
        String cipher = Protocols.AESencryption((request.toJSONString()+"\n"),SecretKey.getBytes());
        Document payload = new Document();
        payload.append("payload",Base64.getEncoder().encodeToString(cipher.getBytes()));
        Protocols.sendMessage(payload.toJson()+"\n",socket);
    }

    //connect和disconnect 用同样的response
    private static void SendPeerCommandResponse(Socket socket,Document request,String SecretKey,boolean status,String msg)throws IOException{
        String command = request.getString("command").replace("REQUEST","RESPONSE");
        log.info("command "+command);
        request.append("command",command);
        request.append("status",status);
        request.append("message",msg);
        String cipher = Protocols.AESencryption((request.toJson()+"\n"),SecretKey.getBytes());
        //log.info(Base64.getEncoder().encodeToString(cipher.getBytes())+"       Base64 编码前 ：      "+cipher+"  length = "+cipher.length());
        Document payload = new Document();
        payload.append("payload",Base64.getEncoder().encodeToString(cipher.getBytes()));
        Protocols.sendMessage(payload.toJson()+"\n",socket);
    }




}
