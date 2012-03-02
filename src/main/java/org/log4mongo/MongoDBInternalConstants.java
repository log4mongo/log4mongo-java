package org.log4mongo;

/*
 * Interface Class Providing all the constants required by log4Mongo. 
 * It includes the constants providing the header information.
 */
public interface MongoDBInternalConstants {
	/*
	 * Default Mongo DB Hostname. The default hostname is 'localhost' if you are
	 * running in the local environment. Otherwise, it is preferable to use an
	 * IP Address. e.g. : https://127.0.0.1/ <br />Do not include the port
	 * number in the hostname
	 */
	final static String DEFAULT_MONGO_DB_HOSTNAME = "localhost";
	/*
	 * Default Port Number for Mongo. Mongo runs on 27017 by default when
	 * deployed.
	 */
	final static String DEFAULT_MONGO_DB_PORT = "27017";
	/*
	 * The database name where the collection is to be used . If the database
	 * doesnt exist, it will be created automatically.
	 */
	final static String DEFAULT_MONGO_DB_DATABASE_NAME = "log4mongo";
	/*
	 * The Collection name used to store the documents. If the collection doesnt
	 * exist, it will be created automatically.
	 */
	final static String DEFAULT_MONGO_DB_COLLECTION_NAME = "logevents";

	/*
	 * The DBObject element names.
	 */
	// Main log event elements
	static final String KEY_TIMESTAMP = "timestamp";
	static final String KEY_LEVEL = "level";
	static final String KEY_THREAD = "thread";
	static final String KEY_MESSAGE = "message";
	static final String KEY_LOGGER_NAME = "loggerName";
	// Source code location
	static final String KEY_FILE_NAME = "fileName";
	static final String KEY_METHOD = "method";
	static final String KEY_LINE_NUMBER = "lineNumber";
	static final String KEY_CLASS = "class";
	// Class info
	static final String KEY_FQCN = "fullyQualifiedClassName";
	static final String KEY_PACKAGE = "package";
	static final String KEY_CLASS_NAME = "className";
	// Exceptions
	static final String KEY_THROWABLES = "throwables";
	static final String KEY_EXCEPTION_MESSAGE = "message";
	static final String KEY_STACK_TRACE = "stackTrace";
	// Host and Process Info
	static final String KEY_HOST = "host";
	static final String KEY_PROCESS = "process";
	static final String KEY_HOSTNAME = "name";
	static final String KEY_IP = "ip";
	// MDC Properties
	static final String KEY_MDC_PROPERTIES = "properties";

}
