package unimelb.bitbox;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import sun.font.CreatedFontTracker;
import sun.nio.cs.ISO_8859_13;
import sun.nio.cs.ISO_8859_2;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileDescriptor;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.ISO_8859_1;


public class Protocols {

    private static Logger log = Logger.getLogger(Protocols.class.getName());
    private static int connections;

    protected static String getMessage(Socket socket) {
        try {
            log.info("get message......");
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String response = null;
            String line = null;
            StringBuffer sb = new StringBuffer();
            line = br.readLine();
            log.info("get message from :"+line);
            sb.append(line);
            response = sb.toString();
            return response;
        } catch (IOException e) {
            // TODO catch block
            e.printStackTrace();
        }
        return null;
    }

    protected static void sendMessage(String message, Socket socket) throws IOException {
        if (socket == null || socket.isClosed())
            log.info("Socket is already closed...");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        bw.write(message);
        bw.flush();
    }


    protected static void createAndSendAck(Socket socket) throws IOException {
        // TODO Auto-generated method stub
        JSONObject handshakeRequest = new JSONObject();
        JSONObject hostPort = new JSONObject();
        int port = Integer.parseInt(Configuration.getConfigurationValue("port"));
        String host = socket.getLocalAddress().getHostName();
        hostPort.put("host", host);
        hostPort.put("port", port);
        handshakeRequest.put("hostPort", hostPort);
        handshakeRequest.put("command", "HANDSHAKE_RESPONSE");
        sendMessage(handshakeRequest.toJSONString() + "\n", socket);

    }

    protected static void createAndSendRFS(Socket socket, List<String> connectedPeers) throws IOException {
        // TODO Auto-generated method stub
        JSONObject handshakeResponse = new JSONObject();
        handshakeResponse.put("command", "CONNECTION_REFUSED");
        handshakeResponse.put("message", "connection limit reached");
        JSONArray peers = new JSONArray();

        for (String entry : connectedPeers) {
            JSONObject peer = new JSONObject();
            peer.put("host", entry.substring(0, entry.indexOf(":")));
            peer.put("port", entry.substring(entry.indexOf(":") + 1));
            peers.add(peer);
        }
        handshakeResponse.put("peers", peers);
        sendMessage(handshakeResponse.toJSONString() + "\n", socket);
    }
    private static String createHandshakeRequest(Socket socket) {
        JSONObject handshakeRequest = new JSONObject();
        handshakeRequest.put("command", "HANDSHAKE_REQUEST");
        JSONObject hostPort = new JSONObject();

        hostPort.put("host", socket.getInetAddress().getHostName());
        hostPort.put("port", Integer.parseInt(Configuration.getConfigurationValue("port")));
        handshakeRequest.put("hostPort", hostPort);
        log.info(handshakeRequest.toJSONString());
        return handshakeRequest.toJSONString() + "\n";

    }

    public static String sendHandshake(Socket socket) throws IOException {
        // TODO Auto-generated method stub
        log.info("Sending Handshake Request...");

        // open socket's output stream and send handshake request
        sendMessage(createHandshakeRequest(socket), socket);

        // wait for the response - and see if its nack or ack.
        return getMessage(socket);
    }

    public static String sendAuthRequest(Socket socket,String msg) throws IOException{
        log.info("Send the Auth request");
        sendMessage(msg,socket);
        return getMessage(socket);
    }

    public static void createAndSendFileCreateRequest(Socket socket, FileSystemEvent event) throws IOException {
        // TODO Auto-generated method stub
        JSONObject fileCreateRequest = new JSONObject();
        fileCreateRequest.put("command", "FILE_CREATE_REQUEST");
        JSONObject fileDescriptor = new JSONObject();
        fileDescriptor.put("md5", event.fileDescriptor.md5);
        fileDescriptor.put("lastModified", event.fileDescriptor.lastModified);
        fileDescriptor.put("fileSize", event.fileDescriptor.fileSize);
        fileCreateRequest.put("fileDescriptor", fileDescriptor);
        fileCreateRequest.put("pathName", event.pathName);
        sendMessage(fileCreateRequest.toJSONString() + "\n", socket);
    }

    public static void createAndSendFileDeleteRequest(Socket socket, FileSystemEvent fileSystemEvent)
            throws IOException {
        // TODO Auto-generated method stub
        JSONObject fileDeleteRequest = new JSONObject();
        fileDeleteRequest.put("command", "FILE_DELETE_REQUEST");
        JSONObject fileDescriptor = new JSONObject();
        fileDescriptor.put("md5", fileSystemEvent.fileDescriptor.md5);
        fileDescriptor.put("lastModified", fileSystemEvent.fileDescriptor.lastModified);
        fileDescriptor.put("fileSize", fileSystemEvent.fileDescriptor.fileSize);
        fileDeleteRequest.put("fileDescriptor", fileDescriptor);
        fileDeleteRequest.put("pathName", fileSystemEvent.pathName);
        sendMessage(fileDeleteRequest.toJSONString() + "\n", socket);
    }

