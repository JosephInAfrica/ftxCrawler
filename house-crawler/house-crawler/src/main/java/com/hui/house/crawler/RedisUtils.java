package com.hui.house.crawler;

import java.io.InputStream;
import java.util.Properties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class RedisUtils {
	private static JedisPool pool;

	// 静态代码初始化池配置

	static {
		try {
			Properties props = new Properties();
//			InputStream is = new FileInputStream("/config.properties");
			InputStream is = RedisUtils.class.getResourceAsStream("/config.properties");
			props.load(is);

			// System.out.println(props.getProperty("redis.port"));
			// 创建jedis池配置实例
			JedisPoolConfig config = new JedisPoolConfig();
			
			// 设置池配置项值
			
			config.setMaxTotal(Integer.valueOf(props.getProperty("redis.pool.maxActive")));
			
			config.setMaxIdle(Integer.valueOf(props.getProperty("redis.pool.maxIdle")));
			
			config.setMaxWaitMillis(Long.valueOf(props.getProperty("redis.pool.maxWait")));
			
			config.setTestOnBorrow(Boolean.valueOf(props.getProperty("redis.pool.testOnBorrow")));
			
			config.setTestOnReturn(Boolean.valueOf(props.getProperty("redis.pool.testOnReturn")));

			// 根据配置实例化jedis池

			pool = new JedisPool(config, props.getProperty("redis.ip"),
					Integer.valueOf(props.getProperty("redis.port")),
					Integer.valueOf(props.getProperty("redis.timeout")), props.getProperty("redis.password"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/** 获得jedis对象 */
	public static Jedis getJedisObject() {
		return pool.getResource();
	}

	/** 归还jedis对象 */
	public static void recycleJedisOjbect(Jedis jedis) {
		pool.returnResource(jedis);
	}

	public static void main(String[] args) {

		Jedis jedis = getJedisObject();// 获得jedis实例

		// jedis.set("name", "zhuxun");

		System.out.println(jedis.get("name"));
		jedis.del("name");
		System.out.println(jedis.exists("name"));
		recycleJedisOjbect(jedis); // 将 获取的jedis实例对象还回迟中

	}

}