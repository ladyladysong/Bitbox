package unimelb.bitbox;//

import unimelb.bitbox.util.Configuration;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class UDPTestPeer {
    private static Logger log = Logger.getLogger(UDPTestPeer.class.getName());
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();

        int port=Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
        DatagramSocket socket = new DatagramSocket(port);
        UDPServerMain serverMain= new UDPServerMain(socket);
        String testHost="203.101.225.147";
        //int testPort=8111;
        InetAddress localHostIp= InetAddress.getLocalHost();
        String localip = localHostIp.getHostAddress();
        String localipPort=localip+":"+Configuration.getConfigurationValue("port");
        System.out.println(localipPort);
        //System.out.println(localHostIp.toString());

        //Create peers list for connecting.
        String p = Configuration.getConfigurationValue("peers");
        StringTokenizer t = new StringTokenizer(p, ",");
        List<String> peerList = new ArrayList<String>();
        while(t.hasMoreTokens()){
            //serverMain.peerQue.offer(t.nextToken());
            serverMain.peers.add(t.nextToken());
        }
        for(int i=0;i<serverMain.peers.size();i++){
            System.out.println(serverMain.peers.get(i));
            serverMain.peerQue.offer(serverMain.peers.get(i));
        }
        System.out.println("size"+serverMain.peerQue.size());


        UDPReceiverThread receiverThread = new UDPReceiverThread(socket,serverMain);
        new Thread(receiverThread).start();
        UDPSenderThread senderThread = new UDPSenderThread(socket,serverMain);
        new Thread(senderThread).start();
        RetryThread retryThread = new RetryThread(serverMain,socket);
        new Thread(retryThread).start();
        UDPScyThread scyThread = new UDPScyThread(serverMain);
        new Thread(scyThread).start();
    }
}
