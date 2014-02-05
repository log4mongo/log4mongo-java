/*
 * Copyright (C) 2009 Robert Stewart (robert@wombatnation.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.log4mongo;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.wombatnation.privateer.Privateer;

/**
 * JUnit unit tests for MongoDbAppender to verify proper behavior when setting the hostname and
 * property properties.
 * 
 * <b>Note:</b> these tests require that a MongoDB server is running, and (by default) assumes that
 * server is listening on the default port (27017) on localhost.
 * <p>
 * Unless a replica set is configured with mongod instances listening on ports 27017 and 27018,
 * errors will be logged. If a host is not listening on port 27018, testOneHostNonDefaultPort() will
 * fail with an error.
 * 
 * To test on localhost with a replica set, do the following (either starting mongod instances in
 * separate terminals or start them with --fork):
 * <ul>
 * <li>$ mkdir -p /data/r0
 * <li>$ mkdir -p /data/r1
 * <li>$ mkdir -p /data/r2
 * <li>$ mongod --replSet foo --smallfiles --port 27017 --dbpath /data/r0
 * <li>$ mongod --replSet foo --smallfiles --port 27018 --dbpath /data/r1
 * <li>$ mongod --replSet foo --smallfiles --port 27019 --dbpath /data/r2
 * <li>$ mongo
 * <li>>config = {"_id": "foo", members:[{_id: 0, host: 'localhost:27017'},{_id: 1, host:
 * 'localhost:27018'},{_id: 2, host: 'localhost:27019', arbiterOnly: true}]}
 * <li>>rs.initiate(config)
 * <li>Then wait about a minute until replica set is established. You can run rs.status() and look
 * for direct confirmation.
 * </ul>
 * Since the unit tests create and drop databases several times, they run about twice as fast if
 * mongod is started with the --smallfiles argument.
 * 
 * @author Robert Stewart (robert@wombatnation.com)
 */
public class TestMongoDbAppenderHosts {
    private final static String TEST_MONGO_SERVER_HOSTNAME = "localhost";
    private final static int TEST_MONGO_SERVER_PORT = 27017;
    private final static String TEST_DATABASE_NAME = "log4mongotest";

    private final static String MONGODB_APPENDER_NAME = "MongoDB";

    private final Privateer p = new Privateer();

    @Test
    public void testOneHost() throws Exception {
        String hostname = TEST_MONGO_SERVER_HOSTNAME;
        PropertyConfigurator.configure(getDefaultPortProperties(hostname));

        MongoDbAppender appender = (MongoDbAppender) Logger.getRootLogger().getAppender(
                MONGODB_APPENDER_NAME);
        MongoClient mongo = (MongoClient) p.getField(appender, "mongo");
        assertTrue(mongo.getAddress() != null);
        assertTrue(TEST_MONGO_SERVER_HOSTNAME.equals(mongo.getAddress().getHost()));
        assertTrue(TEST_MONGO_SERVER_PORT == mongo.getAddress().getPort());

        appender.close();
    }

    @Test
    public void testOneHostNonDefaultPort() throws Exception {
        String hostname = TEST_MONGO_SERVER_HOSTNAME;
        String port = "27018";
        int portNum = Integer.parseInt(port);
        PropertyConfigurator.configure(getNonDefaultPortProperties(hostname, port));

        MongoDbAppender appender = (MongoDbAppender) Logger.getRootLogger().getAppender(
                MONGODB_APPENDER_NAME);

        MongoClient mongo = (MongoClient) p.getField(appender, "mongo");
        assertTrue(mongo.getAddress() != null);
        assertTrue(TEST_MONGO_SERVER_HOSTNAME.equals(mongo.getAddress().getHost()));
        assertTrue(portNum == mongo.getAddress().getPort());

        appender.close();
    }

    /**
     * If this test is run without a mongod running on localhost port 27018, an error will be logged
     * to the console by the appender.
     * 
     * @throws Exception
     */
    @Test
    public void testTwoHostsTwoPorts() throws Exception {
        String hostname = "localhost localhost";
        List<String> hosts = Arrays.asList("localhost", "localhost");
        String port = "27017 27018";
        List<String> ports = Arrays.asList("27017", "27018");
        PropertyConfigurator.configure(getNonDefaultPortProperties(hostname, port));

        MongoDbAppender appender = (MongoDbAppender) Logger.getRootLogger().getAppender(
                MONGODB_APPENDER_NAME);

        MongoClient mongo = (MongoClient) p.getField(appender, "mongo");
        List<ServerAddress> addresses = mongo.getAllAddress();
        assertTrue(addresses != null);
        for (ServerAddress address : addresses) {
            boolean found = false;
            int i = 0;
            for (String host : hosts) {
                if (host.equals(address.getHost())) {
                    String p = String.valueOf(address.getPort());
                    if (ports.get(i).equals(p)) {
                        found = true;
                        break;
                    }
                }
                i++;
            }
            assertTrue(found);
        }

        appender.close();
    }

    private Properties getDefaultPortProperties(String hostname) {
        Properties props = new Properties();
        props.put("log4j.rootLogger", "DEBUG, MongoDB");
        props.put("log4j.appender.MongoDB", "org.log4mongo.MongoDbAppender");
        props.put("log4j.appender.MongoDB.databaseName", TEST_DATABASE_NAME);
        props.put("log4j.appender.MongoDB.hostname", hostname);
        return props;
    }

    private Properties getNonDefaultPortProperties(String hostname, String port) {
        Properties props = new Properties();
        props.put("log4j.rootLogger", "DEBUG, MongoDB");
        props.put("log4j.appender.MongoDB", "org.log4mongo.MongoDbAppender");
        props.put("log4j.appender.MongoDB.databaseName", TEST_DATABASE_NAME);
        props.put("log4j.appender.MongoDB.hostname", hostname);
        props.put("log4j.appender.MongoDB.port", port);
        return props;
    }
}
