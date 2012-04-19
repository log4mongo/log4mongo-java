package org.log4mongo.contrib;

import org.apache.log4j.helpers.PatternParser;
import org.log4mongo.MongoDbPatternLayout;

public class MongoDBMDCPatternLayout extends MongoDbPatternLayout {

	public MongoDBMDCPatternLayout() {
	}

	public MongoDBMDCPatternLayout(String pattern) {
		super(pattern);
	}

	public PatternParser createPatternParser(String pattern) {
		PatternParser parser;
		if (pattern == null)
			parser = new MongoDBMDCPatternParser(DEFAULT_CONVERSION_PATTERN);
		else
			parser = new MongoDBMDCPatternParser(pattern);

		return parser;
	}
}