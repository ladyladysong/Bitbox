package unimelb.bitbox;//

import unimelb.bitbox.util.Configuration;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class UDPSenderThread implements Runnable{
    private static Logger log = Logger.getLogger(UDPSenderThread.class.getName());
    DatagramSocket datagramSocket=null;
    UDPServerMain serverMain=null;
    public UDPSenderThread(DatagramSocket datagramSocket,UDPServerMain serverMain){
        this.datagramSocket = datagramSocket;
        this.serverMain = serverMain;
    }

    @Override
    public void run() {
        Map<String,Integer> Retry = new HashMap<String,Integer>();
        while(true){
            /**if(serverMain.peerQsize()!=0){
             //log.info("size "+serverMain.peerQsize());
             while(serverMain.peerQsize()!=0){
             String epeer = serverMain.peerQue.poll();
             Retry.put(epeer,0);
             }
             }**/
            int retrytimes = Integer.parseInt(Configuration.getConfigurationValue("udpRetries"));
            if (serverMain.peers.size()!=0){
                List<String> peercopy = serverMain.CopyPeers();
                log.info("Peer size "+peercopy.size());
                for(int i =0;i<peercopy.size();i++){
                    String peer = peercopy.get(i);
                    log.info("Connect to peer: "+peer);
                    String remoteHost = peer.substring(0, peer.indexOf(":"));
                    int remotePort = Integer.parseInt(peer.substring(peer.indexOf(":") + 1));
                    try {
                        if(Retry.get(peer)==null){
                            Retry.put(peer,0);
                        }
                        if(Retry.get(peer)>0 && Retry.get(peer)<=Integer.parseInt(Configuration.getConfigurationValue("udpRetries"))){
                            log.info("Start re-connection with peers....");
                            log.info("Retry "+Retry.get(peer)+" times...");
                        }
                        //InetAddress address = InetAddress.getByName(remoteHost);
                        //UDPProtocols.sendHandshake(datagramSocket,address,remotePort);
                        if(Retry.get(peer)<Integer.parseInt(Configuration.getConfigurationValue("udpRetries"))){
                            InetAddress address = InetAddress.getByName(remoteHost);
                            UDPProtocols.sendHandshake(datagramSocket,address,remotePort);
                            Retry.put(peer,Retry.get(peer)+1);
                        }
                        else if(Retry.get(peer)==Integer.parseInt(Configuration.getConfigurationValue("udpRetries"))){
                            InetAddress address = InetAddress.getByName(remoteHost);
                            UDPProtocols.sendHandshake(datagramSocket,address,remotePort);
                            Retry.put(peer,Retry.get(peer)+1);
                            serverMain.peers.remove(peer);
                        }
                    } catch (UnknownHostException e) {
                        log.info(e.getMessage());
                    } catch (IOException e) {
                        log.info(e.getMessage());
                    }
                }
                try {
                    Thread.sleep(Integer.parseInt(Configuration.getConfigurationValue("udpTimeout")));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                continue;
            }
        }
    }
}
