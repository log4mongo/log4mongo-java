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
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;


/**
 * Log4J Appender that writes log events into the MongoDB document oriented database.  Log events are fully parsed and stored
 * as structured records in MongoDB (this appender does not require, nor use a Log4J layout).
 * 
 * The appender does <u>not</u> create any indexes on the data that's stored - it is assumed that if query performance is
 * required those would be created externally (eg. in the mongodb shell or an external reporting application).
 * 
 
 * @author Peter Monks (pmonks@gmail.com)
 * @modify Gabriel Eisbruch (gabrieleisbruch@gmail.com)
 * @see http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Appender.html
 * @see http://www.mongodb.org/
 * @version $Id$
 */
public class MongoDbAppender
    extends BsonAppender
{
    private final static String DEFAULT_MONGO_DB_HOSTNAME        = "localhost";
    private final static int    DEFAULT_MONGO_DB_PORT            = 27017;
    private final static String DEFAULT_MONGO_DB_DATABASE_NAME   = "log4mongo";
    private final static String DEFAULT_MONGO_DB_COLLECTION_NAME = "logevents";
    
    private String hostname       = DEFAULT_MONGO_DB_HOSTNAME;
    private int    port           = DEFAULT_MONGO_DB_PORT;
    private String databaseName   = DEFAULT_MONGO_DB_DATABASE_NAME;
    private String collectionName = DEFAULT_MONGO_DB_COLLECTION_NAME;
    private String userName       = null;
    private String password       = null;
    
    private DBCollection collection = null;

    /**
     * @see org.apache.log4j.Appender#requiresLayout()
     */
    public boolean requiresLayout()
    {
        return(false);
    }

 
    /**
     * @see org.apache.log4j.AppenderSkeleton#activateOptions()
     */
    @Override
    public void activateOptions()
    {
        try
        {
            Mongo mongo    = new Mongo(hostname, port);
            DB    database = mongo.getDB(databaseName);
            
            if (userName != null && userName.trim().length() > 0)
            {
                if (!database.authenticate(userName, password.toCharArray()))
                {
                    throw new RuntimeException("Unable to authenticate with MongoDB server.");
                }
                
                // Allow password to be GCed
                password = null;
            }
            
            setCollection(database.getCollection(collectionName));
        }
        catch (Exception e)
        {
            errorHandler.error("Unexpected exception while initialising MongoDbAppender.", e, ErrorCode.GENERIC_FAILURE);
        }
    }

   
    
    /**
     * Note: this method is primarily intended for use by the unit tests.
     * 
     * @param collection The MongoDB collection to use when logging events.
     */
    public void setCollection(final DBCollection collection)
    {
        // PRECONDITIONS
        assert collection != null : "collection must not be null.";
        
        // Body
        this.collection = collection;
    }
    
    
  
    

    
    /**
     * @see org.apache.log4j.Appender#close()
     */
    public void close()
    {
        collection = null;
    }

    
    /**
     * @return The hostname of the MongoDB server <i>(will not be null, empty or blank)</i>.
     */
    public String getHostname()
    {
        return(hostname);
    }


    /**
     * @param hostname The MongoDB hostname to set <i>(must not be null, empty or blank)</i>.
     */
    public void setHostname(final String hostname)
    {
        // PRECONDITIONS
        assert hostname != null             : "hostname must not be null";
        assert hostname.trim().length() > 0 : "hostname must not be empty or blank";
        
        // Body
        this.hostname = hostname;
    }
    
    
    /**
     * @return The port of the MongoDB server <i>(will be > 0)</i>.
     */
    public int getPort()
    {
        return(port);
    }


    /**
     * @param port The port to set <i>(must be > 0)</i>.
     */
    public void setPort(final int port)
    {
        // PRECONDITIONS
        assert port > 0 : "port must be > 0";
        
        // Body
        this.port = port;
    }
    
    
    /**
     * @return The database used in the MongoDB server <i>(will not be null, empty or blank)</i>.
     */
    public String getDatabaseName()
    {
        return(databaseName);
    }


    /**
     * @param databaseName The database to use in the MongoDB server <i>(must not be null, empty or blank)</i>.
     */
    public void setDatabaseName(final String databaseName)
    {
        // PRECONDITIONS
        assert databaseName != null             : "database must not be null";
        assert databaseName.trim().length() > 0 : "database must not be empty or blank";
        
        // Body
        this.databaseName = databaseName;
    }


    /**
     * @return The collection used within the database in the MongoDB server <i>(will not be null, empty or blank)</i>.
     */
    public String getCollectionName()
    {
        return(collectionName);
    }


    /**
     * @param collectionName The collection used within the database in the MongoDB server <i>(must not be null, empty or blank)</i>.
     */
    public void setCollectionName(final String collectionName)
    {
        // PRECONDITIONS
        assert collectionName != null             : "collection must not be null";
        assert collectionName.trim().length() > 0 : "collection must not be empty or blank";
        
        // Body
        this.collectionName = collectionName;
    }


    /**
     * @return The userName used to authenticate with MongoDB <i>(may be null)</i>.
     */
    public String getUserName()
    {
        return(userName);
    }


    /**
     * @param userName The userName to use when authenticating with MongoDB <i>(may be null)</i>.
     */
    public void setUserName(final String userName)
    {
        this.userName = userName;
    }
    

    /**
     * @param password The password to use when authenticating with MongoDB <i>(may be null)</i>.
     */
    public void setPassword(final String password)
    {
        this.password = password;
    }
    
    
    /**
     * 
     * @return The MongoDB collection to which events are logged.
     */
    protected DBCollection getCollection()
    {
        return(collection);
    }


	@Override
	public void append(DBObject bson) {
        if (bson != null)
        {
            try {
                getCollection().insert(bson);
            } catch (MongoException e) {
                errorHandler.error("Failed to insert document to MongoDB", e,
                               ErrorCode.WRITE_FAILURE);
            }
        }
		
	}

}
