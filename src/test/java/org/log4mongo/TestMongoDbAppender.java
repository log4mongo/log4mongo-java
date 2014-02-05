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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

/**
 * JUnit unit tests for MongoDbAppender.
 * 
 * Note: these tests require that a MongoDB server is running, and (by default) assumes that server
 * is listening on the default port (27017) on localhost.
 * 
 * @author Peter Monks (pmonks@gmail.com)
 */
public class TestMongoDbAppender {
    private final static Logger log = Logger.getLogger(TestMongoDbAppender.class);

    private final static String TEST_MONGO_SERVER_HOSTNAME = "localhost";
    private final static int TEST_MONGO_SERVER_PORT = 27017;
    private final static String TEST_DATABASE_NAME = "log4mongotest";
    private final static String TEST_COLLECTION_NAME = "logevents";

    private final static String MONGODB_APPENDER_NAME = "MongoDB";

    private final static String LOG4J_PROPS = "src/test/resources/log4j.properties";

    private final MongoClient mongo;
    private final MongoDbAppender appender;
    private DBCollection collection;

    public TestMongoDbAppender() throws Exception {
        PropertyConfigurator.configure(LOG4J_PROPS);
        mongo = new MongoClient(TEST_MONGO_SERVER_HOSTNAME, TEST_MONGO_SERVER_PORT);
        appender = (MongoDbAppender) Logger.getRootLogger().getAppender(MONGODB_APPENDER_NAME);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        MongoClient mongo = new MongoClient(TEST_MONGO_SERVER_HOSTNAME, TEST_MONGO_SERVER_PORT);
        mongo.dropDatabase(TEST_DATABASE_NAME);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        MongoClient mongo = new MongoClient(TEST_MONGO_SERVER_HOSTNAME, TEST_MONGO_SERVER_PORT);
        mongo.dropDatabase(TEST_DATABASE_NAME);
    }

