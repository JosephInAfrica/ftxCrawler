package com.hui.house.crawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/*
 * 自动换ip
 * 重试机制
 */
public class Downloader {

	public static String html;

	public static int retry = 5;
	public static int retrySleep = 5000;

	public static String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:52.0) Gecko/20100101 Firefox/52.0";
	static Log log = LogFactory.getLog(Downloader.class);

	private static CloseableHttpClient httpclient =HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom().setRedirectsEnabled(false).build()).build();
	

	public static byte[] readInputStream(InputStream instream) throws Exception {

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1204];
		int len = 0;
		while ((len = instream.read(buffer)) != -1) {
			outStream.write(buffer, 0, len);
		}
		instream.close();
		return outStream.toByteArray();
	}


	public static String getResponse(String url, String httpType, String Charset, String checkword)
			throws InterruptedException, IOException, KeyManagementException, NoSuchAlgorithmException {
		
			
		HttpGet httpget = new HttpGet(url);
	
		httpget.addHeader("User-Agent", ua);
		try {
			
			CloseableHttpResponse response = httpclient.execute(httpget);
			
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				byte[] data = readInputStream(instream);
				String htmlsource = new String(data, Charset);
				if (400 <= response.getStatusLine().getStatusCode() || htmlsource.contains(checkword)) {
					response.close();
					html = htmlsource;
				} else {
					
					throw new Exception("出错了");
					
				}
			}
		} catch (Exception e) {
			log.error("读取网页错误"+url +e);
			System.out.println("出错原因:" + e);
			html = null;

			if (retry > 1) {
				retry -= 1;
				Thread.sleep(retrySleep);
				httpclient = setProxy("http");
				html = getResponse(url, httpType, Charset, checkword);

			}

		}

		return html;

	}

	public static CloseableHttpClient setProxy(String httpType)
			throws IOException, KeyManagementException, NoSuchAlgorithmException {
		// 依次是代理端口号，协议类型
		SSLContext sslcontext = createIgnoreVerifySSL();
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", new SSLConnectionSocketFactory(sslcontext)).build();
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		HttpClients.custom().setConnectionManager(connManager);

		Document doc = Jsoup.connect("http://139.224.196.60/proxy/https").get();
		String proxycont = doc.text();
		System.out.println(proxycont);
		String[] proxies = proxycont.split(":");

		HttpHost proxy = new HttpHost(proxies[0], Integer.valueOf(proxies[1]), httpType);
		DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
		RequestConfig config = RequestConfig.custom().setRedirectsEnabled(false).build();
		CloseableHttpClient hclient = HttpClients.custom().setRoutePlanner(routePlanner).setConnectionManager(connManager).setDefaultRequestConfig(config).build();
		return hclient;
	}
	

	public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sc = SSLContext.getInstance("SSLv3");

		// 实现TrustManager接口，用于绕过验证，不用修改里面的方法?
		X509TrustManager trustManager = new X509TrustManager() {
			public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		sc.init(null, new TrustManager[] { trustManager }, null);
		return sc;
	}
	
}
