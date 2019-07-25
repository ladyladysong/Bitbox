# Bitbox
A distributed file share system
proj1.pdf and proj2.pdf are two demonstration documents of this project.
All the configuration is shown in **configuration.properties**

## Attention
1. In UDP mode, the host should be written in IP format like "127.0.0.1" not "localhost".
2. In UDP mode, when receiving connection_refuse, the peer will wait timeout seconds to connect another peer.

# Explanation of the parameter setting
The parameter of Client.class is 
-c command
-s server host:port
-p (not required) the peer host:port
-i identity of the client (must be consistent with the identity in public key postfix, eg: song@test

*The Client.class reads the "bitboxclient_rsa" from the default path, which is predefined in this program, please ensure that "bitboxclient_rsa" is in the same path with configuration.*
