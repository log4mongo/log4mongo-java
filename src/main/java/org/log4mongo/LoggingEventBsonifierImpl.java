/*
 * Copyright (C) 2010 Robert Stewart (robert@wombatnation.com)
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

package org.log4mongo;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Default implementation class for creating a BSON representation of a Log4J LoggingEvent.
 */
public class LoggingEventBsonifierImpl implements LoggingEventBsonifier {

    // Main log event elements
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_THREAD = "thread";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_LOGGER_NAME = "loggerName";
    // Source code location
    private static final String KEY_FILE_NAME = "fileName";
    private static final String KEY_METHOD = "method";
    private static final String KEY_LINE_NUMBER = "lineNumber";
    private static final String KEY_CLASS = "class";
    // Class info
    private static final String KEY_FQCN = "fullyQualifiedClassName";
    private static final String KEY_PACKAGE = "package";
    private static final String KEY_CLASS_NAME = "className";
    // Exceptions
    private static final String KEY_THROWABLES = "throwables";
    private static final String KEY_EXCEPTION_MESSAGE = "message";
    private static final String KEY_STACK_TRACE = "stackTrace";
    // Host and Process Info
    private static final String KEY_HOST = "host";
    private static final String KEY_PROCESS = "process";
    private static final String KEY_HOSTNAME = "name";
    private static final String KEY_IP = "ip";
    // MDC Properties
    private static final String KEY_MDC_PROPERTIES = "properties";

    private DBObject hostInfo = new BasicDBObject();

    public LoggingEventBsonifierImpl() {
        setupNetworkInfo();
    }

