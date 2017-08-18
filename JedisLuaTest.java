package com.tencent.jungle.test.api;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisApiTest {
	public static JedisPool init() {
		JedisPoolConfig redisConfig = new JedisPoolConfig();
		redisConfig.setMaxWaitMillis(1000L * 10);
		redisConfig.setTestOnBorrow(true);
		JedisPoolConfig config = new JedisPoolConfig();
		// ssh -p 6666 -f -N -g -L 19001:10.100.70.15:19000 root@172.27.192.188
		JedisPool pool = new JedisPool(config, "10.125.40.37", 19001, Protocol.DEFAULT_TIMEOUT, "mqq2015");
		config.setMaxWaitMillis(1000);
		config.setTestOnBorrow(true);
		return pool;
		// 如果不存在,则初始化0,如果存在,score达到阈值,则返回score,如果score未达到阈值,则清0
		/*
		 * System.out.println(jedis.eval("    "// + "local score = redis.call('zscore', KEYS[1], ARGV[1]);"// + "	   if(not score) then "// +++++++++++++++++++++++++++++//key不存在 +
		 * "       redis.call('zadd', KEYS[1], 0, ARGV[1] )"// +++++// + "       return -1 "// +++++++++++++++++++++++++++++++++++// + "    end; " // ++++++++++++++++++++++++++++++++++++++++++// +
		 * "score = tonumber(score)" + "local threshold = tonumber(ARGV[2]);"// + "    if (score >= threshold) then "// +++++++++//key对应的score是否达到阈值 + "      return score"//
		 * ++++++++++++++++++++++++++++++++++// + "    else "// +++++++++++++++++++++++++++++++++++++++++++// + "      return 0; "// ++++++++++++++++++++++++++++++++++++// + "    end; ",//
		 * ++++++++++++++++++++++++++++++++++++++++++// Arrays.asList("_test_luaxxxx"), Arrays.asList("_member", "2")));
		 */

	}

	/** 测试并发安全 */
	public static void doLuatestThread(final JedisPool jedisPool) {
		for (int i = 0; i < 10; i++) {
			new Thread() {
				public void run() {
					Jedis jedis = jedisPool.getResource();
					try {
						for (int i = 0; i < 10000; i++) {
							System.out.println((Long) jedis.eval(""// return 0表示减动作不生效,1表示减动作成功
									+ "local score = redis.call('get', KEYS[1]);"//
									+ "if(not score) then "// step1) key不存在,则不做减操作
									+ "   redis.call('set', KEYS[1], 0);"//
									+ "   return 0;"//
									+ "end; " //
									+ "score = tonumber(score);"//
									+ "local threshold = tonumber(ARGV[1]);"//
									+ "local substract = tonumber(ARGV[2]);"//
									+ "if (score == 0) then "// 状态1) 如果分值已达0,则不生效
									+ "   return 0;"//
									+ "elseif (score >= threshold) then "// 状态2) 如果分值已达到阈值,则不做操作(狼已死)
									+ "   return 0;"//
									+ "elseif (score >= substract) then"// 状态3) 分值够减
									+ "   redis.call('decrby', KEYS[1], substract);"//
									+ "   return 1; " //
									+ "else"// 状态4) 分值不够减,则置为0
									+ "   redis.call('set', KEYS[1], 0);"//
									+ "   return 1;" //
									+ "end; "//
							, Arrays.asList("__member6"), Arrays.asList("10001"/* 阈值 */, String.valueOf("1")/* 减的分值 */)));
						}
					} finally {
						if (jedis != null) {
							jedis.close();
						}
					}
				}
			}.start();
		}
	}

	/** 测试lua */
	public static void doLuatest(final JedisPool jedisPool) {
		Jedis jedis = jedisPool.getResource();
		try {
			System.out.println((Long) jedis.eval(""// return 0,表示狼复活,否则表示狼已死
					+ "local score = redis.call('get', KEYS[1]);"//
					+ "	   if(not score) then "// step1) key不存在,则初始化key
					+ "       redis.call('set', KEYS[1], 0)"//
					+ "       return 0 "//
					+ "    end; " //
					+ "score = tonumber(score)"//
					+ "local threshold = tonumber(ARGV[1]);"//
					+ "    if (score >= threshold) then "// step2) key达到阈值(狼已死)
					+ "      return score"//
					+ "    else "//
					+ "      redis.call('set', KEYS[1], 0)"// step3) key未达到阈值,则复活
					+ "      return 0;" //
					+ "    end; "//
			, Arrays.asList("__member3"), Arrays.asList("10000")));
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	/** 测试lua */
	public static void doLuatest2(final JedisPool jedisPool) {
		Jedis jedis = jedisPool.getResource();
		try {
			System.out.println((Long) jedis.eval(""// return 0表示减动作不生效,1表示减动作成功
					+ "local score = redis.call('get', KEYS[1]);"//
					+ "if(not score) then "// step1) key不存在,则不做减操作
					+ "   redis.call('set', KEYS[1], 0);"//
					+ "   return 0;"//
					+ "end; " //
					+ "score = tonumber(score);"//
					+ "local threshold = tonumber(ARGV[1]);"//
					+ "local substract = tonumber(ARGV[2]);"//
					+ "if (score == 0) then "// 状态1) 如果分值已达0,则不生效
					+ "   return 0;"//
					+ "elseif (score >= threshold) then "// 状态2) 如果分值已达到阈值,则不做操作(狼已死)
					+ "   return 0;"//
					+ "elseif (score >= substract) then"// 状态3) 分值够减
					+ "   redis.call('decrby', KEYS[1], substract);"//
					+ "   return 1; " //
					+ "else"// 状态4) 分值不够减,则置为0
					+ "   redis.call('set', KEYS[1], 0);"//
					+ "   return 1;" //
					+ "end; "//
			, Arrays.asList("__member6"), Arrays.asList("10001"/* 阈值 */, String.valueOf("1")/* 减的分值 */)));
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}
	//ssh -p 6666 -f -N -g -L 19001:10.100.70.15:19000 root@172.27.192.188
	public static void main(String[] args) throws ParseException {
		System.out.println("hello,world");
		// System.out.println(DateUtils.parseDefaultFormatDate("2017-05-31 00:00:00").getTime());
		/*
		 * JedisPool jedisPool = RedisApiTest.init(); doLuatestThread(jedisPool);
		 */
		// System.out.println(String.format("/zk/qconn/pub_config/huayang/dolphin_act/%s/%s/%s",5,6,7));
		/*
		 * for (int i = 1; i < 300; i++) { JedisPool jedisPool = RedisApiTest.init(); Jedis jedis = jedisPool.getResource(); jedis.zadd("dolphin_rank_144002_21000_3001_1_0", i * 100, "memberId" + i);
		 * }
		 */
		/*
		 * JedisPool jedisPool = RedisApiTest.init(); Jedis jedis = jedisPool.getResource(); String script = ""// return 1,表示生效 0 表示未生效 + " local key = KEYS[1]"// + " local rankMember = KEYS[2]"// +
		 * " local score = redis.call('zscore', key, rankMember)"// + " local updateNum = tonumber(ARGV[1])"// + " if(not score) then "// step1) key不存在,则初始化key + "     return 0 "// + " end " // +
		 * " score = tonumber(score)"// + " if(updateNum >= 0) then "// 负数不做操作 + "     return tonumber(redis.call('zscore', key, rankMember))"// + " end "// + " if(-updateNum >= score) then "// step3)
		 * //分值不够减 + "     redis.call('zadd', key, 0, rankMember )"// + "     return 0"// + " else "// + "  	redis.call('zincrby', key, updateNum, rankMember)"// +
		 * "     return tonumber(redis.call('zscore', key, rankMember))" + " end ";// System.out.println((Long)jedis.eval(script, Arrays.asList("__jackietest2", "member1"),
		 * Arrays.asList(String.valueOf(-1)))); ;
		 */

		JedisPool jedisPool = RedisApiTest.init();
		Jedis jedis = jedisPool.getResource();
/*		String script = ""// return 1,表示生效 0 表示未生效
				+ " local key = KEYS[1]"//
				+ " local rankMember = KEYS[2]"//
				+ " local updateNum = tonumber(ARGV[1])"//
				+ "  	redis.call('zincrby', key, updateNum, rankMember)"//
				+ "  return redis.call('zrevrange', key, 0, 100, 'withscores')";*/
	
	/*	Object x = ((List)jedis.eval(script, Arrays.asList("__jackietest2", "member1"), Arrays.asList(String.valueOf(11))));
		System.out.println(x);*/
		/*for(int i = 0; i < 100; i ++){
			jedis.zadd("match_rank", 1000 - i, "m_"+i);
		}*/
		String script = ""// return 1,表示生效 0 表示未生效
				+ " local key = KEYS[1]"//
				+ " local rankMember = KEYS[2]"//
				+ "  	redis.call('zincrby', key, math.random(), rankMember)"//
				+ "  return redis.call('zscore', key, rankMember)";
		for(int i = 0 ; i < 100; i ++){
			System.out.println(jedis.eval(script, Arrays.asList("__test", "member1"), Arrays.asList(String.valueOf(11))));
		}
	}
}
