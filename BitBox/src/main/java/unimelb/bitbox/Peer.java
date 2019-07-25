package unimelb.bitbox;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;

public class Peer
{
    private static Logger log = Logger.getLogger(Peer.class.getName());
    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();

        //-------------------------------For clientport--------------------------

        //prepare for TCP mode
        ServerMain TCPserverMain = new ServerMain();
        //prepare for UDP mode
        int udp_port=Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
        DatagramSocket socket = new DatagramSocket(udp_port);
        UDPServerMain udpServerMain= new UDPServerMain(socket);
        int clientPort = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));

        ServerForManager listenClient = new ServerForManager(clientPort,TCPserverMain,udpServerMain);
        new Thread(listenClient).start();


        if (Configuration.getConfigurationValue("mode").toUpperCase().equals("TCP")){
            //ServerMain serverMain= new ServerMain();
            String testHost="203.101.225.147";
            //int testPort=8111;
            InetAddress localHostIp= InetAddress.getLocalHost();
            System.out.println(localHostIp.toString());

            //Create peers list for connecting.
            String p = Configuration.getConfigurationValue("peers");
            StringTokenizer t = new StringTokenizer(p, ",");
            Queue<String> peerQue= new LinkedList<>();
            int port=Integer.parseInt(Configuration.getConfigurationValue("port"));
            while(t.hasMoreTokens()){
                TCPserverMain.peerQue.offer(t.nextToken());
            }

            //for(int i=0;i<serverMain.peers.size();i++){
            //System.out.println(serverMain.peers.get(i));
            //}
            //Threads start...
            serverThread server = new serverThread(port,TCPserverMain);
            new Thread(server).start();

            sendSycThread sychroMsg = new sendSycThread(TCPserverMain);
            new Thread(sychroMsg).start();
            clientThread peerConnection=new clientThread(TCPserverMain);
            new Thread(peerConnection).start();


        }else
            if (Configuration.getConfigurationValue("mode").toUpperCase().equals("UDP")){
                //int port=Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
                int port = udp_port;
                //DatagramSocket socket = new DatagramSocket(port);
                //UDPServerMain udpServerMain= new UDPServerMain(socket);
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
                    udpServerMain.peers.add(t.nextToken());
                }
                for(int i=0;i<udpServerMain.peers.size();i++){
                    System.out.println(udpServerMain.peers.get(i));
                    udpServerMain.peerQue.offer(udpServerMain.peers.get(i));
                }
                System.out.println("size "+udpServerMain.peerQue.size());


                UDPReceiverThread receiverThread = new UDPReceiverThread(socket,udpServerMain);
                new Thread(receiverThread).start();
                UDPSenderThread senderThread = new UDPSenderThread(socket,udpServerMain);
                new Thread(senderThread).start();
                RetryThread retryThread = new RetryThread(udpServerMain,socket);
                new Thread(retryThread).start();
                SendExtraMsg sendExtraMsg = new SendExtraMsg(socket,udpServerMain);
                new Thread(sendExtraMsg).start();
                UDPScyThread scyThread = new UDPScyThread(udpServerMain);
                new Thread(scyThread).start();

            }
            else
                log.info("Wrong mode!!!!!!!!!!!!");

    }
}
