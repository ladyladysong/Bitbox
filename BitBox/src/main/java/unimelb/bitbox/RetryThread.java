package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.internal.runners.statements.RunAfters;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RetryThread implements Runnable {
    private static Logger log = Logger.getLogger(RetryThread.class.getName());
    UDPServerMain serverMain;
    DatagramSocket socket;
    private Map<String,Integer> retrytimes = new HashMap<>();
    public RetryThread(UDPServerMain serverMain,DatagramSocket socket){
        this.serverMain=serverMain;
        this.socket=socket;
    }

    @Override
    public void run() {
        while(true){
            try {
                Thread.sleep(Integer.parseInt(Configuration.getConfigurationValue("udpTimeout")));
                log.info("Start to re-send lost messages....");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(serverMain.Messagesize()==0){
                try {
                    Thread.sleep(3000);
                    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else {
                List<String> Messages = serverMain.CopyMessages();
                log.info("Now the size is: "+Messages.size());
                for(String request : Messages){
                    if(retrytimes.get(request)==null){
                        retrytimes.put(request,1);
                    }
                    else if(retrytimes.get(request)>=Integer.parseInt(Configuration.getConfigurationValue("udpRetries"))){
                        //serverMain.delMessage(request);
                        if(request.contains("FILE_BYTES_REQUEST")){
                            JSONObject rsp = null;
                            log.info(request);
                            try {
                                String[] seperate = request.split("\\+");
                                rsp = (JSONObject)(new JSONParser().parse(seperate[0]));
                                String pathName = rsp.get("pathName").toString();
                                this.serverMain.fileSystemManager.cancelFileLoader(pathName);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        continue;
                    }
                    else {
                        retrytimes.put(request,retrytimes.get(request)+1);
                    }
                    String[] seperate = request.split("\\+");
                    log.info("First part: "+seperate[0]);
                    log.info("Second part: "+seperate[1]);
                    log.info("Third part: "+seperate[2]);
                    int rqsPort= Integer.parseInt(seperate[2]);
                    try {
                        UDPProtocols.sendMessage(socket,seperate[0], InetAddress.getByName(seperate[1]),rqsPort);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }


                }
            }
        }
    }
}
