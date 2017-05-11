package com.hui.house.crawler;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class TestHouse {
	public static void main(String[] args) throws IOException, KeyManagementException, NoSuchAlgorithmException, InterruptedException {
		HouseCrawler house = new HouseCrawler();
		house.getCity();
		//house.getDetail("http://esf.xuchang.fang.com/////house-a011746/");
		//house.getPage("http://esf.xuchang.fang.com/");
		//System.out.println(house.getForamtUrl("http://esf.xuchang.fang.com/////a///a///"));
		System.out.println("ok");
	}

}
