/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either   express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 * Utility class to perform REST service invocation
 * 
 * @author raman
 * 
 */
public class RemoteConnector {

	/** */
	protected static final String RH_ACCEPT = "Accept";
	/** */
	protected static final String RH_AUTH_TOKEN = "Authorization";

	//
	/** Timeout (in ms) we specify for each http request */
	public static int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

	protected static HttpClient getHttpClient() {
		HttpClient httpClient = new DefaultHttpClient();
		final HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params,
				HTTP_REQUEST_TIMEOUT_MS);
		HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		return httpClient;
	}

	public static String getJSON(String host, String service, String token)
			throws SecurityException, RemoteException {
		return getJSON(host, service, token, null);
	}

	public static String getJSON(String host, String service, String token,
			Map<String, Object> parameters) throws RemoteException {
		final HttpResponse resp;

		try {
			String queryString = generateQueryString(parameters);
			final HttpGet get = new HttpGet(host + service + queryString);
			get.setHeader(RH_ACCEPT, "application/json");
			get.setHeader(RH_AUTH_TOKEN, bearer(token));

			resp = getHttpClient().execute(get);
			String response = EntityUtils.toString(resp.getEntity());
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return response;
			}
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
				throw new SecurityException();
			}
			throw new RemoteException("Error validating "
					+ resp.getStatusLine());
		} catch (ClientProtocolException e) {
			throw new RemoteException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new RemoteException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RemoteException(e.getMessage(), e);
		}

	}

	/**
	 * @param token
	 * @return
	 */
	protected static String bearer(String token) {
		return "Bearer " + token;
	}

	public static String postJSON(String host, String service, String body,
			String token) throws SecurityException, RemoteException {
		return postJSON(host, service, body, token, null);
	}

	public static String postJSON(String host, String service, String body,
			String token, Map<String, Object> parameters)
			throws SecurityException, RemoteException {

		String queryString = generateQueryString(parameters);
		final HttpResponse resp;
		final HttpPost post = new HttpPost(host + service + queryString);

		post.setHeader(RH_ACCEPT, "application/json");
		post.setHeader(RH_AUTH_TOKEN, bearer(token));

		try {
			StringEntity input = new StringEntity(body);
			input.setContentType("application/json");
			post.setEntity(input);

			resp = getHttpClient().execute(post);
			String response = EntityUtils.toString(resp.getEntity());
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return response;
			}
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN
					|| resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				throw new SecurityException();
			}

			String msg = "";
			try {
				msg = response.substring(response.indexOf("<h1>") + 4,
						response.indexOf("</h1>", response.indexOf("<h1>")));
			} catch (Exception e) {
				msg = resp.getStatusLine().toString();
			}
			throw new RemoteException(msg);
		} catch (ClientProtocolException e) {
			throw new RemoteException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new RemoteException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RemoteException(e.getMessage(), e);
		}
	}

	public static String putJSON(String host, String service, String token)
			throws SecurityException, RemoteException {
		return putJSON(host, service, null, token, null);
	}

	public static String putJSON(String host, String service, String body,
			String token) throws SecurityException, RemoteException {
		return putJSON(host, service, body, token, null);
	}

	public static String putJSON(String host, String service, String body,
			String token, Map<String, Object> parameters)
			throws SecurityException, RemoteException {
		final HttpResponse resp;

		String queryString = generateQueryString(parameters);

		final HttpPut put = new HttpPut(host + service + queryString);

		put.setHeader(RH_ACCEPT, "application/json");
		put.setHeader(RH_AUTH_TOKEN, bearer(token));

		try {
			if (body != null) {
				StringEntity input = new StringEntity(body);
				input.setContentType("application/json");
				put.setEntity(input);
			}

			resp = getHttpClient().execute(put);
			String response = EntityUtils.toString(resp.getEntity());
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return response;
			}
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
				throw new SecurityException();
			}
			throw new RemoteException("Error validating "
					+ resp.getStatusLine());
		} catch (final Exception e) {
			throw new RemoteException(e.getMessage(), e);
		}
	}

	public static String deleteJSON(String host, String service, String token)
			throws SecurityException, RemoteException {
		return deleteJSON(host, service, token, null);
	}

	public static String deleteJSON(String host, String service, String token,
			Map<String, Object> parameters) throws SecurityException,
			RemoteException {
		final HttpResponse resp;
		String queryString = generateQueryString(parameters);

		final HttpDelete delete = new HttpDelete(host + service + queryString);

		delete.setHeader(RH_ACCEPT, "application/json");
		delete.setHeader(RH_AUTH_TOKEN, bearer(token));

		try {
			resp = getHttpClient().execute(delete);
			String response = EntityUtils.toString(resp.getEntity());
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return response;
			}
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
				throw new SecurityException();
			}
			throw new RemoteException("Error validating "
					+ resp.getStatusLine());
		} catch (ClientProtocolException e) {
			throw new RemoteException(e.getMessage(), e);
		} catch (ParseException e) {
			throw new RemoteException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RemoteException(e.getMessage(), e);
		}
	}

	protected static String generateQueryString(Map<String, Object> parameters) {
		String queryString = "?";
		if (parameters != null) {
			for (String param : parameters.keySet()) {
				Object value = parameters.get(param);
				if (value == null) {
					if (queryString.length() > 1) {
						queryString += "&";
					}
					queryString += param + "=";
				} else if (value instanceof List) {
					for (Object v : ((List<?>) value)) {
						if (queryString.length() > 1) {
							queryString += "&";
						}
						queryString += param + "=" + encodeValue(v.toString());
					}
				} else {
					if (queryString.length() > 1) {
						queryString += "&";
					}
					queryString += param + "=" + encodeValue(value.toString());
				}

			}
		}
		return queryString.length() > 1 ? queryString : "";
	}

	protected static String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, "utf8");
		} catch (UnsupportedEncodingException e) {
			return value;
		}
	}
}
