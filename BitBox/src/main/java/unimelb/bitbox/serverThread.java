package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.logging.Logger;

public class serverThread implements Runnable {
    private static Logger log= Logger.getLogger(serverThread.class.getName());
    private ServerSocket serverSocket = null;
    private ServerMain serverMain;
    private int port;
    public serverThread(int port,ServerMain serverMain){//改成configuration里的地址
        this.port=port;
        this.serverMain=serverMain;
        try {
            this.serverSocket=new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true){
            try{
                log.info("Receiving connection....");
                Socket clientSocket=this.serverSocket.accept();
                log.info("Connection received.");
                //log.info("Connected Members.."+String.valueOf(serverMain.getLimitNumber()));
                String msg=Protocols.getMessage(clientSocket);
                log.info(msg);
                if(msg.contains("HANDSHAKE_REQUEST")){
                    if(serverMain.getLimitNumber()>= Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"))){
                        log.info("Maximum size reached");
                        Protocols.createAndSendRFS(clientSocket,serverMain.connectedPeers);
                        clientSocket.close();
                        continue;
                    }
                    log.info("Received Handshake Request!");
                    String peer=GetHostPort.getHostPort(msg);
                    try {
                        if(serverMain.containsconnectedPeer(peer)){
                            if(serverMain.getMapvalue(peer).isClosed()){
                                serverMain.delComPeer(peer);
                                //serverMain.delSockets(serverMain.getMapvalue(peer));//去掉原来的socket.
                                serverMain.addSockets(clientSocket);
                                serverMain.removeMapKV(peer);
                                serverMain.addMapKV(peer,clientSocket);
                            }
                            else{
                                log.info("Server:Handshake exists!");
                                Protocols.sendMessage("Handshake exists!",clientSocket);
                                clientSocket.close();
                                continue;
                            }
                        }else {
                            serverMain.addconnectedPeers(peer);
                            serverMain.addComPeer(peer);
                            serverMain.addMap(peer,clientSocket);
                            serverMain.addSockets(clientSocket);
                        }
                        if(peer.equals("error")){
                            log.info("Wrong Host and Port in request!");
                            Protocols.createAndSendInvalidProtocol("Wrong format",clientSocket);
                            clientSocket.close();
                            continue;
                        }
                    }catch (NullPointerException e){
                        log.info("Error in gained HostPort!");
                        Protocols.sendMessage("Error in HostPort!",clientSocket);
                        clientSocket.close();
                        continue;
                    }
                    log.info("Connected Members: "+serverMain.getLimitNumber());
                    //log.info(String.valueOf(serverMain.Sockets.size()));
                    Protocols.createAndSendAck(clientSocket);
                    log.info("Synchronise files and directories...");

                    HandlerServer fileHandler= new HandlerServer(clientSocket,this.serverMain,peer);
                    fileHandler.start();
                }
                else {
                    Protocols.createAndSendInvalidProtocol("Connection falied",clientSocket);
                    clientSocket.close();
                    continue;
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
}
