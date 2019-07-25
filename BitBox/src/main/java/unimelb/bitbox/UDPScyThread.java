package unimelb.bitbox;//


import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;

import java.util.List;
import java.util.logging.Logger;

public class UDPScyThread implements Runnable {
    UDPServerMain serverMain;
    private static Logger log = Logger.getLogger(UDPScyThread.class.getName());
    public UDPScyThread(UDPServerMain serverMain){
        this.serverMain = serverMain;
    }

    @Override
    public void run() {
        int syncInterval=Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));
        while(true){
            try {
                log.info(String.valueOf(serverMain.connectedPeers.size()));
                Thread.sleep(syncInterval*1000);
                log.info("Messagesize "+serverMain.Message.size());
                List<FileSystemManager.FileSystemEvent> events= this.serverMain.fileSystemManager.generateSyncEvents();
                if(serverMain.connectedPeers.size()>0){
                    log.info("Sending synchronising messages...");
                    for(FileSystemManager.FileSystemEvent event:events){
                        this.serverMain.processFileSystemEvent(event);
                    }
                }
                //Thread.sleep(syncInterval*1000);

            } catch (InterruptedException e) {
                log.info(e.getMessage());
            }
        }

    }
}
