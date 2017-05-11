package com.hui.house.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

/**
 * 
 * @author yulong.pan
 * 
 */
public class MongoDBUtils {
	private String url  ;
	private String dataBase;
	private String auth;
	private String name;
	private String password;
	// 线程安全的, mongo是线程不安全的
	private MongoClient mongoClient = null;
	
	
	MongoDBUtils() throws IOException{
		 Properties props = new Properties();
	      InputStream config = this.getClass().getResourceAsStream("/config.properties");
	      props.load(config);
	      this.url = props.getProperty("mongodb.url");
	      this.dataBase = props.getProperty("mongodb.database");
	      this.auth =props.getProperty("mogodb.auth");
	      this.name = props.getProperty("mongodb.name");
	      this.password =props.getProperty("mongodb.passwd");
	      System.out.println(url);
	}
	
	
	//获取mongo客户端
	public synchronized MongoClient getMongoClient(String mongodbAddress) {
		if (null == mongoClient) {
			 init(mongodbAddress);
		}
		return mongoClient;
	}

	/**
	 * 集群初始化
	 * @param mongodbAddress mongo的地址
	 */
	private void init(String mongodbAddress) {
		if (mongoClient == null) {
			MongoClientOptions.Builder build = new MongoClientOptions.Builder();
			build.connectionsPerHost(50); // 与目标数据库能够建立的最大connection数量为50
			// build.autoConnectRetry(true); // 自动重连数据库启动
			build.threadsAllowedToBlockForConnectionMultiplier(50); // 如果当前所有的connection都在使用中，则每个connection上可以有50个线程排队等待
			/*
			 * 一个线程访问数据库的时候，在成功获取到一个可用数据库连接之前的最长等待时间为2分钟
			 * 这里比较危险，如果超过maxWaitTime都没有获取到这个连接的话，该线程就会抛出Exception
			 * 故这里设置的maxWaitTime应该足够大，以免由于排队线程过多造成的数据库访问失败
			 */
			build.maxWaitTime(1000 * 60 * 2);
			build.connectTimeout(1000 * 60); // 与数据库建立连接的timeout设置为1分钟
			MongoClientOptions myOptions = build.build();
			try {
				if (!StringUtils.isBlank(mongodbAddress)) {
					String[] address = mongodbAddress.split(",");
					List<ServerAddress> list = new ArrayList<ServerAddress>();
					for (String addr : address) {
						list.add(new ServerAddress(addr));
					}
					 MongoCredential credential = MongoCredential.createScramSha1Credential( name, auth, password.toCharArray());  
			         List<MongoCredential> credentials = new ArrayList<MongoCredential>();  
			         credentials.add(credential);
					// 数据库连接实例
					mongoClient = new MongoClient(list, credentials ,myOptions);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	 
	/**
	 *
	 * @param tableName 插入的表明
	 * @param query mongdb查询的条件
	 * @param json 要插入的数据
	 */
	public void upsertObject(String tableName, String query,JSONObject json){
		try {
			MongoClient mongoClient = getMongoClient(url);
			//获取mongodb的数据库
			MongoDatabase mongoDatabase = mongoClient.getDatabase(dataBase);
			//获取mongodb的数据库中的表
			MongoCollection mongoCollection = mongoDatabase.getCollection(tableName);
			Document querry_id = new Document();
			querry_id.put("_id",query);

			json.put("insert_time",System.currentTimeMillis());
			Document m_documet = new Document("$set", Document.parse(json.toJSONString()));
			mongoCollection.updateOne(querry_id,m_documet,new UpdateOptions().upsert(true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void insertOneData(String tableName, Map map){
		try{
			MongoClient mongoClient = getMongoClient(url);
			MongoDatabase mongoDatabase = mongoClient.getDatabase(dataBase);	
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(tableName);
			String json  = JSON.toJSONString(map);
			mongoCollection.insertOne(Document.parse(json));
		}catch(Exception e){
			System.out.println("插入数据失败");
			e.printStackTrace();
		}finally{
			mongoClient.close();
			mongoClient =null;
		}
	}
	public void insertManyData(String tableName,List<? extends Map> list){
		try{
			MongoClient mongoClient = getMongoClient(url);		
			MongoDatabase mongoDatabase = mongoClient.getDatabase(dataBase);
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(tableName);
			List<Document> docList = new ArrayList<Document>();
			for(Map  map : list){
				String json  = JSON.toJSONString(map);
				docList.add(Document.parse(json));
			}
			mongoCollection.insertMany(docList);			
		}catch(Exception e){
			System.out.println("插入数据失败");
			e.printStackTrace();
		}finally{
			mongoClient.close();
			mongoClient =null;
		}
	}
	
	
	
}
