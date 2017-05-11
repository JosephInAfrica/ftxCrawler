package com.hui.house.crawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class Downloader {
	public static String html;
	private static int retry = 3;
	private static HttpHost proxy;
	public static String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:52.0) Gecko/20100101 Firefox/52.0";
	static Log log = LogFactory.getLog(Downloader.class);
	public static CloseableHttpClient httpclient;

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

	public static String getResponse(String url, String httpType, String Charset, String checkword, String mustIn)
			throws InterruptedException, IOException, KeyManagementException, NoSuchAlgorithmException {
		httpclient = MyHttpClient.getClient(proxy, true);

		HttpGet httpget = new HttpGet(url);
		httpget.addHeader("User-Agent", ua);
		try {
			CloseableHttpResponse response = httpclient.execute(httpget);

			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				byte[] data = readInputStream(instream);
				String htmlsource = new String(data, Charset);
				if ((200 <= response.getStatusLine().getStatusCode())
						&& (response.getStatusLine().getStatusCode() < 400) && (htmlsource.contains(mustIn))) {
					response.close();
					if ((checkword != "") && (htmlsource.contains(checkword))) {
						throw new Exception("checkword出现在源代码里！");
					}
					html = htmlsource;
				} else {
					throw new Exception("出错了" + response.getStatusLine().getStatusCode());
				}
			}
		} catch (Exception e) {
			log.error("读取网页错误" + url + e);
			System.out.println("出错原因:" + e);
			html = null;

			if (retry > 1) {
				retry -= 1;
				proxy = MyHttpClient.getProxy(httpType);
				if (retry == 2) {
					Thread.sleep(3000L);
					proxy = null;
				}

				html = getResponse(url, httpType, Charset, checkword, mustIn);
			}

		}

		return html;
	}

	public static void closeClient() throws IOException {
		httpclient.close();
	}
}