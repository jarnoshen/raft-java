package com.github.wenweihu86.raft.example.server.service;

/**
 * Created by wenweihu86 on 2017/5/9.
 */
public interface ExampleService
{
    ExampleMessage.AppendResponse append(ExampleMessage.AppendRequest request);

    ExampleMessage.GetLastIdResponse getLastId(ExampleMessage.GetLastIdRequest request);
}