    @Before
    public void setUp() throws Exception {
        // Ensure both the appender and the JUnit test use the same collection
        // object - provides consistency across reads (JUnit) & writes (Log4J)
        collection = mongo.getDB(TEST_DATABASE_NAME).getCollection(TEST_COLLECTION_NAME);
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
    public void testSingleLogEntry() throws Exception {
        log.trace("Trace entry");

        assertEquals(1L, countLogEntries());
        assertEquals(1L, countLogEntriesAtLevel("trace"));
        assertEquals(0L, countLogEntriesAtLevel("debug"));
        assertEquals(0L, countLogEntriesAtLevel("info"));
        assertEquals(0L, countLogEntriesAtLevel("warn"));
        assertEquals(0L, countLogEntriesAtLevel("error"));
        assertEquals(0L, countLogEntriesAtLevel("fatal"));

        // verify log entry content
        DBObject entry = collection.findOne();
        assertNotNull(entry);
        assertEquals("TRACE", entry.get("level"));
        assertEquals("Trace entry", entry.get("message"));
    }

    @Test
    public void testTimestampStoredNatively() throws Exception {
        log.debug("Debug entry");

        assertEquals(1L, countLogEntries());

        // verify timestamp - presence and data type
        DBObject entry = collection.findOne();
        assertNotNull(entry);
        assertTrue("Timestamp is not present in logged entry", entry.containsField("timestamp"));
        assertTrue("Timestamp of logged entry is not stored as native date",
                (entry.get("timestamp") instanceof java.util.Date));
    }

    @Test
    public void testAllLevels() throws Exception {
        log.trace("Trace entry");
        log.debug("Debug entry");
        log.info("Info entry");
        log.warn("Warn entry");
        log.error("Error entry");
        log.fatal("Fatal entry");

        assertEquals(6L, countLogEntries());
        assertEquals(1L, countLogEntriesAtLevel("trace"));
        assertEquals(1L, countLogEntriesAtLevel("debug"));
        assertEquals(1L, countLogEntriesAtLevel("info"));
        assertEquals(1L, countLogEntriesAtLevel("warn"));
        assertEquals(1L, countLogEntriesAtLevel("error"));
        assertEquals(1L, countLogEntriesAtLevel("fatal"));
    }

    @Test
    public void testLogWithException() throws Exception {
        log.error("Error entry", new RuntimeException("Here is an exception!"));

        assertEquals(1L, countLogEntries());
        assertEquals(1L, countLogEntriesAtLevel("error"));

        // verify log entry content
        DBObject entry = collection.findOne();
        assertNotNull(entry);
        assertEquals("ERROR", entry.get("level"));
        assertEquals("Error entry", entry.get("message"));

        // verify throwable presence and content
        assertTrue("Throwable is not present in logged entry", entry.containsField("throwables"));
        BasicDBList throwables = (BasicDBList) entry.get("throwables");
        assertEquals(1, throwables.size());

        DBObject throwableEntry = (DBObject) throwables.get("0");
        assertTrue("Throwable message is not present in logged entry",
                throwableEntry.containsField("message"));
        assertEquals("Here is an exception!", throwableEntry.get("message"));
    }

    @Test
    public void testLogWithChainedExceptions() throws Exception {
        Exception rootCause = new RuntimeException("I'm the real culprit!");

        log.error("Error entry", new RuntimeException("I'm an innocent bystander.", rootCause));

        assertEquals(1L, countLogEntries());
        assertEquals(1L, countLogEntriesAtLevel("error"));

        // verify log entry content
        DBObject entry = collection.findOne();
        assertNotNull(entry);
        assertEquals("ERROR", entry.get("level"));
        assertEquals("Error entry", entry.get("message"));

        // verify throwable presence and content
        assertTrue("Throwable is not present in logged entry", entry.containsField("throwables"));
        BasicDBList throwables = (BasicDBList) entry.get("throwables");
        assertEquals(2, throwables.size());

        DBObject rootEntry = (DBObject) throwables.get("0");
        assertTrue("Throwable message is not present in logged entry",
                rootEntry.containsField("message"));
        assertEquals("I'm an innocent bystander.", rootEntry.get("message"));

        DBObject chainedEntry = (DBObject) throwables.get("1");
        assertTrue("Throwable message is not present in logged entry",
                chainedEntry.containsField("message"));
        assertEquals("I'm the real culprit!", chainedEntry.get("message"));
    }

    @Test
    public void testQuotesInMessage() {
        assertEquals(0L, countLogEntries());
        log.warn("Quotes\" \"embedded");
        assertEquals(1L, countLogEntries());
        assertEquals(1L, countLogEntriesAtLevel("WARN"));

        // verify log entry content
        DBObject entry = collection.findOne();
        assertNotNull(entry);
        assertEquals("WARN", entry.get("level"));
        assertEquals("Quotes\" \"embedded", entry.get("message"));
    }

    @Test
    public void testPerformance() throws Exception {
        // Log one event to minimize start up effects on performance
        log.warn("Warn entry");

        long NUM_MESSAGES = 1000;
        long now = System.currentTimeMillis();
        for (long i = 0; i < NUM_MESSAGES; i++) {
            log.warn("Warn entry");
        }
        long dur = System.currentTimeMillis() - now;
        System.out.println("Milliseconds for MongoDbAppender to log " + NUM_MESSAGES + " messages:"
                + dur);
        assertEquals(NUM_MESSAGES + 1, countLogEntries());
    }

    @Test
    public void testRegularLoggerRecordsLoggerNameCorrectly() {
        log.info("From an unwrapped logger");

        assertEquals(1, countLogEntries());
        assertEquals(1, countLogEntriesAtLevel("info"));
        assertEquals(
                1,
                countLogEntriesWhere(BasicDBObjectBuilder.start()
                        .add("loggerName.className", "TestMongoDbAppender").get()));
        assertEquals(
                1,
                countLogEntriesWhere(BasicDBObjectBuilder.start()
                        .add("class.className", "TestMongoDbAppender").get()));
    }

    @Test
    public void testWrappedLoggerRecordsLoggerNameCorrectly() {
        WrappedLogger wrapped = new WrappedLogger(log);
        wrapped.info("From a wrapped logger");

        assertEquals(1, countLogEntries());
        assertEquals(1, countLogEntriesAtLevel("info"));
        assertEquals(
                1,
                countLogEntriesWhere(BasicDBObjectBuilder.start()
                        .add("loggerName.className", "TestMongoDbAppender").get()));
        assertEquals(
                1,
                countLogEntriesWhere(BasicDBObjectBuilder.start()
                        .add("class.className", "WrappedLogger").get()));
    }

    @Test
    public void testHostInfoRecords() throws Exception {
        assertEquals(0L, countLogEntries());
        log.warn("Testing hostinfo");
        assertEquals(1L, countLogEntries());
        assertEquals(1L, countLogEntriesAtLevel("WARN"));

        // verify log entry content
        DBObject entry = collection.findOne();
        assertNotNull(entry);
        assertEquals("WARN", entry.get("level"));
        assertEquals("Testing hostinfo", entry.get("message"));
        assertNotNull(entry.get("host"));
        DBObject hostinfo = (DBObject) entry.get("host");
        assertNotNull(hostinfo.get("process"));
        assertEquals(InetAddress.getLocalHost().getHostName(), hostinfo.get("name"));
        assertEquals(ManagementFactory.getRuntimeMXBean().getName(), hostinfo.get("process"));
    }

    private long countLogEntries() {
        return (collection.getCount());
    }

    private long countLogEntriesAtLevel(final String level) {
        return (countLogEntriesWhere(BasicDBObjectBuilder.start().add("level", level.toUpperCase())
                .get()));
    }

    private long countLogEntriesWhere(final DBObject whereClause) {
        return collection.getCount(whereClause);
    }
}
