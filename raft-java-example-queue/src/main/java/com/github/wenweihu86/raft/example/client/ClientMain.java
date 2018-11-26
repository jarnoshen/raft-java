package com.github.wenweihu86.raft.example.client;

import com.github.wenweihu86.raft.example.server.service.ExampleMessage;
import com.github.wenweihu86.raft.example.server.service.ExampleService;
import com.github.wenweihu86.rpc.client.RPCClient;
import com.github.wenweihu86.rpc.client.RPCProxy;

/**
 * Created by wenweihu86 on 2017/5/14.
 */
public class ClientMain
{
    public static void main(String[] args)
    {
        if (args.length < 2)
        {
            System.out.printf("Usage: ./run_server.sh CLUSTER MESSAGE\n");
            System.exit(-1);
        }

        // parse args
        String ipPorts = args[0];
        String message = args[1];

        // init rpc client
        RPCClient rpcClient = new RPCClient(ipPorts);
        ExampleService exampleService = RPCProxy.getProxy(rpcClient, ExampleService.class);

        // get new id
        ExampleMessage.GetLastIdRequest getLastIdRequest = ExampleMessage.GetLastIdRequest.newBuilder().build();
        ExampleMessage.GetLastIdResponse getLastIdResponse = exampleService.getLastId(getLastIdRequest);

        // generate and store message
        ExampleMessage.AppendRequest appendRequest = ExampleMessage.AppendRequest.newBuilder()
                .setId(getLastIdResponse.getId() + 1).setContent(message).build();
        ExampleMessage.AppendResponse appendResponse = exampleService.append(appendRequest);
        System.out.println("store message result: " + appendResponse.getSuccess());

        rpcClient.stop();
    }
}
