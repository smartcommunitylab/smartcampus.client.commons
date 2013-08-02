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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author mirko perillo
 * 
 */
public class JsonUtils {

	private static final Set<Class> WRAPPER_TYPES = new HashSet<Class>(
			Arrays.asList(Boolean.class, Character.class, Byte.class,
					Short.class, Integer.class, Long.class, Float.class,
					Double.class, Void.class));

	private static boolean isWrapperType(Class clazz) {
		return WRAPPER_TYPES.contains(clazz);
	}

	public static String toJson(Object o) {
		String result = "";
		if (o == null) {
			return null;
		}
		if (o instanceof Collection<?>) {
			Iterator<?> iter = ((Collection) o).iterator();
			boolean isFirst = true;
			result += "[";
			while (iter.hasNext()) {
				if (!isFirst) {
					result += ",";
				}
				result += toJson(iter.next());
				isFirst = false;
			}
			result += "]";
			return result;
		}
		if (o.getClass().isArray()) {
			boolean isFirst = true;
			result += "[";
			for (Object element : (Object[]) o) {
				if (!isFirst) {
					result += ",";
				}
				result += toJson(element);
				isFirst = false;
			}
			result += "]";
			return result;
		}
		if (o instanceof String) {
			return JSONObject.quote((String) o);
		} else if (o.getClass().isPrimitive() || isWrapperType(o.getClass())
				|| o instanceof String) {
			return o.toString();
		} else {
			try {
				return (String) o.getClass().getMethod("toJson", o.getClass())
						.invoke(null, o);
			} catch (Exception e) {
				throw new IllegalArgumentException(o.getClass().getName()
						+ " MUST declare toJson method");
			}
		}
	}
	
	 public static Object toJSON(Object object) throws JSONException {
	        if (object instanceof Map) {
	            JSONObject json = new JSONObject();
	            Map map = (Map) object;
	            for (Object key : map.keySet()) {
	                json.put(key.toString(), toJSON(map.get(key)));
	            }
	            return json;
	        } else if (object instanceof Iterable) {
	            JSONArray json = new JSONArray();
	            for (Object value : ((Iterable)object)) {
	                json.put(value);
	            }
	            return json;
	        } else {
	            return object;
	        }
	    }

	    public static boolean isEmptyObject(JSONObject object) {
	        return object.names() == null;
	    }

	    public static Map<String, Object> getMap(JSONObject object, String key) throws JSONException {
	        return toMap(object.getJSONObject(key));
	    }

	    public static Map<String, Object> toMap(JSONObject object) throws JSONException {
	        Map<String, Object> map = new HashMap();
	        Iterator keys = object.keys();
	        while (keys.hasNext()) {
	            String key = (String) keys.next();
	            map.put(key, fromJson(object.get(key)));
	        }
	        return map;
	    }

	    public static List toList(JSONArray array) throws JSONException {
	        List list = new ArrayList();
	        for (int i = 0; i < array.length(); i++) {
	            list.add(fromJson(array.get(i)));
	        }
	        return list;
	    }

	    private static Object fromJson(Object json) throws JSONException {
	        if (json == JSONObject.NULL) {
	            return null;
	        } else if (json instanceof JSONObject) {
	            return toMap((JSONObject) json);
	        } else if (json instanceof JSONArray) {
	            return toList((JSONArray) json);
	        } else {
	            return json;
	        }
	    }
}
