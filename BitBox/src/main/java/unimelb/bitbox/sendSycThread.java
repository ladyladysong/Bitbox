package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;

import java.net.SocketException;
import java.util.List;
import java.util.logging.Logger;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
public class sendSycThread implements Runnable {
    ServerMain serverMain=null;
    private static Logger log= Logger.getLogger(serverThread.class.getName());
    public sendSycThread(ServerMain serverMain){
        this.serverMain=serverMain;
    }

    @Override
    public void run() {
        int syncInterval=Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));
        while(true){
            try {
                log.info(String.valueOf(serverMain.Sockets.size()));
                Thread.sleep(syncInterval*100);
                List<FileSystemEvent> events= this.serverMain.fileSystemManager.generateSyncEvents();
                if(serverMain.Sockets.size()>0){
                    log.info("Sending synchronising messages...");
                    for(FileSystemEvent event:events){
                        this.serverMain.processFileSystemEvent(event);
                    }
                }
                Thread.sleep(syncInterval*1000);

            } catch (InterruptedException e) {
                log.info(e.getMessage());
            }
        }
    }
}
