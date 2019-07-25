package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;


public class HandlerServer extends Thread {

    Socket socket;
    ServerMain serverMain=null;
    String peer;

    private static Logger log = Logger.getLogger(HandlerServer.class.getName());

    public HandlerServer(Socket socket, ServerMain serverMain,String peer) {
        this.socket = socket;
        this.serverMain=serverMain;
        this.peer=peer;
    }

    @Override
    public void run() {
        BufferedReader br = null;
        log.info("Start the FileEvent listening thread!");
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            log.info(e.getMessage());
        }
        String msg = null;
        do {
            try {
                msg = br.readLine();
                log.info("Received Message: " + msg);
                if(msg==null){
                    log.info("PeerClient says: Null in Message! Connection reset!");
                    if (!this.socket.isClosed()){
                        try {
                            if(serverMain.containsComPeer(this.peer)){
                                serverMain.delComPeer(this.peer);
                            }
                            serverMain.delSockets(socket);
                            this.socket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }else {
                        if(serverMain.containsComPeer(this.peer)){
                            serverMain.delComPeer(this.peer);
                            serverMain.delconnectedPeers(this.peer);
                        }
                        serverMain.delSockets(socket);
                    }
                    break;
                }

                HandleFileEvent(msg);
            } catch (SocketException se) {
                if (se.getMessage().contains("Connection reset")) {
                    if (!this.socket.isClosed()){
                        try {
                            if(serverMain.containsComPeer(this.peer)){
                                serverMain.delComPeer(this.peer);
                            }
                            serverMain.delSockets(socket);
                            this.socket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }else {
                        if(serverMain.containsComPeer(this.peer)){
                            serverMain.delComPeer(this.peer);
                            serverMain.delconnectedPeers(this.peer);
                        }
                        serverMain.delSockets(socket);
                    }
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException no) {
                no.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        } while (msg != null);
    }

    public void HandleFileEvent(String msg) throws IOException, NoSuchAlgorithmException {
        if (msg != null) {
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
                            fileCreateResponse(file_desc, pathName, "file loader ready", true);
                            if (!this.serverMain.fileSystemManager.checkShortcut(pathName)) {
                                fileBytesCreateRequest(md5, lastModify, fileSize, pathName, 0, fileSize);
                                log.info(fileCreateRequest.toString());
                                //faster transferring
                            } else
                                log.info("copy content from existing file");
                        } else
                            fileCreateResponse(file_desc, pathName, "there was a problem creating the file", false);
                    } else
                        fileCreateResponse(file_desc, pathName, "pathname aleady exists", false);
                } else
                    fileCreateResponse(file_desc, pathName, "unsafe pathname given", false);
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
                    ByteBuffer buf = this.serverMain.fileSystemManager.readFile(md5, pos, length);
                    String content = Base64.getEncoder().encodeToString(buf.array());
                    fileBytesResponse(file_desc, pathName, pos, length, content, "successful read", true);
                }
                if (fileSize <= blockSize) {
                    ByteBuffer buf = this.serverMain.fileSystemManager.readFile(md5, pos, length);
                    String content = Base64.getEncoder().encodeToString(buf.array());
                    log.info("read content= " + content);
                    //? when not successful read?
                    fileBytesResponse(file_desc, pathName, pos, length, content, "successful read", true);
                }
            }