    public static void createAndSendFileModifyRequest(Socket socket, FileSystemEvent fileSystemEvent)
            throws IOException {
        // TODO Auto-generated method stub
        JSONObject fileModifyRequest = new JSONObject();
        fileModifyRequest.put("command", "FILE_MODIFY_REQUEST");
        JSONObject fileDescriptor = new JSONObject();
        fileDescriptor.put("md5", fileSystemEvent.fileDescriptor.md5);
        fileDescriptor.put("lastModified", fileSystemEvent.fileDescriptor.lastModified);
        fileDescriptor.put("fileSize", fileSystemEvent.fileDescriptor.fileSize);
        fileModifyRequest.put("fileDescriptor", fileDescriptor);
        fileModifyRequest.put("pathName", fileSystemEvent.pathName);
        sendMessage(fileModifyRequest.toJSONString() + "\n", socket);
    }

    public static void createAndSendDirectoryCreateRequest(Socket socket, FileSystemEvent fileSystemEvent)
            throws IOException {
        // TODO Auto-generated method stub
        JSONObject dirCreateRequest = new JSONObject();
        dirCreateRequest.put("command", "DIRECTORY_CREATE_REQUEST");
        dirCreateRequest.put("pathName", fileSystemEvent.pathName);
        sendMessage(dirCreateRequest.toJSONString() + "\n", socket);
    }

    public static void createAndSendDirectoryDeleteRequest(Socket socket, FileSystemEvent fileSystemEvent)
            throws IOException {
        // TODO Auto-generated method stub
        JSONObject dirDeleteRequest = new JSONObject();
        dirDeleteRequest.put("command", "DIRECTORY_DELETE_REQUEST");
        dirDeleteRequest.put("pathName", fileSystemEvent.pathName);
        sendMessage(dirDeleteRequest.toJSONString() + "\n", socket);
    }

    public static void createAndSendInvalidProtocol(String message, Socket socket) throws IOException {
        // TODO Auto-generated method stub
        JSONObject invalidProtocol = new JSONObject();
        invalidProtocol.put("command", "INVALID_PROTOCOL");
        invalidProtocol.put("message", message);
        sendMessage(invalidProtocol.toJSONString() + "\n", socket);
    }


    public static boolean checkClientValidation(String ClientId) {
        boolean isValid = false;
        String publickeys = Configuration.getConfigurationValue("publickey");
        String[] publicKeyList = publickeys.split(";");
        for (String pk:publicKeyList) {
            String[] RSAcomponent = pk.split(" ");
            String EncodedPubKey = RSAcomponent[1];
            String identity = RSAcomponent[2];
            log.info("check Id："+identity);
            if (identity.compareTo(ClientId)== 0){
                isValid = true;
                break;
            }
        }
        return isValid;
    }

    public static byte[] EncodeSecretKey(String ClientId, String SecretKey)throws NoSuchAlgorithmException,InvalidKeySpecException,Exception{
        String publickeys = Configuration.getConfigurationValue("publickey");
        //TODO:if there are a list of public key,need a loop to find the correct one
        String validPublicKey = "";
        String[] publicKeyList = publickeys.split(";");
        for (String pk:publicKeyList) {
            String[] RSAcomponent = pk.split(" ");
            String EncodedPubKey = RSAcomponent[1];
            String identity = RSAcomponent[2];
            if (identity.compareTo(ClientId)== 0){
               validPublicKey = EncodedPubKey;
                break;
            }
        }
        //TODO:use client's publickey to encrypt the secret key and send back to the client
        RSAPublicKey key = decodePublicKey(Base64.getDecoder().decode(validPublicKey.getBytes()));
        //encode SecretKey by client's public key
        byte[] KeyAfterEncoded = RSAencryption(SecretKey,key);
        return KeyAfterEncoded;
    }

    private static int decodeUint32(byte[] key, int start_index){
        byte[] test = Arrays.copyOfRange(key, start_index, start_index + 4);
        return new BigInteger(test).intValue();
    }

