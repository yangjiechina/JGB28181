package com.yangjie.JGB28181.media.server;

import com.yangjie.JGB28181.media.server.remux.Observer;

public interface Observable {

	public void subscribe(Observer observer);
}
