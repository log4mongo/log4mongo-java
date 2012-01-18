package org.log4mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This extended MongoDbAppender is designed so you can add top
 * level elements to each logging entry. I think that users can also extend MongoDbAppender themselves in
 * order to add the top level elements.
 *
 * Rational: My use case is that many different code bases will use a common
 * appender for unified logs, and the common elements in a unified logging system
 * should be consistent such as application, eventType etc...
 * So by adding a property called rootLevelProperties with a key=value
 * pair of elements to be added to the root level log.
 *
 * @author Mick Knutson (http://www.baselogic.com)
 */
public class ExtendedMongoDbAppender extends MongoDbAppender {

    private DBObject constants;

    private Map<String, String> rootProperties = new LinkedHashMap<String, String>();


    /**
     * @see org.apache.log4j.AppenderSkeleton#activateOptions()
     */
    @Override
    public void activateOptions() {
        super.activateOptions();
        initTopLevelProperties();
    }

    /**
     * Initialize Top level JSON properties
     * Allow users to create custom properties to be added
     * to the top level JSON log.
     */
    public void initTopLevelProperties(){
        constants = new BasicDBObject();
        if(!rootProperties.isEmpty()){
            constants.putAll(rootProperties);
        }
    }

    /**
     * This will handle spaces and empty values
     * A = minus- & C=equals= & E==F
     * For XML, must escape (&amp;)
     * @param rootLevelProperties
     */
    public void setRootLevelProperties(String rootLevelProperties) {
        for(String keyValue : rootLevelProperties.split(" *& *")) {
            String[] pairs = keyValue.split(" *= *", 2);
            rootProperties.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
        }
    }


    /**
     * @param bson The BSON object to insert into a MongoDB database collection.
     */
    @Override
    public void append(DBObject bson) {
        if (this.isInitialized() && bson != null) {
            if (constants != null) {
                bson.putAll(constants);
            }
            super.append(bson);
        }
    }
}