package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interface ChatServer
 * 
 * @author Juan José Santos Cambra
 * 
 */
public interface ChatServer {

	/**
	 * startup
	 * 
	 */
	public void startup();

	/**
	 * shutdown
	 * 
	 */
	public void shutdown();

	/**
	 * broadcast
	 * 
	 * @param message mensaje que se retransmitirá
	 */
	public void broadcast(ChatMessage message);

	/**
	 * remove
	 * 
	 * @param id usuario que se eliminará.
	 */
	public void remove(int id);

}
