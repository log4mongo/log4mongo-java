package com.google.code.log4mongo.contrib;

import org.apache.log4j.helpers.PatternParser;

import com.google.code.log4mongo.MongoDbPatternLayout;

public class AddHostPatternLayout extends MongoDbPatternLayout
{

	public AddHostPatternLayout()
	{
	}

	public AddHostPatternLayout(String pattern)
	{
		super(pattern);
	}

	public PatternParser createPatternParser(String pattern)
	{
		PatternParser parser;
		if (pattern == null)
			parser = new AddHostPatternParser(DEFAULT_CONVERSION_PATTERN);
		else
			parser = new AddHostPatternParser(pattern);

		return parser;
	}
}