    private static RSAPublicKey decodePublicKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] sshrsa = new byte[]{0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a'};
        int start_index = sshrsa.length;
        /* Decode the public exponent */
        int len = decodeUint32(key, start_index);
        start_index += 4;
        byte[] pe_b = new byte[len];
        for (int i = 0; i < len; i++) {
            pe_b[i] = key[start_index++];
        }
        BigInteger pe = new BigInteger(pe_b);
        /* Decode the modulus */
        len = decodeUint32(key, start_index);
        start_index += 4;
        byte[] md_b = new byte[len];
        for (int i = 0; i < len; i++) {
            md_b[i] = key[start_index++];
        }
        BigInteger md = new BigInteger(md_b);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec ks = new RSAPublicKeySpec(md, pe);
        return (RSAPublicKey) keyFactory.generatePublic(ks);
    }

    public static PrivateKey decodePrivateKey(String filePath){
        String EncodePrivateKey = "";
        String line = null;
        BufferedReader in;
        try{
            //read content from file
            in = new BufferedReader(new FileReader(filePath));
            while ((line = in.readLine())!=null){
                if (!line.contains("---")){
                    EncodePrivateKey += line;
                }
            }
            byte[] DecodePrivateKey = Base64.getDecoder().decode(EncodePrivateKey);
            DerInputStream dis = new DerInputStream(DecodePrivateKey);
            //9 section in the private key
            DerValue[] ders = dis.getSequence(9);
            //read RSA info
            int version = ders[0].getBigInteger().intValue();
            BigInteger modules = ders[1].getBigInteger();
            BigInteger publicExponent = ders[2].getBigInteger();
            BigInteger privateExponent = ders[3].getBigInteger();
            BigInteger prime1 = ders[4].getBigInteger();
            BigInteger prime2 = ders[5].getBigInteger();
            BigInteger exponent1 = ders[6].getBigInteger();
            BigInteger exponent2 = ders[7].getBigInteger();
            BigInteger crtCoefficient = ders[8].getBigInteger();
            //generate key
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modules,publicExponent);
            PublicKey publicKey = keyFactory.generatePublic(rsaPublicKeySpec);
            RSAPrivateCrtKeySpec rsaPrivateKeySpec = new RSAPrivateCrtKeySpec(modules,publicExponent,privateExponent,prime1,prime2,exponent1,exponent2,crtCoefficient);
            PrivateKey privateKey = keyFactory.generatePrivate(rsaPrivateKeySpec);

            return privateKey;
        }catch (InvalidKeySpecException invalidK){
            invalidK.getMessage();
        }
        catch(NoSuchAlgorithmException no){
            no.getMessage();
        }
        catch(IOException e){
            e.getMessage();
        }
        return null;
    }

    public static byte[] RSAencryption(String msg,RSAPublicKey key)throws Exception{
        byte[] content = msg.getBytes();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] bytes = cipher.doFinal(content);
        //System.out.println(msg.length()+" length of encrpted bytes "+content.length+"  after encrypted "+bytes.length+" string "+new String(bytes).length());
        return bytes;
    }


    public static byte[] RSAdecryption(byte[] encrypedMsg, PrivateKey key)throws Exception{
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int BLOCK_SIZE = 256;
        //byte[] content = encrypedMsg.getBytes();
        byte[] content = encrypedMsg;
        byte[] bytes;
        int msg_length = content.length;
        int index = 0;
        while (msg_length - index > 0){
            //need to be segemented
            if (msg_length - index > BLOCK_SIZE){
                bytes = cipher.doFinal(content,index,BLOCK_SIZE);
            }else
                bytes = cipher.doFinal(content,index,msg_length-index);
            out.write(bytes,0,bytes.length);
            index += BLOCK_SIZE;
        }
        byte[] finalmsg = out.toByteArray();
        out.close();
        //System.out.println(bytes);
        return finalmsg;
    }

    public static String GenerateKey()throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        SecretKey sk = kg.generateKey();
        byte[] b = sk.getEncoded();
        //System.out.println("genereateKey : "+new String(b));
        return parseByte2HexStr(b);
    }

    private static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }




    public static void AuthResponse(boolean isValid, byte[] key, Socket socket)throws IOException{
        Document AuthResp = new Document();
        AuthResp.append("command","AUTH_RESPONSE");
        boolean status = isValid?true:false;
        AuthResp.append("status",status);
        String msg = isValid?"public key found":"public key not found";
        AuthResp.append("message",msg);
        if (isValid)
            AuthResp.append("AES128",Base64.getEncoder().encodeToString(key));
        sendMessage(AuthResp.toJson()+"\n",socket);
    }

    public static String AESencryption(String plainText,byte[] key) {
        //System.out.println("key length is "+key.length);
        int length = plainText.getBytes().length;
        int remainder = length%16;

        String padding = "thisisusedforpad";
        plainText += padding.substring(0,16-remainder);
        //System.out.println(plainText.length() +" "+plainText.getBytes().length);
        String encryptedText = "";
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NOPADDING");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE,secretKey);
            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF8"));
            Base64.Encoder encoder = Base64.getEncoder();
            encryptedText = encoder.encodeToString(cipherText);

        } catch (Exception E) {
            log.info("Encrypt Exception : " + E.getMessage());
            //System.err.println("Encrypt Exception : " + E.getMessage());
        }
        return encryptedText;
    }

    public static String AESdecryption(String encryptedText,byte[] key) {
        String decryptedText = "";
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NOPADDING");
            //Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE,secretKey);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] cipherText = decoder.decode(encryptedText.getBytes("UTF8"));
            String decryptedStr = new String(cipher.doFinal(cipherText), "UTF-8");
            decryptedText = decryptedStr.substring(0,decryptedStr.indexOf("\n"));

        } catch (Exception E) {
            log.info("decrypt Exception : " + E.getMessage());
            //System.err.
            // ("decrypt Exception : " + E.getMessage());
        }
        return decryptedText;
    }

}