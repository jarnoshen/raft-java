package com.github.wenweihu86.raft.example.server.service.impl;

import com.github.wenweihu86.raft.example.server.ExampleStateMachine;
import com.github.wenweihu86.raft.example.server.service.ExampleMessage;
import com.github.wenweihu86.raft.example.server.service.ExampleService;
import com.github.wenweihu86.raft.RaftNode;
import com.github.wenweihu86.raft.proto.RaftMessage;
import com.github.wenweihu86.rpc.client.RPCClient;
import com.github.wenweihu86.rpc.client.RPCProxy;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wenweihu86 on 2017/5/9.
 */
public class ExampleServiceImpl implements ExampleService
{

    private static final Logger LOG = LoggerFactory.getLogger(ExampleServiceImpl.class);
    private static JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();

    private RaftNode raftNode;
    private ExampleStateMachine stateMachine;

    public ExampleServiceImpl(RaftNode raftNode, ExampleStateMachine stateMachine)
    {
        this.raftNode = raftNode;
        this.stateMachine = stateMachine;
    }

    @Override
    public ExampleMessage.AppendResponse append(ExampleMessage.AppendRequest request)
    {
        ExampleMessage.AppendResponse.Builder responseBuilder = ExampleMessage.AppendResponse.newBuilder();
        // 如果自己不是leader，将写请求转发给leader
        if (raftNode.getLeaderId() <= 0)
        {
            responseBuilder.setSuccess(false);
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId())
        {
            RPCClient rpcClient = raftNode.getPeerMap().get(raftNode.getLeaderId()).getRpcClient();
            ExampleService exampleService = RPCProxy.getProxy(rpcClient, ExampleService.class);
            ExampleMessage.AppendResponse responseFromLeader = exampleService.append(request);
            responseBuilder.mergeFrom(responseFromLeader);
        } else
        {
            // 数据同步写入raft集群
            byte[] data = request.toByteArray();
            boolean success = raftNode.replicate(data, RaftMessage.EntryType.ENTRY_TYPE_DATA);
            responseBuilder.setSuccess(success);
        }

        ExampleMessage.AppendResponse response = responseBuilder.build();
        try
        {
            LOG.info("append request, request={}, response={}", printer.print(request), printer.print(response));
        } catch (InvalidProtocolBufferException ex)
        {
            ex.printStackTrace();
        }
        return response;
    }

    @Override
    public ExampleMessage.GetLastIdResponse getLastId(ExampleMessage.GetLastIdRequest request)
    {
        ExampleMessage.GetLastIdResponse response = stateMachine.getLastId(request);
        try
        {
            LOG.info("get last id request, request={}, response={}", printer.print(request), printer.print(response));
        } catch (InvalidProtocolBufferException ex)
        {
            ex.printStackTrace();
        }
        return response;
    }
}
