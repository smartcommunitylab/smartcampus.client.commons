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
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
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

	/**
	 * {@link HttpClient} certificate verifier type. Use with caution.
	 * @author raman
	 *
	 */
	public enum CLIENT_TYPE {CLIENT_NORMAL, CLIENT_WILDCARD, CLIENT_ACCEPTALL};
	
	private static CLIENT_TYPE clientType = CLIENT_TYPE.CLIENT_NORMAL;
	
	/**
	 * 
	 */
	private static final String DEFAULT_CHARSET = "UTF-8";
	/** */
	protected static final String RH_ACCEPT = "Accept";
	/** */
	protected static final String RH_AUTH_TOKEN = "Authorization";

	//
	/** Timeout (in ms) we specify for each http request */
	public static int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

	protected static HttpClient getHttpClient() {
		HttpClient httpClient = null;
		switch (clientType) {
		case CLIENT_WILDCARD:
			httpClient = getWildcartHttpClient(null);
			break;
		case CLIENT_ACCEPTALL:
			httpClient = getAcceptAllHttpClient(null);
			break;
		default:
			httpClient = getDefaultHttpClient(null);
		}
		
		final HttpParams params = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
		return httpClient;
	}

	/**
	 * Set the way the SSL certificates are managed for the HTTP calls. 
	 * @param type
	 */
	public static void setClientType(CLIENT_TYPE type) {
		clientType = type;
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
			String response = EntityUtils.toString(resp.getEntity(),DEFAULT_CHARSET);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return response;
			}
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN
					|| resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
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
			StringEntity input = new StringEntity(body, DEFAULT_CHARSET);
			input.setContentType("application/json");
			post.setEntity(input);

			resp = getHttpClient().execute(post);
			String response = EntityUtils.toString(resp.getEntity(),DEFAULT_CHARSET);
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
				StringEntity input = new StringEntity(body, DEFAULT_CHARSET);
				input.setContentType("application/json");
				put.setEntity(input);
			}

			resp = getHttpClient().execute(put);
			String response = EntityUtils.toString(resp.getEntity(),DEFAULT_CHARSET);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return response;
			}
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN
					|| resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
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
			String response = EntityUtils.toString(resp.getEntity(),DEFAULT_CHARSET);
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return response;
			}
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN
					|| resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
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
	
	private static HttpClient getDefaultHttpClient(HttpParams inParams) {
		if (inParams != null) {
			return new DefaultHttpClient(inParams);
		} else {
			return new DefaultHttpClient();
		}
	}

	private static HttpClient getAcceptAllHttpClient(HttpParams inParams) {
		HttpClient client = null;

		HttpParams params = inParams != null ? inParams : new BasicHttpParams();

		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

			// IMPORTANT: use CustolSSLSocketFactory for 2.2
			SSLSocketFactory sslSocketFactory = new CustomSSLSocketFactory(trustStore);
			sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			registry.register(new Scheme("https", sslSocketFactory, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

			client = new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			client = new DefaultHttpClient(params);
		}

		return client;
	}

	private static HttpClient getWildcartHttpClient(HttpParams inParams) {
		HttpClient client = null;

		HttpParams params = inParams != null ? inParams : new BasicHttpParams();

		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

			SSLSocketFactory sslSocketFactory = new CustomSSLSocketFactory(trustStore);
			final X509HostnameVerifier delegate = sslSocketFactory.getHostnameVerifier();
			if (!(delegate instanceof WildcardVerifier)) {
				sslSocketFactory.setHostnameVerifier(new WildcardVerifier(delegate));
			}
			registry.register(new Scheme("https", sslSocketFactory, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

			client = new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			client = new DefaultHttpClient(params);
		}

		return client;
	}

	/*
	 * Custom classes
	 */
	private static class WildcardVerifier extends AbstractVerifier {
		private final X509HostnameVerifier delegate;

		public WildcardVerifier(final X509HostnameVerifier delegate) {
			this.delegate = delegate;
		}

		@Override
		public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
			boolean ok = false;
			try {
				delegate.verify(host, cns, subjectAlts);
			} catch (SSLException e) {
				for (String cn : cns) {
					if (cn.startsWith("*.")) {
						try {
							delegate.verify(host, new String[] { cn.substring(2) }, subjectAlts);
							ok = true;
						} catch (Exception e1) {
							throw new SSLException(e1);
						}
					}
				}
				if (!ok) {
					throw e;
				}
			}
		}
	}

	private static class CustomSSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public CustomSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException,
				KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
				UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

}
