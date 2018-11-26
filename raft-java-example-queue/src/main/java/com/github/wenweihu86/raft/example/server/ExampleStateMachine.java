package com.github.wenweihu86.raft.example.server;

import com.github.wenweihu86.raft.StateMachine;
import com.github.wenweihu86.raft.example.server.service.ExampleMessage;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by wenweihu86 on 2017/5/9.
 */
public class ExampleStateMachine implements StateMachine
{

    private static final Logger LOG = LoggerFactory.getLogger(ExampleStateMachine.class);

    private static String fileName = "messages.data";

    private List<ExampleMessage.AppendRequest> messages = Lists.newArrayList();
    private String raftDataDir;

    public ExampleStateMachine(String raftDataDir)
    {
        this.raftDataDir = raftDataDir;
    }

    @Override
    public void writeSnapshot(String snapshotDir)
    {

        try (FileWriter writer = new FileWriter(Paths.get(raftDataDir, fileName).toFile()))
        {
            for (ExampleMessage.AppendRequest message : messages)
            {
                writer.append(String.format("%d,%s\n", message.getId(), message.getContent()));
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            LOG.warn("writeSnapshot meet exception, dir={}, msg={}", snapshotDir, ex.getMessage());
        }
    }

    @Override
    public void readSnapshot(String snapshotDir)
    {

        messages.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(Paths.get(raftDataDir, fileName).toFile())))
        {
            do
            {
                String message = reader.readLine();
                if (message == null)
                {
                    break;
                }
                List<String> data = Splitter.on(',').splitToList(message);
                Preconditions.checkElementIndex(2, data.size());
                ExampleMessage.AppendRequest request = ExampleMessage.AppendRequest.newBuilder()
                        .setId(Integer.parseInt(data.get(0))).setContent(data.get(1)).build();
                messages.add(request);
            } while (true);
        } catch (Exception ex)
        {
            LOG.warn("meet exception, msg={}", ex.getMessage());
        }
    }

    @Override
    public void apply(byte[] dataBytes)
    {
        try
        {
            ExampleMessage.AppendRequest request = ExampleMessage.AppendRequest.parseFrom(dataBytes);
            Preconditions.checkArgument(
                    messages.isEmpty() || messages.get(messages.size() - 1).getId() < request.getId(),
                    "unordered messages");
            messages.add(request);
        } catch (Exception ex)
        {
            LOG.warn("meet exception, msg={}", ex.getMessage());
        }
    }

    public ExampleMessage.GetLastIdResponse getLastId(ExampleMessage.GetLastIdRequest request)
    {
        try
        {
            ExampleMessage.GetLastIdResponse.Builder responseBuilder = ExampleMessage.GetLastIdResponse.newBuilder();
            if (messages.isEmpty())
            {
                responseBuilder.setId(-1);
            } else
            {
                responseBuilder.setId(messages.get(messages.size() - 1).getId());
            }
            ExampleMessage.GetLastIdResponse response = responseBuilder.build();
            return response;
        } catch (Exception ex)
        {
            LOG.warn("read data error, msg={}", ex.getMessage());
            return null;
        }
    }

}
