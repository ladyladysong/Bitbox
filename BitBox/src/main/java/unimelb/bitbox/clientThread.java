package unimelb.bitbox;//


import org.json.simple.JSONObject;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

//Interactive client that reads input from the command line and sends it to
//a server
public class clientThread implements Runnable {
    Socket socket=null;
    ServerMain serverMain;
    Queue<String> recommandPeers = new LinkedList<String>();
    private String messageFromServer;
    public static Logger log=Logger.getLogger(clientThread.class.getName());

    public clientThread(ServerMain serverMain) {//改成peer
        this.serverMain = serverMain;

    }


    @Override
    public void run() {
        while(serverMain.peerQue.size()!=0){
            String peer= serverMain.peerQue.poll();
            String remoteHost = peer.substring(0, peer.indexOf(":"));
            int remotePort = Integer.parseInt(peer.substring(peer.indexOf(":") + 1));
            Random random = new Random();
            int sleepTime=(random.nextInt(6)+1)*500;
            //log.info(remoteHost+String.valueOf(remotePort));
            try{
                Thread.sleep(sleepTime);
                this.socket= new Socket(remoteHost,remotePort);
            }catch (IOException e){
                //e.printStackTrace();
                serverMain.peerQue.offer(peer);
                continue;
                //一直连接；
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
            do{
                try {
                    // Create a stream socket bounded to any port and connect it to the
                    log.info("Connection establishing....");
                    // Get the input/output streams for reading/writing data from/to the socket
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                    //if(serverMain.getLimitNumber()>=Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"))){
                    //log.info("Limitation of Connection reached.");
                    //socket.close();
                    //} Open code above can set restrict for client number
                    String response=Protocols.sendHandshake(socket);
                    log.info("Response from Server: "+response);
                    this.messageFromServer=response;
                    //String add=null;
                    if(response.contains("HANDSHAKE_RESPONSE")) {
                        if(serverMain.containsconnectedPeer(peer)){
                            if(serverMain.getMapvalue(peer).isClosed()) {
                                //serverMain.delSockets(serverMain.getMapvalue(peer));
                                serverMain.removeMapKV(peer);
                                serverMain.addMap(peer, socket);
                                serverMain.addSockets(socket);
                            }else {
                                log.info(peer+": Handshake already exists!");
                                Protocols.sendMessage("Handshake exists!",socket);
                                socket.close();
                            }
                        }else{
                            serverMain.addconnectedPeers(peer);
                            serverMain.addMap(peer,socket);
                            serverMain.addSockets(socket);
                        }
                        log.info("Handshake success, connected");
                        //while ((add = in.readLine()) != null) {
                        //System.out.println(add);
                        HandlerClient fileHandler= new HandlerClient(this.socket,this.serverMain,peer);
                        fileHandler.start();
                        break;
                    }
                    else if(response.contains("CONNECTION_REFUSED")){
                        log.info("Connection refused! Changing to another Peer...");
                        socket.close();
                        List<String> vistedPeer= new ArrayList<String>();
                        vistedPeer.add(peer);
                        GetRefuseMsg.getQueue(response,recommandPeers);
                        log.info("rcmdPeers size :"+recommandPeers.size());
                        while(recommandPeers.size()!=0){
                            String newpeer=recommandPeers.poll();
                            log.info("Add to new peer "+newpeer);
                            if(!vistedPeer.contains(newpeer)){
                                String newRemoteHost = newpeer.substring(0, newpeer.indexOf(":"));
                                int newRemotePort = Integer.parseInt(newpeer.substring(newpeer.indexOf(":") + 1));
                                try {
                                    this.socket=new Socket(newRemoteHost,newRemotePort);

                                }catch (IOException e){
                                    continue;
                                }
                                vistedPeer.add(newpeer);
                                log.info("VisitedPeer: "+vistedPeer.get(0));
                                break;
                            }
                        }
                        log.info("rcmdPeers size :"+recommandPeers.size());
                    }
                    else{
                        log.info("connection failed");
                        log.info("The message from Server: "+response);
                        socket.close();
                        break;
                    }
                } catch (UnknownHostException e) {
                    log.info(e.getMessage());
                }catch (SocketException e){
                    log.info("Connection reset !");
                    serverMain.peerQue.offer(peer);
                    continue;
                }
                catch (Exception e) {
                    log.info(e.getMessage());
                }

            }while(true);

        }
    }
}
