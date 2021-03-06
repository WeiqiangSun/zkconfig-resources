package com.github.phantomthief.zookeeper;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.curator.utils.ZKPaths.makePath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.transaction.CuratorTransactionResult;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2017-01-04.
 */
class TransactionTest {

    private static final String PARENT_PATH = "/test1";
    private static TestingServer testingServer;
    private static CuratorFramework curatorFramework;
    private static CuratorFramework curatorFramework2;

    @BeforeAll
    static void init() throws Exception {
        testingServer = new TestingServer(true);
        curatorFramework = CuratorFrameworkFactory.newClient(testingServer.getConnectString(),
                new ExponentialBackoffRetry(10000, 20));
        curatorFramework.start();
        curatorFramework2 = CuratorFrameworkFactory.newClient(testingServer.getConnectString(),
                new ExponentialBackoffRetry(10000, 20));
        curatorFramework2.start();
    }

    @Test
    void test() throws Exception {
        curatorFramework.create().forPath(PARENT_PATH);
        String node1 = makePath(PARENT_PATH, "node1");
        String node2 = makePath(PARENT_PATH, "node2");
        curatorFramework.create().forPath(node1);
        ZkBasedTreeNodeResource<List<String>> zkNode = ZkBasedTreeNodeResource
                .<List<String>> newBuilder() //
                .curator(curatorFramework2) //
                .path(PARENT_PATH) //
                .keysFactoryEx(this::factory) //
                .build();
        System.out.println("first access:" + zkNode.get());

        Collection<CuratorTransactionResult> commit = curatorFramework.inTransaction() //
                .delete().forPath(node1) //
                .and().create().forPath(node1, "1".getBytes()) //
                .and().create().forPath(node2, "2".getBytes()) //
                .and().commit();
        System.out.println("result:" + commit);
        sleepUninterruptibly(1, SECONDS);
        System.out.println("second access:" + zkNode.get());
    }

    private List<String> factory(Collection<String> collection) {
        ArrayList<String> strings = new ArrayList<>(collection);
        System.out.println("build:" + strings);
        return strings;
    }
}
