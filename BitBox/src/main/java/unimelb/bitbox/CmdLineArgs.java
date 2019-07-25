package unimelb.bitbox;


import org.kohsuke.args4j.Option;

public class CmdLineArgs {

    @Option(required = false, name = "-c", aliases = {"--command"}, usage = "command")
    private String command="";

    @Option(required = true, name = "-s",usage = "server HostPort")
    private String s_hostport;

    @Option(required = false, name = "-p", usage = "peer HostPort")
    private String p_hostport="";

    @Option(required = false, name = "-i", usage = "identify")
    private String identify="song@test";

    public String getCommand(){
        return command;
    }

    public String getServerHostport() {
        return s_hostport;
    }

    public String getPeerHostport() {
        return p_hostport;
    }

    public String getIdentify() { return identify; }

}
