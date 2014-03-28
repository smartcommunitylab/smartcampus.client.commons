/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.network;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.NopAnnotationIntrospector;
import org.codehaus.jackson.type.TypeReference;

/**
 * @author mirko perillo
 * 
 */
public class JsonUtils {

	private static ObjectMapper fullMapper = new ObjectMapper();
	static {
		fullMapper.setAnnotationIntrospector(NopAnnotationIntrospector
				.nopInstance());
		fullMapper
				.enable(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING);
		fullMapper
				.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);

		fullMapper
				.enable(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING);
		fullMapper.disable(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS);
	}

	/**
	 * Convert an object to object of the specified class
	 * 
	 * @param object
	 * @param cls
	 *            target object class
	 * @return converted object
	 */
	public static <T> T convert(Object object, Class<T> cls) {
		return fullMapper.convertValue(object, cls);
	}

	/**
	 * Convert an object to JSON
	 * 
	 * @param data
	 * @return JSON representation of the object
	 */
	public static String toJSON(Object data) {
		try {
			return fullMapper.writeValueAsString(data);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Convert JSON String to an object of the specified class
	 * 
	 * @param body
	 * @param cls
	 * @return
	 */
	public static <T> T toObject(String body, Class<T> cls) {
		try {
			return fullMapper.readValue(body, cls);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Convert JSON array string to the list of objects of the specified class
	 * 
	 * @param body
	 * @param cls
	 * @return
	 */
	public static <T> List<T> toObjectList(String body, Class<T> cls) {
		try {
			List<Object> list = fullMapper.readValue(body,
					new TypeReference<List<?>>() {
					});
			List<T> result = new ArrayList<T>();
			for (Object o : list) {
				result.add(fullMapper.convertValue(o, cls));
			}
			return result;
		} catch (Exception e) {
			return null;
		}
	}

}
