package com.yangjie.JGB28181.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.yangjie.JGB28181.message.config.ConfigProperties;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis操作工具类
 * @author yangjie
 * 2020年3月9日
 */
@Component
@EnableConfigurationProperties(ConfigProperties.class)
public class RedisUtil {

	private static Logger logger = LoggerFactory.getLogger("RedisUtil");


	public static  int EXPIRE;

	private static int PERSISTENCE = -1;

	private static int DEFAULT_DB = 0;

	@Value("${config.heartbeatExpire}")
	public void setHeartbeatDuration(int duration){
		RedisUtil.EXPIRE = duration;
	}
	@Value("${config.redisAddress}")
	public void setRedisAddress (String address){
		redisAddr = address;
	}

	@Value("${config.redisAuth}")
	public void setRedisAuth (String auth){
		redisAuth = auth;
	}
	private static String redisAddr = "";

	private static String redisAuth = ""; 
	private static int port = 6379;
	private static int MAX_TOTAL = 300;
	private static int MAX_IDLE = 200;
	private static int MAX_WAIT = 10000;
	private static int TIMEOUT = 10000;
	private static boolean TEST_ON_BORROW = true;
	private static JedisPool jedisPool;


	public synchronized static Jedis getJedis(){
		try{
			if(jedisPool == null){
				try{
					JedisPoolConfig config = new JedisPoolConfig();
					config.setMaxTotal(MAX_TOTAL);
					config.setMaxIdle(MAX_IDLE);
					config.setMaxWaitMillis(MAX_WAIT);
					config.setTestOnBorrow(TEST_ON_BORROW);
					if(StringUtils.isEmpty(redisAuth)){
						jedisPool = new JedisPool(config,redisAddr,port,TIMEOUT);
					}else {
						jedisPool = new JedisPool(config,redisAddr,port,TIMEOUT,redisAuth);
					}
				}catch (Exception e){
					e.printStackTrace();
				}
			}
			if (jedisPool != null){
				Jedis jedis = jedisPool.getResource();
				return jedis;
			}else {
				return null;
			}
		}catch (Exception e){
			e.printStackTrace();
			logger.error(e.toString());
			return null;
		}
	}

	public static String set(String key,String value){
		return set(key,PERSISTENCE,value,DEFAULT_DB);
	}
	public static String set(String key,int expire,String value){
		return set(key,expire,value,DEFAULT_DB);
	}
	public static String set(String key,String value,int index){
		return set(key,PERSISTENCE,value,index);
	}
	public static String set(String key,int expire,String value,int dbIndex){
		Jedis jedis = getJedis();
		if(jedis == null){
			return null;
		}
		jedis.select(dbIndex);
		String result = null;
		try{
			if(expire != PERSISTENCE){
				result = jedis.setex(key, expire, value);
			}else {
				result = jedis.set(key, value);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			releaseResource(jedis);
		}
		return result;
	}
	public static String get(String key) {
		return get(key,0);
	}
	public static String get(String key,int dbIndex){
		Jedis jedis = getJedis();
		if(jedis == null){
			return null;
		}
		jedis.select(dbIndex);
		String result = null;
		try { 
			result = jedis.get(key);
		} catch (Exception e) {
		} finally {
			releaseResource(jedis);
		}

		return result;
	}
	public static Long expire(String key,int expire){
		return expire(key, expire,DEFAULT_DB);
	}
	public static Long expire(String key,int expire,int dbIndex){
		Jedis jedis = getJedis();
		if(jedis == null){
			return null;
		}
		jedis.select(dbIndex);
		Long result = null;
		try { 
			result = jedis.expire(key, expire);
		}catch(Exception e){

		}finally {
			releaseResource(jedis);
		}
		return result;
	}
	public static boolean checkExist(String key){
		return checkExist(key,DEFAULT_DB);
	}
	public static boolean checkExist(String key,int index){
		Jedis jedis = getJedis();
		if(jedis == null){
			return false;
		}
		jedis.select(index);
		try { 
			return jedis.exists(key);
		} catch (Exception e) {
		} finally {
			releaseResource(jedis);
		}
		return false;
	}
	/**
	 * 释放Jedis
	 * @param jedis
	 */
	public static void releaseResource( Jedis jedis){
		if (jedis != null)  
			jedis.close();  
	}  


}
