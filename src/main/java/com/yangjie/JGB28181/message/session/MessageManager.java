package com.yangjie.JGB28181.message.session;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageManager {


	private Map<String, SyncFuture<?>> map = new ConcurrentHashMap<>();

	private static  MessageManager messageManager;

	public static MessageManager getInstance(){
		if(messageManager == null){
			synchronized ("") {
				if(messageManager == null){
					messageManager = new MessageManager();
				}
			}
		}
		return messageManager;
	}
	private MessageManager(){

	}
	public SyncFuture<?> receive(String key) {
		SyncFuture<?> future = new SyncFuture();
		map.put(key, future);
		return future;
	}

	public void remove(String key) {
		map.remove(key);
	}
	
	public void put(String key, Object value) {
		SyncFuture syncFuture = map.get(key);
		if (syncFuture == null) {
			return;
		}
		syncFuture.setResponse(value);
	}

}
