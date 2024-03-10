package es.ubu.lsi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Clase ChatClientImpl Gestiona las conexión y envío de mensajes al servidor
 * 
 * @author Juan José Santos Cambra
 * 
 */
public class ChatClientImpl implements ChatClient {

	/**
	 * Para registro de horas
	 */
	private static SimpleDateFormat sdf;

	/**
	 * Puerto por defecto
	 */
	private static final int DEFAULT_PORT = 1500;

	/**
	 * Ip del servidor
	 */
	private String server;

	/**
	 * Nickname
	 */
	private String username;

	/**
	 * Puerto del servidor
	 */
	private int port;

	/**
	 * Booleano para continuar leyendo
	 */
	private boolean carryOn;

	/**
	 * Id del cliente
	 */
	private int id;

	/**
	 * Objeto socket
	 */
	private Socket _socket;

	/**
	 * Output stream para envío de datos a servidor
	 */
	ObjectOutputStream _os;

	/**
	 * Salida de programa con error
	 */
	private static final int EXIT_NOK = 1;

	/**
	 * Texto de logout
	 */
	private static final String LOGOUT_TEXT = "logout";

	/**
	 * Main Punto de entrada de la aplicación, recupera los parámetros y arranca el
	 * cliente
	 * 
	 * @param args 1 solo parámetro, username. 2 parámetros, server y username.
	 */
	public static void main(String[] args) {
		// Valores por defecto
		String username = "";
		String server = "localhost";

		// Si solo nos pasan 1 argumento
		if (args.length == 1) {
			username = args[0];
		}

		// Si nos pasan 2 argumentos
		if (args.length == 2) {
			server = args[0];
			username = args[1];
		}

		// Se instancia el cliente
		ChatClientImpl chatClientImpl = new ChatClientImpl(server, DEFAULT_PORT, username);

		// Se arranca el cliente
		chatClientImpl.start();
	}

	/**
	 * Constructor de la clase
	 * 
	 * @param server:   Dirección del servidor
	 * @param port:     Puerto donde escuchara el servidor
	 * @param username: Nickname del usuario
	 */
	public ChatClientImpl(String server, int port, String username) {
		this.server = server;
		this.username = username;
		this.port = port;
		this.carryOn = true;
		sdf = new SimpleDateFormat("HH:mm:ss");
		this.id = username.hashCode();
		try {
			// Creamos el socket para conectar con el cliente
			this._socket = new Socket(this.server, this.port);

			// Obtenemos el output strem para enviar mensajes
			_os = new ObjectOutputStream(_socket.getOutputStream());
		} catch (IOException e) {
			System.err.println("[" + sdf.format(new Date()) + "]" + " - Error creating client.");
			System.exit(EXIT_NOK);
		}
	}

	/**
	 * start lanza el hilo de escucha del cliente y lee el input del usuario para
	 * enviar los mensajes
	 * 
	 * @return true if all ok
	 */
	@Override
	public boolean start() {
		// Thread para empezar a escuchar los mensajes
		new Thread(new ChatClientListener(_socket)).start();

		// Mensaje con el username para logearse
		sendMessage(new ChatMessage(id, MessageType.MESSAGE, username));

		// Escaner para leer el teclado
		Scanner scn = new Scanner(System.in);

		// Entrada del usuario
		String input;

		// Ciclo principal
		while (carryOn) {

			// Leemos la siguiente línea
			input = scn.nextLine();

			// Si el input del usuario es para hacer logout cambiamos el tipo de mensaje y
			// mandamos desconectar
			if (input.contains(LOGOUT_TEXT)) {
				sendMessage(new ChatMessage(id, MessageType.LOGOUT, (input)));
				disconnect();
				return true;
			}

			// Enviamos el mensaje actual
			sendMessage(new ChatMessage(id, MessageType.MESSAGE, (input)));
		}

		return true;
	}

	/**
	 * sendMessage Envía un mensaje al servidor
	 * 
	 * @param msg: Mensaje a enviar
	 */
	@Override
	public void sendMessage(ChatMessage msg) {
		try {
			// Envia el mensaje por el canal de salida
			_os.writeObject(msg);
		} catch (IOException e) {
			System.err.println("[" + sdf.format(new Date()) + "] - Error sending message");
		}
	}

	/**
	 * disconnect Para el ciclo de envío y cierra el socket
	 * 
	 */
	@Override
	public void disconnect() {
		carryOn = false;
		try {
			_socket.close();
		} catch (IOException e) {
			System.err.println("[" + sdf.format(new Date()) + "] - Error disconnecting socket");
		}
	}

	/**
	 * Clase ChatClientListener gestiona la recepción de mensajes del cliente
	 * 
	 * @author Juan José Santos Cambra
	 */
	class ChatClientListener implements Runnable {

		/**
		 * Input stream de la instancia del hilo
		 */
		private ObjectInputStream _is;

		/**
		 * Constructor
		 * 
		 * @param clientSocket: Objeto socket del cliente que conecta con el servidor
		 */
		public ChatClientListener(Socket clientSocket) {
			try {
				// Input y output streams
				this._is = new ObjectInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
				System.err.println("[" + sdf.format(new Date()) + "] - Error creating listener.");
			}
		}

		/**
		 * run función run que recepciona e imprime los mensajes del servidor
		 * 
		 */
		public void run() {
			while (carryOn) {
				try {
					System.out.println((((ChatMessage) _is.readObject()).getMessage()));
				} catch (IOException | ClassNotFoundException e) {
					System.err.println("[" + sdf.format(new Date()) + "] - Disconnected from server. Exiting...");
					disconnect();
					System.exit(EXIT_NOK);
				}
			}
		}
	}

}
