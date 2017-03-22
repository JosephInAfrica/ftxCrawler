package com.hui.house.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import redis.clients.jedis.Jedis;
public class HouseCrawler {
	
	public   String cityRedis;
	public   String pageRedis;
	public Jedis jedis = RedisUtils.getJedisObject();
	private final Log log =LogFactory.getLog(getClass());
	
	
	public static String  getForamtUrl(String url) {
		String ur =  url.substring(7);
		
		return url.substring(0, 7) +ur.replaceAll("/+", "/");
		
	}

	
	public HouseCrawler() throws IOException{
		 Properties props = new Properties();
	      InputStream is = this.getClass().getResourceAsStream("/config.properties");
	      props.load(is);
	      cityRedis = props.getProperty("redis_city");
	      pageRedis =props.getProperty("redis_page");
	}

	
	public  String jsoupNoError (Element etree ,String selector ){  
		String data  ;
		try{
			data= etree.select(selector).first().text();
		}catch(Exception e){
			log.info("解析出错"+selector);
			System.out.println(e);
			data ="";
		}
		
		return data;
	}
	

	public  void getCity() throws IOException, KeyManagementException, NoSuchAlgorithmException, InterruptedException{
		
		String citys =  Downloader.getResponse("http://esf.bd.fang.com/newsecond/esfcities.aspx", "http", "GB2312", "");
		Document doc = Jsoup.parse(citys);
		Elements city = doc.select("div#c02 li a");
		for(Element ele :city){
			
			String cityLink =getForamtUrl(ele.attr("href"));
			jedis.rpush(cityRedis, cityLink);
		}
	}
	
	public void getPage(String ur) throws KeyManagementException, NoSuchAlgorithmException, InterruptedException, IOException{
		new Downloader();
		String cityPage = Downloader.getResponse(ur,"http", "GB2312", "");
		Document doc = Jsoup.parse(cityPage);
//		if(!doc.select("#list_D02_10 > div.qxName > a.org.selected").isEmpty()){
//			log.info("结束");
//			return;
//		}  
		Elements ele = doc.select("#list_D02_10 > div.qxName > a[href ^=/house]");
		for(Element link :ele){
			String pageLink = getForamtUrl(ur+ link.attr("href"));
		
			System.out.println(pageLink);
			jedis.rpush(pageRedis,pageLink);
		}

	}
	
	public  void getDetail(String ur) throws InterruptedException{

		int k =1;
		Random rand = new Random();
		SimpleDateFormat d = new  SimpleDateFormat("yyyy-MM-dd");
		
		String update =  d.format(new Date());
		while (true){
			Document doc = null; //当前页面的dom 对象
			try {
				String detailUr =String.format("%s/i3%d/",ur,k);
				System.out.println(detailUr);
				ArrayList<Map> list = new ArrayList<Map>();
				String page =Downloader.getResponse(detailUr ,"http","GB2312","");
				doc  = Jsoup.parse(page);
				Elements house = doc.select("div.houseList dd.info.rel.floatr");
				for(Element ele :house){
				Map<String,String> detail = new HashMap<String, String>();
					
					detail.put("title", jsoupNoError(ele,"p.title a"));
					detail.put("info", jsoupNoError(ele,"p.mt12")); //1
					detail.put("adress",jsoupNoError(ele,"span.iconAdress"));//1
					try{
					detail.put("local", ele.select("p.mt10 a").first().attr("title"));
					}catch(Exception e){
						detail.put("local","");
					}
				
					String size =   jsoupNoError(ele,"div.area.alignR p");
					detail.put("size", size.replaceAll("\\D+?", ""));
					String  danjia  =  jsoupNoError(ele,"p.danjia");
					detail.put("single_price", danjia.replaceAll("\\D+?", ""));
					detail.put("price",  jsoupNoError(ele,"span.price"));
					detail.put("source","房天下");
					detail.put("update",update );				
					list.add(detail);
					
					
					
					System.out.println(detail);
				}	
				MongoDBUtils mongo = new MongoDBUtils();
				mongo.insertManyData("esf_info", list);
				
				
			} catch (Exception e) {
			
				e.printStackTrace();
			}
			
			System.out.println(k + "------------------------------------");
			k += 1;
			Thread.sleep(rand.nextInt(5));
			if (doc.getElementById("PageControl1_hlk_next") == null ||k >100){
				return ;
			}
		}
		
	}
}
