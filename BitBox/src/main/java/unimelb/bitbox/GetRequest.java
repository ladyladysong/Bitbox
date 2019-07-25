package unimelb.bitbox;//

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unimelb.bitbox.util.Document;

public class GetRequest {
    public static String getDirCreateRQ(String msg) throws ParseException {
        JSONObject rsp = (JSONObject)(new JSONParser().parse(msg));
        String pathName = rsp.get("pathName").toString();
        JSONObject request = new JSONObject();
        request.put("command","DIRECTORY_CREATE_REQUEST");
        request.put("pathName",pathName);
        String orgRequest= request.toJSONString()+"\n";
        return orgRequest;
    }
    public static String getDirDeleteRQ(String msg) throws ParseException {
        JSONObject rsp = (JSONObject)(new JSONParser().parse(msg));
        String pathName = rsp.get("pathName").toString();
        JSONObject request = new JSONObject();
        request.put("command","DIRECTORY_DELETE_REQUEST");
        request.put("pathName",pathName);
        String orgRequest= request.toJSONString()+"\n";
        return orgRequest;
    }
    public static String getFileCreateRQ(String msg) throws ParseException {
        Document Response = Document.parse(msg);
        Document file_desc = (Document) Response.get("fileDescriptor");
        String pathName = Response.getString("pathName");
        Document request = new Document();
        request.append("command", "FILE_CREATE_REQUEST");
        request.append("fileDescriptor", file_desc);
        request.append("pathName", pathName);
        String orgRequest= request.toJson()+"\n";
        System.out.println("FCRQ IS "+orgRequest);
        return orgRequest;
    }
    public static String getFileDeleteRQ(String msg) throws ParseException {
        Document Response = Document.parse(msg);
        Document file_desc = (Document) Response.get("fileDescriptor");
        String pathName = Response.getString("pathName");
        Document request = new Document();
        request.append("command", "FILE_DELETE_REQUEST");
        request.append("fileDescriptor", file_desc);
        request.append("pathName", pathName);
        String orgRequest= request.toJson()+"\n";
        System.out.println("DELETE IS "+orgRequest);
        return orgRequest;
    }
    public static String getFileModifyRQ(String msg) throws ParseException {
        Document Response = Document.parse(msg);
        Document file_desc = (Document) Response.get("fileDescriptor");
        String pathName = Response.getString("pathName");
        Document request = new Document();
        request.append("command", "FILE_MODIFY_REQUEST");
        request.append("fileDescriptor", file_desc);
        request.append("pathName", pathName);
        String orgRequest= request.toJson()+"\n";
        System.out.println("Modify IS "+orgRequest);
        return orgRequest;
    }
    public static String getFileByteRQ(String msg) throws ParseException {
        Document fileBytesResponse = Document.parse(msg);
        Document file_desc = (Document) fileBytesResponse.get("fileDescriptor");
        String pathName = fileBytesResponse.getString("pathName");
        long pos = fileBytesResponse.getLong("position");
        long length = fileBytesResponse.getLong("length");
        Document BytesCreateRequest = new Document();
        BytesCreateRequest.append("command", "FILE_BYTES_REQUEST");
        BytesCreateRequest.append("position",pos);
        BytesCreateRequest.append("length",length);
        BytesCreateRequest.append("fileDescriptor", file_desc);
        BytesCreateRequest.append("pathName", pathName);
        String orgRequest= BytesCreateRequest.toJson()+"\n";
        System.out.println(orgRequest);
        return orgRequest;


        //JSONObject rsp = (JSONObject)(new JSONParser().parse(msg));
        //String pathName = rsp.get("pathName").toString();
        //long position = (long) rsp.get("position");
        //long length = (long) rsp.get("length");
        //String fileDescriptor = rsp.get("fileDescriptor").toString();
        //System.out.println(fileDescriptor);
        //JSONObject request = new JSONObject();
        //request.put("command","FILE_BYTES_REQUEST");
        //request.put("fileDescriptor",fileDescriptor);
        //request.put("pathName",pathName);
        //request.put("position",position);
        //request.put("length",length);
        //String orgRequest= request.toJSONString()+"\n";

    }

}
