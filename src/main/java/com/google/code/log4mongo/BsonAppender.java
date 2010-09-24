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
 *
 */

package com.google.code.log4mongo;


import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Category;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Logger;
import org.apache.log4j.or.ObjectRenderer;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.bson.BSON;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


/**
 * Log4J Appender that writes log events with bson format
 * 
 * An example BSON structure for a single log entry is as follows:
 * 
 * <pre>
 * {
 *   "_id"        : ObjectId("f1c0895fd5eee04a445deb00"),
 *   "timestamp"  : "Thu Oct 22 2009 16:46:29 GMT-0700 (Pacific Daylight Time)",
 *   "level"      : "ERROR",
 *   "thread"     : "main",
 *   "message"    : "Error entry",
 *   "fileName"   : "TestMongoDbAppender.java",
 *   "method"     : "testLogWithChainedExceptions",
 *   "lineNumber" : "147",
 *   "loggerName" : {
 *                    "fullyQualifiedClassName" : "com.google.code.log4mongo.TestMongoDbAppender",
 *                    "package"                 : [ "com", "google", "code", "log4mongo" ],
 *                    "className"               : "TestMongoDbAppender"
 *                  },
 *   "class"      : {
 *                    "fullyQualifiedClassName" : "com.google.code.log4mongo.TestMongoDbAppender",
 *                    "package"                 : [ "com", "google", "code", "log4mongo" ],
 *                    "className"               : "TestMongoDbAppender"
 *                  },
 *   "throwables" : [
 *                    {
 *                      "message"    : "I'm an innocent bystander.",
 *                      "stackTrace" : [
 *                                       {
 *                                         "fileName"   : "TestMongoDbAppender.java",
 *                                         "method"     : "testLogWithChainedExceptions",
 *                                         "lineNumber" : 147,
 *                                         "class"      : {
 *                                                          "fullyQualifiedClassName" : "com.google.code.log4mongo.TestMongoDbAppender",
 *                                                          "package"                 : [ "com", "google", "code", "log4mongo" ],
 *                                                          "className"               : "TestMongoDbAppender"
 *                                                        }
 *                                       },
 *                                       {
 *                                         "method"     : "invoke0",
 *                                         "lineNumber" : -2,
 *                                         "class"      : {
 *                                                          "fullyQualifiedClassName" : "sun.reflect.NativeMethodAccessorImpl",
 *                                                          "package"                 : [ "sun", "reflect" ],
 *                                                          "className"               : "NativeMethodAccessorImpl"
 *                                                        }
 *                                       },
 *                                       ... 8< ...
 *                                     ]
 *                    },
 *                    {
 *                      "message" : "I'm the real culprit!",
 *                      "stackTrace" : [
 *                                       {
 *                                         "fileName" : "TestMongoDbAppender.java",
 *                                         "method" : "testLogWithChainedExceptions",
 *                                         "lineNumber" : 145,
 *                                         "class" : {
 *                                                     "fullyQualifiedClassName" : "com.google.code.log4mongo.TestMongoDbAppender",
 *                                                     "package"                 : [ "com", "google", "code", "log4mongo" ],
 *                                                     "className"               : "TestMongoDbAppender"
 *                                                   }
 *                                       },
 *                                       ... 8< ...
 *                                     ]
 *                    }
 *                  ]
 * }
 * </pre>
 *
 * @author Peter Monks (pmonks@gmail.com)
 * @modify Gabriel Eisbruch (gabrieleisbruch@gmail.com)
 * @see http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Appender.html
 * @see http://www.mongodb.org/
 * @version $Id$
 */
public abstract class BsonAppender
    extends AppenderSkeleton
{
	
	LogEventBsonifier bsonifier = new LogEventBsonifierImpl();
    /**
     * @see org.apache.log4j.Appender#requiresLayout()
     */
    public boolean requiresLayout()
    {
        return(false);
    }

    
    /**
     * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append(final LoggingEvent loggingEvent)
    {
    	
        DBObject bson = bsonifier.bsonify(loggingEvent);
        /*
         * Call the append method with the bson object 
         */
        append(bson);
    }

    /**
     * This append implementation call with a processed formated bson object 
     * @param bson
     */
	protected abstract void append(DBObject bson);


	public LogEventBsonifier getBsonifier() {
		return bsonifier;
	}


	public void setBsonifier(LogEventBsonifier bsonifier) {
		this.bsonifier = bsonifier;
	}
    
	
    
}
