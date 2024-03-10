package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interface ChatClient
 * 
 * @author Juan José Santos Cambra
 * 
 */

public interface ChatClient {

	/**
	 * start
	 * 
	 * @return true si arranca, false si no arranca
	 */
	public boolean start();

	/**
	 * sendMessage
	 * 
	 * @param msg: Mensaje a enviar
	 */
	public void sendMessage(ChatMessage msg);

	/**
	 * disconnect
	 */
	public void disconnect();
}
