package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SendExtraMsg implements Runnable {
    private static Logger log = Logger.getLogger(SendExtraMsg.class.getName());
    DatagramSocket socket;
    UDPServerMain serverMain;
    public SendExtraMsg(DatagramSocket socket,UDPServerMain serverMain){
        this.serverMain=serverMain;
        this.socket=socket;
    }

    @Override
    public void run() {
        Map<String,Integer> Retry = new HashMap<String,Integer>();
        while(true){
            List<String> extraPeers = serverMain.CopyExtraPeers();
            if(extraPeers.size()==0){
                continue;
            }
            else {
                for (String peer : extraPeers) {
                    String remoteHost = peer.substring(0, peer.indexOf(":"));
                    int remotePort = Integer.parseInt(peer.substring(peer.indexOf(":") + 1));
                    InetAddress address = null;
                    try {
                        address = InetAddress.getByName(remoteHost);

                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    try {
                        /*
                        if(Retry.get(peer)==null){
                            Retry.put(peer,0);
                        }
                        if(Retry.get(peer)>0 && Retry.get(peer)<=Integer.parseInt(Configuration.getConfigurationValue("udpRetries"))){
                            log.info("Start re-connection with extra peers ....");
                            log.info("Retry "+Retry.get(peer)+" times...");
                        }
                        //InetAddress address = InetAddress.getByName(remoteHost);
                        //UDPProtocols.sendHandshake(datagramSocket,address,remotePort);
                        if(Retry.get(peer)<Integer.parseInt(Configuration.getConfigurationValue("udpRetries"))){
                            address = InetAddress.getByName(remoteHost);
                            UDPProtocols.sendHandshake(socket,address,remotePort);
                            Retry.put(peer,Retry.get(peer)+1);
                        }
                        else if(Retry.get(peer)==Integer.parseInt(Configuration.getConfigurationValue("udpRetries"))){
                            address = InetAddress.getByName(remoteHost);
                            UDPProtocols.sendHandshake(socket,address,remotePort);
                            Retry.put(peer,Retry.get(peer)+1);
                            serverMain.extraPeers.remove(peer);
                        }
                        */
                        UDPProtocols.sendHandshake(socket,address,remotePort);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                Thread.sleep(Integer.parseInt(Configuration.getConfigurationValue("udpTimeout")));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
