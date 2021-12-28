/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package poc.redis.race.service;

/**
 *
 * @author Edward M. Kagan {@literal <}kaganem{@literal @}2pm.tech{@literal >}
 */
public class CacheHelper {
	
	private static JedisCache cache;
	
	public static JedisCache getInstance (){
		return cache;
	}
	
	public static JedisCache init (String strategyOption) {
		if (cache == null) {
//			String strategyOption = 
			JedisCache.ImplementationVariant strategy = JedisCache.ImplementationVariant.BLIND_WRITE;
			switch (strategyOption) {
				case "blind" : {
					strategy = JedisCache.ImplementationVariant.BLIND_WRITE;
					break;
				}
				case "dummy" : {
					strategy = JedisCache.ImplementationVariant.DUMMY_WATCH_AND_WRITE;
					break;
				}
				case "wtw" : {
					strategy = JedisCache.ImplementationVariant.WATCH_TRANSACTION_WRITE;
					break;
				}
				case "cas" : {
					strategy = JedisCache.ImplementationVariant.CHECK_AND_SET;
					break;
				}
				case "pessimistic" : {
					strategy = JedisCache.ImplementationVariant.PESSIMISTIC_LOCK;
					break;
				}
				case "late-cas" : {
					strategy = JedisCache.ImplementationVariant.LATE_CHECK_AND_SET;
					break;
				}
				case "late-pessimistic" : {
					strategy = JedisCache.ImplementationVariant.LATE_PESSIMISTIC_LOCK;
					break;
				}
				default:{
					strategy = JedisCache.ImplementationVariant.BLIND_WRITE;
					break;
				}
			}
//			cache = JedisCache.init("cache", 16379, 200, true, strategy);
			cache = JedisCache.init("cache", 6379, 200, true, strategy);
		}
		return cache;
	}
}
