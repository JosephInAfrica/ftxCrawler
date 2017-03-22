package com.hui.house.crawler;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

public class Main implements Runnable {

	private final static Logger logger = Logger.getLogger(Main.class);
	
	public static void main(String[] args)
			throws KeyManagementException, NoSuchAlgorithmException, InterruptedException, IOException {
		logger.info("爬虫工作开始！");
		Thread runner = new Thread(new Main());
		//Thread runner1 = new Thread(new Main());
		runner.start();
		//runner1.start();
	}

	public void run() {
		try {
			HouseCrawler house = new HouseCrawler();
			while (true) {
				String cityUrl = house.jedis.rpop(house.cityRedis);
				if (cityUrl == null) {
					break;
				}
				try {
					house.getPage(cityUrl);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			while (true) {
				String pageUrl = house.jedis.rpop(house.pageRedis);
				if (pageUrl == null) {
					break;
				}
				try {
					house.getDetail(pageUrl);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