            if (msg.contains("FILE_BYTES_RESPONSE")) {
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
                    fileBytesCreateRequest(md5,lastModify,fileSize,pathName,pos,length);
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
                            fileDeleteResponse(file_desc, pathName, "file deleted", true);
                        } else
                            fileDeleteResponse(file_desc, pathName, "there was a problem deleting the file", false);
                    } else
                        fileDeleteResponse(file_desc, pathName, "pathname does not exist", false);
                } else
                    fileDeleteResponse(file_desc, pathName, "unsafe pathname given", false);
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
                            fileModifyResponse(file_desc, pathName, "file loader ready", true);
                            fileBytesCreateRequest(md5, lastModify, fileSize, pathName, 0, fileSize);
                        } else
                            fileModifyResponse(file_desc, pathName, "there was a problem modifying the file", false);

                    } else
                        fileModifyResponse(file_desc, pathName, "pathname already exists", false);
                } else
                    fileModifyResponse(file_desc, pathName, "unsafe pathname given", false);
            }

            if (msg.contains("DIRECTORY_CREATE_REQUEST")) {
                Document DirCreateRequest = Document.parse(msg);
                String pathName = DirCreateRequest.getString("pathName");
                if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
                    if (!this.serverMain.fileSystemManager.dirNameExists(pathName)) {
                        if (this.serverMain.fileSystemManager.makeDirectory(pathName))
                            DirCreateResponse(pathName, "directory created", true);
                    } else
                        DirCreateResponse(pathName, "pathname already exitsts", false);
                } else
                    DirCreateResponse(pathName, "unsafe pathname given", false);
            }
            if (msg.contains("DIRECTORY_DELETE_REQUEST")) {
                Document DirDeleteRequest = Document.parse(msg);
                String pathName = DirDeleteRequest.getString("pathName");
                if (this.serverMain.fileSystemManager.isSafePathName(pathName)) {
                    if (this.serverMain.fileSystemManager.dirNameExists(pathName)) {
                        if (this.serverMain.fileSystemManager.deleteDirectory(pathName))
                            DirDeleteResponse(pathName, "directory deleted", true);
                    } else
                        DirDeleteResponse(pathName, "there was a problem deleting the directory", false);
                } else
                    DirDeleteResponse(pathName, "unsafe pathname given", false);
            }

        }
    }

    private void fileCreateResponse(Document file_desc, String pathName, String msg, boolean status) throws IOException {
        Document fileCreateResp = new Document();
        fileCreateResp.append("command", "FILE_CREATE_RESPONSE");
        fileCreateResp.append("fileDescriptor", file_desc);
        fileCreateResp.append("pathName", pathName);
        fileCreateResp.append("message", msg);
        fileCreateResp.append("status", status);
        Protocols.sendMessage(fileCreateResp.toJson() + "\n", socket);
    }

    private void fileBytesCreateRequest(String md5, long lastModify, long fileSize, String pathName, long pos, long length) throws IOException {
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
        log.info(BytesCreateRequest.toString());
        Protocols.sendMessage(BytesCreateRequest.toJson() + "\n", socket);
    }


    private void fileBytesResponse(Document file_desc, String pathName, long pos, long length, String content, String msg, boolean status) throws IOException {
        Document fileByteRsp = new Document();
        fileByteRsp.append("command", "FILE_BYTES_RESPONSE");
        fileByteRsp.append("fileDescriptor", file_desc);
        fileByteRsp.append("pathName", pathName);
        fileByteRsp.append("position", pos);
        fileByteRsp.append("length", length);
        fileByteRsp.append("content", content);
        fileByteRsp.append("message", msg);
        fileByteRsp.append("status", status);
        Protocols.sendMessage(fileByteRsp.toJson() + "\n", socket);
    }

    private void fileDeleteResponse(Document file_desc, String pathName, String msg, boolean status) throws IOException {
        Document fileDelResponse = new Document();
        fileDelResponse.append("command", "FILE_DELETE_RESPONSE");
        fileDelResponse.append("fileDescriptor", file_desc);
        fileDelResponse.append("pathName", pathName);
        fileDelResponse.append("message", msg);
        fileDelResponse.append("status", status);
        Protocols.sendMessage(fileDelResponse.toJson() + "\n", socket);
    }


    private void fileModifyResponse(Document file_desc, String pathName, String msg, boolean status) throws IOException {
        Document fileModResp = new Document();
        fileModResp.append("command", "FILE_MODIFY_RESPONSE");
        fileModResp.append("fileDescriptor", file_desc);
        fileModResp.append("pathName", pathName);
        fileModResp.append("message", msg);
        fileModResp.append("status", status);
        Protocols.sendMessage(fileModResp.toJson() + "\n", socket);

    }

    private void DirCreateResponse(String pathName, String msg, boolean status) throws IOException {
        Document dirCreRsp = new Document();
        dirCreRsp.append("command", "DIRECTORY_CREATE_RESPONSE");
        dirCreRsp.append("pathName", pathName);
        dirCreRsp.append("message", msg);
        dirCreRsp.append("status", status);
        Protocols.sendMessage(dirCreRsp.toJson() + "\n", socket);

    }


    private void DirDeleteResponse(String pathName, String msg, boolean status) throws IOException {
        Document dirDelRsp = new Document();
        dirDelRsp.append("command", "DIRECTORY_DELETE_RESPONSE");
        dirDelRsp.append("pathName", pathName);
        dirDelRsp.append("message", msg);
        dirDelRsp.append("status", status);
        Protocols.sendMessage(dirDelRsp.toJson() + "\n", socket);
    }

}