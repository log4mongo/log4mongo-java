/*
 * Copyright (C) 2009 Peter Monks (pmonks@gmail.com)
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

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.PropertyConfigurator;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * JUnit unit tests for ExtendedMongoDbAppender.
 * <p/>
 * Note: these tests require that a MongoDB server is running, and (by default)
 * assumes that server is listening on the default port (27017) on localhost.
 *
 * @author Mick Knutson (http://www.baselogic.com)
 */
public class TestExtendedMongoDbAppender {
    private final static Logger log = Logger.getLogger(TestExtendedMongoDbAppender.class);

    private final static String TEST_MONGO_SERVER_HOSTNAME = "localhost";
    private final static int TEST_MONGO_SERVER_PORT = 27017;
    private final static String TEST_DATABASE_NAME = "log4mongotest";
    private final static String TEST_COLLECTION_NAME = "logevents";

    private final static String MONGODB_APPENDER_NAME = "MongoDB";

    private final static String LOG4J_PROPS = "src/test/resources/log4j_extended.properties";
    //private final static String LOG4J_PROPS = "src/test/resources/log4j.xml";

    private final Mongo mongo;
    private final ExtendedMongoDbAppender appender;
    private DBCollection collection;

    public TestExtendedMongoDbAppender() throws Exception {
        PropertyConfigurator.configure(LOG4J_PROPS);
        mongo = new Mongo(TEST_MONGO_SERVER_HOSTNAME, TEST_MONGO_SERVER_PORT);
        appender = (ExtendedMongoDbAppender) Logger.getRootLogger().getAppender(
                MONGODB_APPENDER_NAME);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Mongo mongo = new Mongo(TEST_MONGO_SERVER_HOSTNAME,
                TEST_MONGO_SERVER_PORT);
        mongo.dropDatabase(TEST_DATABASE_NAME);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        Mongo mongo = new Mongo(TEST_MONGO_SERVER_HOSTNAME,
                TEST_MONGO_SERVER_PORT);
        mongo.dropDatabase(TEST_DATABASE_NAME);
    }

    @Before
    public void setUp() throws Exception {
        // Ensure both the appender and the JUnit test use the same collection
        // object - provides consistency across reads (JUnit) & writes (Log4J)
        collection = mongo.getDB(TEST_DATABASE_NAME).getCollection(
                TEST_COLLECTION_NAME);
        collection.drop();
        appender.setCollection(collection);

        mongo.getDB(TEST_DATABASE_NAME).requestStart();
    }

    @After
    public void tearDown() throws Exception {
        mongo.getDB(TEST_DATABASE_NAME).requestDone();
    }

    @Test
    public void testInitialized() throws Exception {
        if (!appender.isInitialized())
            fail();
    }

    @Test
    public void testRootLevelProperties() throws Exception {
        assertEquals(0L, countLogEntries());

        Map<String, String> obj = new HashMap<String, String>() {
            //Unnamed Block.
            {
                put("key1", "value1");
                put("key2", "value2");
            }
        };

        //slf4j style: log.warn("Testing Object in Message: {}", obj);
        log.warn("Testing Object in Message: " + obj);
        assertEquals(1L, countLogEntries());
        assertEquals(1L, countLogEntriesAtLevel("WARN"));

        // verify log entry content
        DBObject entry = collection.findOne();
        assertNotNull(entry);
        assertEquals("WARN", entry.get("level"));
        assertEquals("Testing Object in Message: {key2=value2, key1=value1}", entry.get("message"));
    }

    @Test
    public void testObjectAsMessage() throws Exception {
        assertEquals(0L, countLogEntries());


        log.warn("Testing Object in Message");

        long appNameCount = countLogEntriesWhere(BasicDBObjectBuilder.start().add("applicationName", "MyProject").get());

        assertEquals(1L, appNameCount);

        // verify log entry content
        DBObject entry = collection.findOne();
        assertNotNull(entry);
        assertEquals("Development", entry.get("eventType"));
    }

    @Test
    public void testMdcProperties() throws Exception {
        assertEquals(0L, countLogEntries());

        MDC.put("uuid", "1000");
        MDC.put("recordAssociation", "xyz");

        log.warn("Testing MDC Properties");
        assertEquals(1L, countLogEntries());
        assertEquals(1L, countLogEntriesAtLevel("WARN"));

        // verify log entry content
        DBObject entry = collection.findOne();
        assertNotNull(entry);
        assertEquals("WARN", entry.get("level"));
        assertEquals("Testing MDC Properties", entry.get("message"));
        assertNotNull(entry.get("properties"));
        DBObject mdcProperties = (DBObject) entry.get("properties");

        assertNotNull(mdcProperties.get("uuid"));
        assertNotNull(mdcProperties.get("recordAssociation"));
    }

    //-----------------------------------------------------------------------//
    // Private methods
    //-----------------------------------------------------------------------//
    private long countLogEntries() {
        return (collection.getCount());
    }

    private long countLogEntriesAtLevel(final String level) {
        return (countLogEntriesWhere(BasicDBObjectBuilder.start().add("level",
                level.toUpperCase()).get()));
    }

    private long countLogEntriesWhere(final DBObject whereClause) {
        return collection.getCount(whereClause);
    }
}
