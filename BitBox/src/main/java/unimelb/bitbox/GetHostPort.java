package unimelb.bitbox;//

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GetHostPort {
    public static String getHostPort(String msg){
        try {
            JSONObject new_msg=(JSONObject)(new JSONParser().parse(msg));
            String remoteHostPort= new_msg.get("hostPort").toString();
            JSONObject peesHost=(JSONObject)(new JSONParser().parse(remoteHostPort));
            String hostAndport=peesHost.get("host").toString()+":"+peesHost.get("port").toString();
            return hostAndport;

        } catch (ParseException e) {
            e.printStackTrace();
            return "error";
        }
    }
}
