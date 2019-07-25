package unimelb.bitbox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import sun.nio.cs.ISO_8859_2;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;

import javax.sql.rowset.serial.SerialStruct;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import static java.lang.System.out;
import static java.lang.System.setOut;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * Created by Pan on 8/5/19.
 */
public class Client {
    public static Logger log=Logger.getLogger(Client.class.getName());

    public static byte[] SecretKey;
    public static String command;
    public static String serverHostPort;
    public static String peerHostPort;
    public static String identity;


    public static void main(String[] args) {

        CmdLineArgs argsBean = new CmdLineArgs();
        CmdLineParser parser = new CmdLineParser(argsBean);
        try {

            //Parse the arguments
            parser.parseArgument(args);

            //After parsing, the fields in argsBean have been updated with the given
            //command line arguments
            command = argsBean.getCommand();
            serverHostPort = argsBean.getServerHostport();
            peerHostPort = argsBean.getPeerHostport();
            identity = argsBean.getIdentify();

            System.out.println("command: "+ argsBean.getCommand());
            System.out.println("server hp: " + argsBean.getServerHostport());
            System.out.println("peer hp: " + argsBean.getPeerHostport());
            System.out.println("identity "+ argsBean.getIdentify());
        } catch (CmdLineException e) {
            log.info(e.getMessage());
            e.printStackTrace();
        }
        String s_host = serverHostPort.substring(0,serverHostPort.indexOf(":"));
        int s_port = Integer.parseInt(serverHostPort.substring(serverHostPort.indexOf(":")+1));
        Socket socket=null;
        System.out.println("connecting as a client");
        try{
            socket = new Socket(s_host, s_port);
        } catch (UnknownHostException e) {
           log.info(e.getMessage());
           e.printStackTrace();
        } catch (IOException e) {
            log.info(e.getMessage());
            e.printStackTrace();
        }

        try {
            BufferedWriter bw= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader br= new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String msg = SendAuthRequest(socket,identity);
            if (msg.contains("AUTH_RESPONSE")){
                Document AuthResp = Document.parse(msg);
                boolean status = AuthResp.getBoolean("status");
                if (status){
                    //the key-->encode().getbytes()
                    //System.out.println(AuthResp.getString("AES128"));
                    //String EncodedSecretKey = new String(Base64.getDecoder().decode(AuthResp.getString("AES128")));
                    byte[] encrptedSecretKey = Base64.getDecoder().decode(AuthResp.getString("AES128"));
                    //String EncodedSecretKey = AuthResp.getString("AES128");
                    PrivateKey privateKey = Protocols.decodePrivateKey("bitboxclient_rsa");
                    try{
                        SecretKey = Protocols.RSAdecryption(encrptedSecretKey,privateKey);
                        log.info("The received key is "+SecretKey);
                        log.info("waiting around 10s for response");
                        if (command.equals("list_peers")){
                            SendListPeerRequest(socket);
                        }
                        if (command.equals("connect_peer")){

                            SendConnectPeerRequest(socket,peerHostPort);
                        }
                        if (command.equals("disconnect_peer")){
                            SendDisconnectPeerRequest(socket,peerHostPort);
                        }
                        String rsp = null;
                        do{
                            rsp = br.readLine();
                            handleReceivedRsp(rsp,SecretKey);

                        }while (rsp!=null);
                        socket.close();
                        //TODO:所有的exception都被catch了
                    }catch (Exception e){
                        log.info(e.getMessage());
                        e.printStackTrace();
                    }
                }else
                    log.info("Please the check the identity of the client!");
            }
            else
                log.info("Status false because of: "+msg);
        }catch (IOException io) {
            log.info(io.getMessage());
            io.printStackTrace();
        }catch (NullPointerException e){
            log.info(e.getMessage());
            e.printStackTrace();
        }


    }

