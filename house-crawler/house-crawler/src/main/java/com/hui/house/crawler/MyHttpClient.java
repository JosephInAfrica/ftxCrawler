package com.hui.house.crawler;

import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class MyHttpClient {
	public static HttpHost getProxy(String httpType) throws IOException {
		Document doc = Jsoup.connect("http://139.224.196.60/proxy/" + httpType).get();
		String proxycont = doc.text();
		System.out.println(proxycont);
		String[] proxies = proxycont.split(":");

		HttpHost proxy = new HttpHost(proxies[0], Integer.valueOf(proxies[1]).intValue(), httpType);
		return proxy;
	}

	public static CloseableHttpClient getClient(HttpHost proxy, boolean redirect)
			throws KeyManagementException, NoSuchAlgorithmException {
		SSLContext sslcontext = createIgnoreVerifySSL();

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", new SSLConnectionSocketFactory(sslcontext)).build();
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connManager);
		DefaultProxyRoutePlanner routePlanner = null;
		if (proxy != null) {
			routePlanner = new DefaultProxyRoutePlanner(proxy);
			builder.setRoutePlanner(routePlanner);
		}

		RequestConfig.Builder configer = RequestConfig.custom().setConnectTimeout(30000)
				.setConnectionRequestTimeout(5000).setSocketTimeout(5000);
		RequestConfig config;
		if (redirect) {
			config = configer.build();
		} else {
			config = configer.setRedirectsEnabled(false).build();
		}

		CloseableHttpClient hclient = builder.setDefaultRequestConfig(config).setConnectionManager(connManager).build();
		return hclient;
	}

	public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sc = SSLContext.getInstance("SSLv3");

		X509TrustManager trustManager = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
					throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
					throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		sc.init(null, new TrustManager[] { trustManager }, null);
		return sc;
	}
}