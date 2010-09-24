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
    	
        DBObject bson = bsonifyLoggingEvent(loggingEvent);
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
    
    
	  /**
     * BSONifies a single Log4J LoggingEvent object.
     * 
     * @param loggingEvent The LoggingEvent object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the LoggingEvent object <i>(may be null)</i>.
     */
	protected DBObject bsonifyLoggingEvent(final LoggingEvent loggingEvent)
    {
        DBObject result = null;

        if (loggingEvent != null)
        {
            result = new BasicDBObject();
            
            result.put("timestamp", new Date(loggingEvent.getTimeStamp()));
            nullSafePut(result, "level",   loggingEvent.getLevel().toString());
            nullSafePut(result, "thread",  loggingEvent.getThreadName());
            nullSafePut(result, "message", loggingEvent.getMessage());
            nullSafePut(result, "loggerName", bsonifyClassName(loggingEvent.getLoggerName()));
            
            addLocationInformation(result, loggingEvent.getLocationInformation());
            addThrowableInformation(result, loggingEvent.getThrowableInformation());
        }
        
        return(result);
    }
    
    /**
     * Adds the ThrowableInformation object to an existing BSON object.
     * 
     * @param bson          The BSON object to add the throwable info to <i>(must not be null)</i>.
     * @param throwableInfo The ThrowableInformation object to add to the BSON object <i>(may be null)</i>.
     */
	protected void addThrowableInformation(DBObject bson, final ThrowableInformation throwableInfo)
    {
        if (throwableInfo != null)
        {
            Throwable currentThrowable = throwableInfo.getThrowable();
            List      throwables       = new BasicDBList();
            
            while (currentThrowable != null)
            {
                DBObject throwableBson = bsonifyThrowable(currentThrowable);
                
                if (throwableBson != null)
                {
                    throwables.add(throwableBson);
                }
                
                currentThrowable = currentThrowable.getCause();
            }
            
            if (throwables.size() > 0)
            {
                bson.put("throwables", throwables);
            }
        }
    }
    

    /**
     * BSONifies the given Throwable.
     * 
     * @param throwable The throwable object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the Throwable object <i>(may be null)</i>.
     */
	protected DBObject bsonifyThrowable(final Throwable throwable)
    {
        DBObject result = null;
       
        if (throwable != null)
        {
            result = new BasicDBObject();
            
            nullSafePut(result, "message",    throwable.getMessage());
            nullSafePut(result, "stackTrace", bsonifyStackTrace(throwable.getStackTrace()));
        }
        
        return(result);
    }
    
    
    /**
     * BSONifies the given stack trace.
     * 
     * @param stackTrace The stack trace object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the stack trace object <i>(may be null)</i>.
     */
	protected DBObject bsonifyStackTrace(final StackTraceElement[] stackTrace)
    {
        BasicDBList result = null;
        
        if (stackTrace != null && stackTrace.length > 0)
        {
            result = new BasicDBList();
            
            for (StackTraceElement element : stackTrace)
            {
                DBObject bson = bsonifyStackTraceElement(element);
                
                if (bson != null)
                {
                    result.add(bson);
                }
            }
        }
        
        return(result);
    }
    
    
    
    /**
     * BSONifies the given stack trace element.
     * 
     * @param element The stack trace element object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the stack trace element object <i>(may be null)</i>.
     */
	protected DBObject bsonifyStackTraceElement(final StackTraceElement element)
    {
        DBObject result = null;
        
        if (element != null)
        {
            result = new BasicDBObject();
            
            nullSafePut(result, "fileName",   element.getFileName());
            nullSafePut(result, "method",     element.getMethodName());
            nullSafePut(result, "lineNumber", element.getLineNumber());
            nullSafePut(result, "class",      bsonifyClassName(element.getClassName()));
        }
        
        return(result);
    }
    
    
    /**
     * BSONifies the given class name.
     * 
     * @param className The class name to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the class name <i>(may be null)</i>.
     */
	protected DBObject bsonifyClassName(final String className)
    {
        DBObject result = null;
        
        if (className != null && className.trim().length() > 0)
        {
            result = new BasicDBObject();
            
            result.put("fullyQualifiedClassName", className);
            
            List     packageComponents   = new BasicDBList();
            String[] packageAndClassName = className.split("\\.");
            
            packageComponents.addAll(Arrays.asList(Arrays.copyOf(packageAndClassName, packageAndClassName.length - 1)));
            
            if (packageComponents.size() > 0)
            {
                result.put("package",   packageComponents);
            }
            
            result.put("className", packageAndClassName[packageAndClassName.length - 1]);
        }
        
        return(result);
    }


    /**
     * Adds the given value to the given key, except if it's null (in which case this method does nothing).
     * 
     * @param bson  The BSON object to add the key/value to <i>(must not be null)</i>.
     * @param key   The key of the object <i>(must not be null)</i>.
     * @param value The value of the object <i>(may be null)</i>.
     */
	protected void nullSafePut(DBObject bson, final String key, final Object value)
    {
        if (value != null)
        {
            if (value instanceof String)
            {
                String stringValue = (String)value;
                
                if (stringValue.trim().length() > 0)
                {
                    bson.put(key, stringValue);
                }
            }
            else
            {
                bson.put(key, value);
            }
        }
    }
    
    /**
     * Adds the LocationInfo object to an existing BSON object. 
     * 
     * @param bson         The BSON object to add the location info to <i>(must not be null)</i>.
     * @param locationInfo The LocationInfo object to add to the BSON object <i>(may be null)</i>.
     */
	protected void addLocationInformation(DBObject bson, final LocationInfo locationInfo)
    {
        if (locationInfo != null)
        {
            nullSafePut(bson, "fileName",   locationInfo.getFileName());
            nullSafePut(bson, "method",     locationInfo.getMethodName());
            nullSafePut(bson, "lineNumber", locationInfo.getLineNumber());
            nullSafePut(bson, "class",      bsonifyClassName(locationInfo.getClassName()));
        }
    }
   
}
