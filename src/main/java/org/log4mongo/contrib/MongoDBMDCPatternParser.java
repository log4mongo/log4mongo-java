/* Copyright (C) 2010 Robert Stewart (robert@wombatnation.com)
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

package org.log4mongo.contrib;

import java.util.Map;

import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * PatternParser that adds pattern converters for logging useful host-related
 * info, specifically:
 * <ul>
 * <li>hostname</li>
 * <li>VM name (which often includes the pid) of the JVM on this host</li>
 * <li>IP address</li>
 * </ul>
 */
public class MongoDBMDCPatternParser extends PatternParser {
	static final char MDC_NAME = 'X';

	public MongoDBMDCPatternParser(String pattern) {
		super(pattern);
	}

	/**
	 * This method is called on each pattern converter character while the
	 * PatternParser superclass is parsing the pattern. If the character is for
	 * a custom converter handled by this PatternParser subclass, this class
	 * adds the appropriate converter to a LinkedList of converters. If not, it
	 * allows the superclass to handle the converter character.
	 * 
	 * @see org.apache.log4j.helpers.PatternParser#finalizeConverter(char)
	 */
	public void finalizeConverter(char formatChar) {
		PatternConverter pc = null;
		switch (formatChar) {
		case MDC_NAME:
			String xOpt = extractOption();
			pc = new MongoDBMDCPatternConverter(formattingInfo, xOpt);
			currentLiteral.setLength(0);
			addConverter(pc);
			break;
		default:
			super.finalizeConverter(formatChar);
		}
	}

	private class MongoDBMDCPatternConverter extends PatternConverter {
		private String key;

		MongoDBMDCPatternConverter(FormattingInfo formattingInfo, String key) {
			super(formattingInfo);
			this.key = key;
		}

		protected void nullSafePut(DBObject bson, final String key,
				final Object value) {
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

		@SuppressWarnings("unchecked")
		public String convert(LoggingEvent event) {
			if (key == null) {
				Map<Object, Object> properties = event.getProperties();

				if (properties != null && properties.size() > 0) {

					BasicDBObject mdcProperties = new BasicDBObject();

					// Copy MDC properties into document
					for (Map.Entry<Object, Object> entry : properties
							.entrySet()) {
						nullSafePut(mdcProperties, entry.getKey().toString(),
								entry.getValue().toString());
					}
					return mdcProperties.toString();
				}

			}
			return null;
		}
	}
}