    private static void handleReceivedRsp(String msg,byte[] SecretKey) {
        System.out.println("the received msg is " + msg);
        if (msg != null) {
            if (msg.contains("payload")) {
                //取出payload部分->Base64解码->AES解密->Parse为一个Document（正常的明文信息）
                String msg_ = new String(Base64.getDecoder().decode(Document.parse(msg).getString("payload")));

                //log.info(msg+"       Base64 解码后 ：      "+msg_+"  length = "+msg_.length());
                String message_Str = Protocols.AESdecryption(msg_, SecretKey);
                Document message = Document.parse(message_Str);
                String command = message.getString("command");
                if (command.contains("RESPONSE")) {
                    if (command.equals("LIST_PEERS_RESPONSE")) {
                        getPeerlistInfo(message_Str);
                    } else if (command.equals("CONNECT_PEER_RESPONSE")) {
                        log.info("Received message of CONNECT_PEER_RESPONSE " + message.toJson());
                    } else if (command.equals("DISCONNECT_PEER_RESPONSE")) {
                        log.info("Received message of DISCONNECT_PEER_RESPONSE " + message.toJson());
                    }
                } else {
                    log.info("Not the valid response format!");
                }
            } else {
                log.info("Message format error !");
            }
        }
        else{
            log.info("No message coming.");
        }

    }

    private static String SendAuthRequest(Socket socket,String id)throws IOException{
        Document request = new Document();
        request.append("command","AUTH_REQUEST");
        request.append("identity",id);
        String msg = Protocols.sendAuthRequest(socket,request.toJson()+"\n");
        //receive response from
        return msg;
    }

    private static void getPeerlistInfo(String message){
        try{
            JSONObject msg = (JSONObject)(new JSONParser().parse(message));
            JSONArray Peers = (JSONArray) msg.get("peers");
            log.info("The address of the connected peer "+Peers.toString());
            for (int i = 0;i<Peers.size();i++){
                String peer = Peers.get(i).toString();
                log.info(peer);
                JSONObject newpeer = (JSONObject)(new JSONParser().parse(peer));
                String hostport = newpeer.get("host").toString()+":"+newpeer.get("port");
                //log.info("The address of the connected peer : "+hostport);
            }
        }catch (ParseException e){
            log.info(e.getMessage());
            e.printStackTrace();
        }

    }

    private static void SendListPeerRequest(Socket socket)throws IOException{
        Document request = new Document();
        request.append("command","LIST_PEERS_REQUEST");
        String plain = request.toJson()+"\n";
        String cipher = Protocols.AESencryption(plain,SecretKey);
        Document payload = new Document();
        payload.append("payload",Base64.getEncoder().encodeToString(cipher.getBytes()));
        log.info("client sending request: "+payload.toJson()+"\n");
        Protocols.sendMessage(payload.toJson()+"\n",socket);

    }

    private static void SendConnectPeerRequest(Socket socket,String hostport)throws IOException{
        String host = hostport.substring(0,hostport.indexOf(":"));
        String port = hostport.substring(hostport.indexOf(":")+1);
        Document request = new Document();
        request.append("command","CONNECT_PEER_REQUEST");
        request.append("host",host);
        request.append("port",port);
        String plain = request.toJson()+"\n";
        String cipher = Protocols.AESencryption(plain,SecretKey);
        Document payload = new Document();
        payload.append("payload",Base64.getEncoder().encodeToString(cipher.getBytes()));
        Protocols.sendMessage(payload.toJson()+"\n",socket);
    }

    private static void SendDisconnectPeerRequest(Socket socket,String hostport)throws IOException{
        String host = hostport.substring(0,hostport.indexOf(":"));
        String port = hostport.substring(hostport.indexOf(":")+1);
        Document request = new Document();
        request.append("command","DISCONNECT_PEER_REQUEST");
        request.append("host",host);
        request.append("port",port);
        String plain = request.toJson()+"\n";
        String cipher = Protocols.AESencryption(plain,SecretKey);
        Document payload = new Document();
        payload.append("payload",Base64.getEncoder().encodeToString(cipher.getBytes()));
        Protocols.sendMessage(payload.toJson()+"\n",socket);
    }


}