    private void setupNetworkInfo() {
        hostInfo.put(KEY_PROCESS, ManagementFactory.getRuntimeMXBean().getName());
        try {
            hostInfo.put(KEY_HOSTNAME, InetAddress.getLocalHost().getHostName());
            hostInfo.put(KEY_IP, InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            LogLog.warn(e.getMessage());
        }
    }

    /**
     * BSONifies a single Log4J LoggingEvent object.
     * 
     * @param loggingEvent
     *            The LoggingEvent object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the LoggingEvent object <i>(may be null)</i>.
     */
    public DBObject bsonify(final LoggingEvent loggingEvent) {
        DBObject result = null;

        if (loggingEvent != null) {
            result = new BasicDBObject();

            result.put(KEY_TIMESTAMP, new Date(loggingEvent.getTimeStamp()));
            nullSafePut(result, KEY_LEVEL, loggingEvent.getLevel().toString());
            nullSafePut(result, KEY_THREAD, loggingEvent.getThreadName());
            nullSafePut(result, KEY_MESSAGE, loggingEvent.getMessage());
            nullSafePut(result, KEY_LOGGER_NAME, bsonifyClassName(loggingEvent.getLoggerName()));

            addMDCInformation(result, loggingEvent.getProperties());
            addLocationInformation(result, loggingEvent.getLocationInformation());
            addThrowableInformation(result, loggingEvent.getThrowableInformation());
            addHostnameInformation(result);
        }

        return (result);
    }

    /**
     * Adds MDC Properties to the DBObject.
     * 
     * @param bson
     *            The root DBObject
     * @param props
     *            MDC Properties to be logged
     */
    protected void addMDCInformation(DBObject bson, final Map<Object, Object> props) {
        if (props != null && props.size() > 0) {

            BasicDBObject mdcProperties = new BasicDBObject(props);
        }
    }

    /**
     * Adds the LocationInfo object to an existing BSON object.
     * 
     * @param bson
     *            The BSON object to add the location info to <i>(must not be null)</i>.
     * @param locationInfo
     *            The LocationInfo object to add to the BSON object <i>(may be null)</i>.
     */
    protected void addLocationInformation(DBObject bson, final LocationInfo locationInfo) {
        if (locationInfo != null) {
            nullSafePut(bson, KEY_FILE_NAME, locationInfo.getFileName());
            nullSafePut(bson, KEY_METHOD, locationInfo.getMethodName());
            nullSafePut(bson, KEY_LINE_NUMBER, locationInfo.getLineNumber());
            nullSafePut(bson, KEY_CLASS, bsonifyClassName(locationInfo.getClassName()));
        }
    }

    /**
     * Adds the ThrowableInformation object to an existing BSON object.
     * 
     * @param bson
     *            The BSON object to add the throwable info to <i>(must not be null)</i>.
     * @param throwableInfo
     *            The ThrowableInformation object to add to the BSON object <i>(may be null)</i>.
     */
    @SuppressWarnings(value = "unchecked")
    protected void addThrowableInformation(DBObject bson, final ThrowableInformation throwableInfo) {
        if (throwableInfo != null) {
            Throwable currentThrowable = throwableInfo.getThrowable();
            List throwables = new BasicDBList();

            while (currentThrowable != null) {
                DBObject throwableBson = bsonifyThrowable(currentThrowable);

                if (throwableBson != null) {
                    throwables.add(throwableBson);
                }

                currentThrowable = currentThrowable.getCause();
            }

            if (throwables.size() > 0) {
                bson.put(KEY_THROWABLES, throwables);
            }
        }
    }

    /**
     * Adds the current process's host name, VM name and IP address
     * 
     * @param bson
     *            A BSON object containing host name, VM name and IP address
     */
    protected void addHostnameInformation(DBObject bson) {
        nullSafePut(bson, KEY_HOST, hostInfo);
    }

    /**
     * BSONifies the given Throwable.
     * 
     * @param throwable
     *            The throwable object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the Throwable object <i>(may be null)</i>.
     */
    protected DBObject bsonifyThrowable(final Throwable throwable) {
        DBObject result = null;

        if (throwable != null) {
            result = new BasicDBObject();

            nullSafePut(result, KEY_EXCEPTION_MESSAGE, throwable.getMessage());
            nullSafePut(result, KEY_STACK_TRACE, bsonifyStackTrace(throwable.getStackTrace()));
        }

        return (result);
    }

    /**
     * BSONifies the given stack trace.
     * 
     * @param stackTrace
     *            The stack trace object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the stack trace object <i>(may be null)</i>.
     */
    protected DBObject bsonifyStackTrace(final StackTraceElement[] stackTrace) {
        BasicDBList result = null;

        if (stackTrace != null && stackTrace.length > 0) {
            result = new BasicDBList();

            for (StackTraceElement element : stackTrace) {
                DBObject bson = bsonifyStackTraceElement(element);

                if (bson != null) {
                    result.add(bson);
                }
            }
        }

        return (result);
    }

    /**
     * BSONifies the given stack trace element.
     * 
     * @param element
     *            The stack trace element object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the stack trace element object <i>(may be null)</i>.
     */
    protected DBObject bsonifyStackTraceElement(final StackTraceElement element) {
        DBObject result = null;

        if (element != null) {
            result = new BasicDBObject();

            nullSafePut(result, KEY_FILE_NAME, element.getFileName());
            nullSafePut(result, KEY_METHOD, element.getMethodName());
            nullSafePut(result, KEY_LINE_NUMBER, element.getLineNumber());
            nullSafePut(result, KEY_CLASS, bsonifyClassName(element.getClassName()));
        }

        return (result);
    }

    /**
     * BSONifies the given class name.
     * 
     * @param className
     *            The class name to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the class name <i>(may be null)</i>.
     */
    @SuppressWarnings(value = "unchecked")
    protected DBObject bsonifyClassName(final String className) {
        DBObject result = null;

        if (className != null && className.trim().length() > 0) {
            result = new BasicDBObject();

            result.put(KEY_FQCN, className);

            List packageComponents = new BasicDBList();
            String[] packageAndClassName = className.split("\\.");

            packageComponents.addAll(Arrays.asList(packageAndClassName));
            // Requires Java 6
            // packageComponents.addAll(Arrays.asList(Arrays.copyOf(packageAndClassName,
            // packageAndClassName.length - 1)));

            if (packageComponents.size() > 0) {
                result.put(KEY_PACKAGE, packageComponents);
            }

            result.put(KEY_CLASS_NAME, packageAndClassName[packageAndClassName.length - 1]);
        }

        return (result);
    }

    /**
     * Adds the given value to the given key, except if it's null (in which case this method does
     * nothing).
     * 
     * @param bson
     *            The BSON object to add the key/value to <i>(must not be null)</i>.
     * @param key
     *            The key of the object <i>(must not be null)</i>.
     * @param value
     *            The value of the object <i>(may be null)</i>.
     */
    protected void nullSafePut(DBObject bson, final String key, final Object value) {
        if (value != null) {
            if (value instanceof String) {
                String stringValue = (String) value;

                if (stringValue.trim().length() > 0) {
                    bson.put(key, stringValue);
                }
            } else {
                bson.put(key, value);
            }
        }
    }

}
