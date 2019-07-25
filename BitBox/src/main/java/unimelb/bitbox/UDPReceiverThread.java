package unimelb.bitbox;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public class UDPReceiverThread implements Runnable {
    private static Logger log = Logger.getLogger(UDPReceiverThread.class.getName());
    DatagramSocket socket;
    UDPServerMain serverMain;
    Queue<String> recommandPeer= new LinkedList<String>();
    String localHostPort = InetAddress.getLocalHost().getHostAddress()+":"+Configuration.getConfigurationValue("port");
    int maxconnection = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
    public UDPReceiverThread(DatagramSocket socket,UDPServerMain serverMain) throws UnknownHostException {
        this.socket = socket;
        this.serverMain = serverMain;
    }

    @Override
    public void run() {
        while(true){
            byte[] recvData = new byte[65535];
            DatagramPacket rcvPacket = new DatagramPacket(recvData,recvData.length);
            try {
                socket.receive(rcvPacket);
                InetAddress address=rcvPacket.getAddress();
                String ip =rcvPacket.getAddress().getHostAddress();
                int port = rcvPacket.getPort();
                String peerName = ip+":"+String.valueOf(port);
                String msg = new String(rcvPacket.getData(),0,rcvPacket.getLength());
                log.info("Message from other Peer: "+msg);
                if(msg.contains("HANDSHAKE_RESPONSE")){
                    if(!serverMain.containsconnectedPeer(peerName)){
                        serverMain.addconnectedPeers(peerName);
                        log.info("The number of connected Peer: "+serverMain.connectedPeers.size());
                        if(serverMain.containsPeer(peerName)){
                            serverMain.delPeers(peerName);
                        }

                    }
                }
                else if(msg.contains("HANDSHAKE_REQUEST")){
                    //!serverMain.containsconnectedPeer(peerName
                    if(!serverMain.containsconnectedPeer(peerName)){
                        if(serverMain.commingPeers.size()<maxconnection){
                            serverMain.addconnectedPeers(peerName);
                            serverMain.addComPeer(peerName);
                            if(serverMain.containsPeer(peerName)){
                                serverMain.delPeers(peerName);
                                //log.info("DELETE Success");
                            }
                            UDPProtocols.createAndSendAck(socket,address,port);

                        }
                        else {
                            UDPProtocols.createAndSendRFS(socket,address,port,serverMain.connectedPeers);
                        }
                    }
                    else {
                        UDPProtocols.createAndSendAck(socket,address,port);
                    }
                }
                else if(msg.contains("CONNECTION_REFUSED")){
                    log.info("Peer "+peerName+" refuse connection, waiting(udpTime out) for sending message to another peer");
                    GetRefuseMsg.getQueue(msg,recommandPeer);
                    boolean flag = true;
                    while(recommandPeer.size()!=0){
                        String newPeer = recommandPeer.poll();
                        if(newPeer.equals(localHostPort)){
                            continue;
                        }
                        else if(serverMain.containsconnectedPeer(newPeer)){
                            continue;
                        }
                        else {
                            serverMain.delPeers(peerName);
                            serverMain.addPeers(newPeer);
                            break;
                        }
                    }
                }
                else if(msg.contains("INVALID_PROTOCOL")){
                    log.info("Errors in protocols");
                }
                else if(msg.contains("DIRECTORY_CREATE_RESPONSE")){
                    String request = GetRequest.getDirCreateRQ(msg)+"+"+ip+"+"+port;
                    //String[] seperate = request.split("\\+");
                    //log.info(seperate[0]);
                    //log.info(seperate[1]);
                    //log.info("aaaaaaa: "+request);
                    if(serverMain.containsMessage(request)){
                        serverMain.delMessage(request);
                        log.info("success");
                    }

                }
                else if(msg.contains("DIRECTORY_DELETE_RESPONSE")){
                    String request = GetRequest.getDirDeleteRQ(msg)+"+"+ip+"+"+port;
                    if(serverMain.containsMessage(request)){
                        serverMain.delMessage(request);
                        log.info("success");
                    }
                }
                else if(msg.contains("FILE_CREATE_RESPONSE")){
                    String request = GetRequest.getFileCreateRQ(msg)+"+"+ip+"+"+port;
                    if(serverMain.containsMessage(request)){
                        serverMain.delMessage(request);
                        log.info("success");
                    }
                }
                else if(msg.contains("FILE_DELETE_RESPONSE")){
                    String request = GetRequest.getFileDeleteRQ(msg)+"+"+ip+"+"+port;
                    if(serverMain.containsMessage(request)){
                        serverMain.delMessage(request);
                        log.info("success");
                    }
                }
                else if(msg.contains("FILE_MODIFY_RESPONSE")){
                    String request = GetRequest.getFileModifyRQ(msg)+"+"+ip+"+"+port;
                    if(serverMain.containsMessage(request)){
                        serverMain.delMessage(request);
                        log.info("success");
                    }
                }
                else {
                    if(!serverMain.containsconnectedPeer(peerName)){
                        serverMain.addconnectedPeers(peerName);
                        serverMain.addComPeer(peerName);
                    }
                    log.info("Handle message: "+msg);
                    HandleFileEvent(msg,address,port);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
    public void HandleFileEvent(String msg,InetAddress address,int port) throws IOException, NoSuchAlgorithmException, ParseException {
        if (msg != null) {
            String ip = address.getHostAddress();
            Long blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
            if (msg.contains("FILE_CREATE_REQUEST")) {
                Document fileCreateRequest = Document.parse(msg);
                Document file_desc = (Document) fileCreateRequest.get("fileDescriptor");
                long fileSize = file_desc.getLong("fileSize");
                String md5 = file_desc.getString("md5");
                long lastModify = file_desc.getLong("lastModified");
                String pathName = fileCreateRequest.getString("pathName");
                if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
                    if (!this.serverMain.fileSystemManager.fileNameExists(pathName)) {
                        if (this.serverMain.fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModify)) {
                            //create file loader, successful
                            fileCreateResponse(file_desc, pathName, "file loader ready", true,address,port);
                            if (!this.serverMain.fileSystemManager.checkShortcut(pathName)) {
                                long length = 0;
                                if(fileSize<=blockSize){
                                    length = fileSize;
                                }
                                else {
                                    length = blockSize;
                                }
                                fileBytesCreateRequest(md5, lastModify, fileSize, pathName, 0, length,address,port);
                                log.info(fileCreateRequest.toString());
                                //faster transferring
                            } else
                                log.info("copy content from existing file");
                        } else
                            fileCreateResponse(file_desc, pathName, "there was a problem creating the file", false,address,port);
                    } else
                        fileCreateResponse(file_desc, pathName, "pathname aleady exists", false,address,port);
                } else
                    fileCreateResponse(file_desc, pathName, "unsafe pathname given", false,address,port);
            }

            if (msg.contains("FILE_BYTES_REQUEST")) {
                Document fileBytesRequest = Document.parse(msg);
                Document file_desc = (Document) fileBytesRequest.get("fileDescriptor");
                long fileSize = file_desc.getLong("fileSize");
                String md5 = file_desc.getString("md5");
                long lastModify = file_desc.getLong("lastModified");
                String pathName = fileBytesRequest.getString("pathName");
                long pos = fileBytesRequest.getLong("position");
                long length = fileBytesRequest.getLong("length");
                if (fileSize > blockSize) {
                    //long count = 0;
                    //long nextpos=pos+blockSize;
                    if (pos< fileSize) {
                        if ((pos + blockSize) > fileSize)
                            length = fileSize - pos;
                        else
                            length = blockSize;
                    }
                    ByteBuffer buf = this.serverMain.fileSystemManager.readFile(md5, pos, length);
                    String content = Base64.getEncoder().encodeToString(buf.array());
                    fileBytesResponse(file_desc, pathName, pos, length, content, "successful read", true,address,port);
                }
                if (fileSize <= blockSize) {
                    ByteBuffer buf = this.serverMain.fileSystemManager.readFile(md5, pos, length);
                    String content = Base64.getEncoder().encodeToString(buf.array());
                    log.info("read content= " + content);
                    //? when not successful read?
                    fileBytesResponse(file_desc, pathName, pos, length, content, "successful read", true,address,port);
                }
            }

            if (msg.contains("FILE_BYTES_RESPONSE")) {
                //System.out.println(msg);
                Document fileBytesResponse = Document.parse(msg);
                Document file_desc = (Document) fileBytesResponse.get("fileDescriptor");
                long fileSize = file_desc.getLong("fileSize");
                String pathName = fileBytesResponse.getString("pathName");
                long pos = fileBytesResponse.getLong("position");
                String content = fileBytesResponse.getString("content");
                long length = fileBytesResponse.getLong("length");
                String md5 = file_desc.getString("md5");
                long lastModify = file_desc.getLong("lastModified");
                ByteBuffer buf = ByteBuffer.wrap(Base64.getDecoder().decode(content));
                String request = GetRequest.getFileByteRQ(msg)+"+"+ip+"+"+port;
                log.info("Get request: "+request);
                if(serverMain.containsMessage(request)){
                    serverMain.delMessage(request);
                    log.info("success delete");
                }
                if (this.serverMain.fileSystemManager.writeFile(pathName, buf, pos)) {
                    log.info("successfully write");
                }
                if (this.serverMain.fileSystemManager.checkWriteComplete(pathName)) {
                    log.info("write completed");
                }
                else {
                    log.info("writing is not completed");
                    pos=pos+length;
                    if (pos< fileSize) {
                        if ((pos + blockSize) > fileSize)
                            length = fileSize - pos;
                        else
                            length = blockSize;
                    }
                    fileBytesCreateRequest(md5,lastModify,fileSize,pathName,pos,length,address,port);
                }
            }

            if (msg.contains("FILE_DELETE_REQUEST")) {
                Document fileDeleteReq = Document.parse(msg);
                Document file_desc = (Document) fileDeleteReq.get("fileDescriptor");
                long fileSize = file_desc.getLong("fileSize");
                String md5 = file_desc.getString("md5");
                long lastModify = file_desc.getLong("lastModified");
                String pathName = fileDeleteReq.getString("pathName");
                if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
                    if (this.serverMain.fileSystemManager.fileNameExists(pathName)) {
                        if (this.serverMain.fileSystemManager.deleteFile(pathName, lastModify, md5)) {
                            //response
                            fileDeleteResponse(file_desc, pathName, "file deleted", true,address,port);
                        } else
                            fileDeleteResponse(file_desc, pathName, "there was a problem deleting the file", false,address,port);
                    } else
                        fileDeleteResponse(file_desc, pathName, "pathname does not exist", false,address,port);
                } else
                    fileDeleteResponse(file_desc, pathName, "unsafe pathname given", false,address,port);
            }

            if (msg.contains("FILE_MODIFY_REQUEST")) {
                Document fileModifyReq = Document.parse(msg);
                Document file_desc = (Document) fileModifyReq.get("fileDescriptor");
                long fileSize = file_desc.getLong("fileSize");
                String md5 = file_desc.getString("md5");
                long lastModify = file_desc.getLong("lastModified");
                String pathName = fileModifyReq.getString("pathName");
                if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
                    if (this.serverMain.fileSystemManager.fileNameExists(pathName)) {
                        //if not has same content, return false

                        if (this.serverMain.fileSystemManager.modifyFileLoader(pathName, md5, lastModify)) {
                            fileModifyResponse(file_desc, pathName, "file loader ready", true,address,port);
                            fileBytesCreateRequest(md5, lastModify, fileSize, pathName, 0, fileSize,address,port);
                        } else
                            fileModifyResponse(file_desc, pathName, "there was a problem modifying the file", false,address,port);

                    } else
                        fileModifyResponse(file_desc, pathName, "pathname already exists", false,address,port);
                } else
                    fileModifyResponse(file_desc, pathName, "unsafe pathname given", false,address,port);
            }

            if (msg.contains("DIRECTORY_CREATE_REQUEST")) {
                Document DirCreateRequest = Document.parse(msg);
                String pathName = DirCreateRequest.getString("pathName");
                if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
                    if (!this.serverMain.fileSystemManager.dirNameExists(pathName)) {
                        if (this.serverMain.fileSystemManager.makeDirectory(pathName))
                            DirCreateResponse(pathName, "directory created", true,address,port);
                    } else
                        DirCreateResponse(pathName, "pathname already exitsts", false,address,port);
                } else
                    DirCreateResponse(pathName, "unsafe pathname given", false,address,port);
            }
            if (msg.contains("DIRECTORY_DELETE_REQUEST")) {
                Document DirDeleteRequest = Document.parse(msg);
                String pathName = DirDeleteRequest.getString("pathName");
                if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
                    if (this.serverMain.fileSystemManager.dirNameExists(pathName)) {
                        if (this.serverMain.fileSystemManager.deleteDirectory(pathName))
                            DirDeleteResponse(pathName, "directory deleted", true,address,port);
                    } else
                        DirDeleteResponse(pathName, "there was a problem deleting the directory", false,address,port);
                } else
                    DirDeleteResponse(pathName, "unsafe pathname given", false,address,port);
            }

        }
    }

    private void fileCreateResponse(Document file_desc, String pathName, String msg, boolean status,InetAddress address, int port) throws IOException {
        Document fileCreateResp = new Document();
        fileCreateResp.append("command", "FILE_CREATE_RESPONSE");
        fileCreateResp.append("fileDescriptor", file_desc);
        fileCreateResp.append("pathName", pathName);
        fileCreateResp.append("message", msg);
        fileCreateResp.append("status", status);
        UDPProtocols.sendMessage(socket,fileCreateResp.toJson()+"\n",address,port);
    }

    private void fileBytesCreateRequest(String md5, long lastModify, long fileSize, String pathName, long pos, long length,InetAddress address, int port) throws IOException {
        Document BytesCreateRequest = new Document();
        BytesCreateRequest.append("command", "FILE_BYTES_REQUEST");
        Document file_desc = new Document();
        file_desc.append("md5", md5);
        file_desc.append("lastModified", lastModify);
        file_desc.append("fileSize", fileSize);
        BytesCreateRequest.append("position",pos);
        BytesCreateRequest.append("length",length);
        BytesCreateRequest.append("fileDescriptor", file_desc);
        BytesCreateRequest.append("pathName", pathName);
        UDPProtocols.sendMessage(socket,BytesCreateRequest.toJson()+"\n",address,port);
        String request = BytesCreateRequest.toJson()+"\n"+"+"+address.getHostAddress()+"+"+port;
        log.info("Save: "+request);
        serverMain.addMessage(request);
    }


    private void fileBytesResponse(Document file_desc, String pathName, long pos, long length, String content, String msg, boolean status,InetAddress address, int port) throws IOException {
        Document fileByteRsp = new Document();
        fileByteRsp.append("command", "FILE_BYTES_RESPONSE");
        fileByteRsp.append("fileDescriptor", file_desc);
        fileByteRsp.append("pathName", pathName);
        fileByteRsp.append("position", pos);
        fileByteRsp.append("length", length);
        fileByteRsp.append("content", content);
        fileByteRsp.append("message", msg);
        fileByteRsp.append("status", status);
        //log.info(file_desc.toString());
        UDPProtocols.sendMessage(socket,fileByteRsp.toJson()+"\n",address,port);
    }

    private void fileDeleteResponse(Document file_desc, String pathName, String msg, boolean status,InetAddress address, int port) throws IOException {
        Document fileDelResponse = new Document();
        fileDelResponse.append("command", "FILE_DELETE_RESPONSE");
        fileDelResponse.append("fileDescriptor", file_desc);
        fileDelResponse.append("pathName", pathName);
        fileDelResponse.append("message", msg);
        fileDelResponse.append("status", status);
        UDPProtocols.sendMessage(socket,fileDelResponse.toJson()+"\n",address,port);
    }


    private void fileModifyResponse(Document file_desc, String pathName, String msg, boolean status,InetAddress address,int port) throws IOException {
        Document fileModResp = new Document();
        fileModResp.append("command", "FILE_MODIFY_RESPONSE");
        fileModResp.append("fileDescriptor", file_desc);
        fileModResp.append("pathName", pathName);
        fileModResp.append("message", msg);
        fileModResp.append("status", status);
        UDPProtocols.sendMessage(socket,fileModResp.toJson()+"\n",address,port);

    }

    private void DirCreateResponse(String pathName, String msg, boolean status,InetAddress address, int port) throws IOException {
        Document dirCreRsp = new Document();
        dirCreRsp.append("command", "DIRECTORY_CREATE_RESPONSE");
        dirCreRsp.append("pathName", pathName);
        dirCreRsp.append("message", msg);
        dirCreRsp.append("status", status);
        UDPProtocols.sendMessage(socket,dirCreRsp.toJson()+"\n",address,port);

    }


    private void DirDeleteResponse(String pathName, String msg, boolean status, InetAddress address, int port) throws IOException {
        Document dirDelRsp = new Document();
        dirDelRsp.append("command", "DIRECTORY_DELETE_RESPONSE");
        dirDelRsp.append("pathName", pathName);
        dirDelRsp.append("message", msg);
        dirDelRsp.append("status", status);
        UDPProtocols.sendMessage(socket,dirDelRsp.toJson()+"\n",address,port);
    }
}
