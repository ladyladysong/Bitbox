package unimelb.bitbox;//


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class GetRefuseMsg {
    public static void getQueue(String jsonString, Queue<String> queue) {
        try {
            JSONObject rsp = (JSONObject) (new JSONParser().parse(jsonString));
            JSONArray MSG = (JSONArray) rsp.get("peers");
            for (int i = 0; i < MSG.size(); i++) {
                String peer = MSG.get(i).toString();
                JSONObject newpeer = (JSONObject) (new JSONParser().parse(peer));
                String remoteHostPort = newpeer.get("host").toString() + ":" + newpeer.get("port");
                queue.offer(remoteHostPort);
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}

