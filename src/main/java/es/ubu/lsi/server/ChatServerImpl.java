package es.ubu.lsi.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Clase ChatServerImpl Gestiona las conexiones y retransmisiones de los
 * distintos clientes
 * 
 * @author Juan José Santos Cambra
 * 
 */
public class ChatServerImpl implements ChatServer {

	/**
	 * Puerto por defecto
	 */
	private static final int DEFAULT_PORT = 1500;

	/**
	 * Total de clientes
	 */
	private static int clientId;

	/**
	 * Para registro de horas
	 */
	private static SimpleDateFormat sdf;

	/**
	 * Puerto del servidor
	 */
	private int port;

	/**
	 * Booleano para continuar aceptando conexiones
	 */
	private boolean alive;

	/**
	 * Mapa con nickname y su thread asociado
	 */
	Map<String, ServerThreadForClient> _clientsThreads;

	/**
	 * Mapa con id de cliente y su nickname asociado
	 */
	Map<Integer, String> _clientsIds;

	/**
	 * Hashset de clientes baneados
	 */
	HashSet<String> _clientsBanned;

	/**
	 * Socket del server
	 */
	ServerSocket server;

	/**
	 * Salida del programa con error
	 */
	private static final int EXIT_NOK = 1;

	/**
	 * Texto para banear
	 */
	private static final String BAN_TEXT = "ban";

	/**
	 * Texto para desbanear
	 */
	private static final String UNBAN_TEXT = "unban";

	/**
	 * Main Punto de entrada de la aplicación, inicia servidor
	 * 
	 * @param args no recibe nada
	 */
	public static void main(String[] args) {
		// Se instancia el servidor
		ChatServerImpl chatServerImpl = new ChatServerImpl();
		// Se arranca el servidor
		chatServerImpl.startup();
	}

	/**
	 * Constructor de la clase
	 * 
	 */
	public ChatServerImpl() {
		this(DEFAULT_PORT);
	}

	/**
	 * Constructor de la clase
	 * 
	 * @param port: Puerto donde escuchará el servidor
	 */
	public ChatServerImpl(int port) {
		// Iniciamos los mapas
		this._clientsThreads = new HashMap<String, ServerThreadForClient>();
		this._clientsBanned = new HashSet<String>();
		this._clientsIds = new HashMap<Integer, String>();
		this.port = port;
		sdf = new SimpleDateFormat("HH:mm:ss");
		try {
			// Creamos el servidor de escucha en el puerto indicado
			this.server = new ServerSocket(this.port);
		} catch (IOException e) {
			System.err.println("[" + sdf.format(new Date()) + "]" + " - Error creating socket server. Shutting down.");
			System.exit(EXIT_NOK);
		}
	}

	/**
	 * startup Comienza la aceptación de clientes
	 * 
	 */
	@Override
	public void startup() {
		Socket clientSocket;
		alive = true;
		System.out.println("Server started, port: " + DEFAULT_PORT);

		while (alive) {
			try {
				// Aceptamos las conexiones de los clientes
				clientSocket = server.accept();
				clientId++;
				System.out.println("[" + sdf.format(new Date()) + "]" + " - New Client: "
						+ clientSocket.getInetAddress() + "/" + clientSocket.getPort() + " total clients " + clientId);

				// Iniciamos el thread para el cliente
				new ServerThreadForClient(clientSocket).start();
			} catch (Exception e) {
				alive = false;
				System.err
						.println("[" + sdf.format(new Date()) + "]" + " - Error accepting connections. Shutting down.");
			}
		}
	}

	/**
	 * shutdown Cierra el servidor
	 * 
	 */
	@Override
	public void shutdown() {
		try {

			if (server != null) {
				server.close();
			}
		} catch (IOException e) {
			System.err.println("[" + sdf.format(new Date()) + "]" + " - Error shutting down socket server.");
		}
	}

	/**
	 * broadcast emite un mensaje a todos los clientes que no estén baneados
	 * 
	 */
	@Override
	public void broadcast(ChatMessage message) {
		message.setMessage(
				"[" + sdf.format(new Date()) + "] - " + _clientsIds.get(message.getId()) + ": " + message.getMessage());
		for (Map.Entry<String, ServerThreadForClient> entry : _clientsThreads.entrySet()) {
			try {
				// Comprobamos si el usuario está baneado para no enviarle el mensaje
				if (!_clientsBanned.contains(entry.getValue().username)) {
					// Envía el mensaje
					entry.getValue()._os.writeObject(message);
				}
			} catch (IOException e) {
				System.err.println("[" + sdf.format(new Date()) + "]" + " - Error sending message to client "
						+ entry.getValue().username);
				remove(message.getId());
			}
		}
	}

	/**
	 * Elimina a un cliente
	 * 
	 */
	@Override
	public void remove(int id) {
		// Restamos del total de clientes
		clientId--;
		try {

			if (_clientsThreads.get((_clientsIds.get(id)))._clientSocket != null) {
				// Cerramos el socket
				_clientsThreads.get((_clientsIds.get(id)))._clientSocket.close();
			}

			// Lo quitamos del mapa de threads
			_clientsThreads.remove((_clientsIds.get(id)));

			// Lo quitamos del set de baneados
			_clientsBanned.remove((_clientsIds.get(id)));

			// Lo quitamos del mapa de ids
			_clientsIds.remove(id);

			System.out.println("[" + sdf.format(new Date()) + "]" + " - Remaining clients " + clientId);
		} catch (IOException e) {
			System.err.println("[" + sdf.format(new Date()) + "]" + " - Error removing clientid: " + id);
		}
	}

	/**
	 * Clase ServerThreadForClient Gestiona la recepción de mensajes de un cliente
	 * 
	 * @author Juan José Santos Cambra
	 */
	class ServerThreadForClient extends Thread {

		/**
		 * Id del usuario asociado a la instancia del hilo
		 */
		private int id;

		/**
		 * Nickname del usuario asociado a la instancia del hilo
		 */
		private String username;

		/**
		 * Client socket de la instancia del hilo
		 */
		private Socket _clientSocket;

		/**
		 * Input stream del socket del hilo
		 */
		private ObjectInputStream _is;

		/**
		 * Output stream del socket del hilo
		 */
		private ObjectOutputStream _os;

		/**
		 * Variable para gestionar la vida del hilo
		 */
		private boolean _killThread;

		/**
		 * Constructor de la clase
		 * 
		 * @param clientSocket: Objeto socket del cliente conectado
		 */
		public ServerThreadForClient(Socket clientSocket) {
			this._clientSocket = clientSocket;
			this._killThread = false;

			try {
				// Input y output streams
				this._is = new ObjectInputStream(clientSocket.getInputStream());
				this._os = new ObjectOutputStream(clientSocket.getOutputStream());
				// Leemos el mensaje de bienvenida del usuario
				ChatMessage welcome = (ChatMessage) _is.readObject();
				// Asignamos los datos que vienen en el mensaje
				this.username = welcome.getMessage();
				this.id = welcome.getId();

				// Añadimos y damos bienvenida al usuario
				welcomeUser(username, id);

			} catch (IOException | ClassNotFoundException e) {
				System.err.println(
						"[" + sdf.format(new Date()) + "]" + " - Error creating thread for client: " + username);
			}
			System.out.println("[" + sdf.format(new Date()) + "]" + " - Created client thread for client: " + username);
		}

		/**
		 * Método run del thread ejecuta la espera de mensajes
		 * 
		 */
		@Override
		public void run() {
			while (!_killThread) {
				waitMessages();
			}
		}

		/**
		 * welcomeUser registra el usuario en los mapas y le da un mensaje de bienvenida
		 * 
		 * @param username: Nombre del usuario
		 * @param id:       Id del usuario
		 */
		private void welcomeUser(String username, int id) {
			// Añadimos el usuario a los mapas
			_clientsThreads.put(username, this);
			_clientsIds.put(id, username);

			try {
				// Emitimos un mensaje de bienvenida solo a este usuario
				_os.writeObject(new ChatMessage(0, MessageType.MESSAGE,
						("[" + sdf.format(new Date()) + "] - " + username + " welcome to the SISDIS chat.")));
			} catch (IOException e) {
				System.err.println("[" + sdf.format(new Date()) + "]" + " - Error welcoming user " + username);
				logout();
			}
		}

		/**
		 * waitMessages recepciona los menasjes del cliente y ejecuta sus acciones
		 * 
		 */
		private void waitMessages() {
			try {

				// Leemos del input stream
				ChatMessage clientMessage = (ChatMessage) _is.readObject();

				// Comprobamos si el usuario está baneado
				if (_clientsBanned.contains(username)) {
					// Emitimos un mensaje de advertencia
					_os.writeObject(new ChatMessage(0, MessageType.MESSAGE,
							("[" + sdf.format(new Date()) + "] - " + username + " you are banned.")));
					return;
				}

				// Comprobamos el tipo de mensaje recibido
				switch (clientMessage.getType()) {
				case MESSAGE:

					// Obtenemos el texto del mensaje
					String msgText = clientMessage.getMessage();

					// Comprobamos si es de desbaneo
					if (msgText.contains(UNBAN_TEXT)) {
						// Obtenemos el usuario a desbanear
						String unbannedUser = msgText.replace(UNBAN_TEXT, "");
						unbannedUser = unbannedUser.trim();
						_clientsBanned.remove(unbannedUser);
						broadcast(new ChatMessage(clientMessage.getId(), MessageType.MESSAGE,
								"has unbanned " + unbannedUser));
					}
					// Comprobamos si es un mensaje de tipo baneo
					else if (msgText.contains(BAN_TEXT)) {
						// Obtenemos el usuario a banear
						String bannedUser = msgText.replace(BAN_TEXT, "");
						bannedUser = bannedUser.trim();
						broadcast(new ChatMessage(clientMessage.getId(), MessageType.MESSAGE,
								"has banned " + bannedUser));
						_clientsBanned.add(bannedUser);

					} else {
						// Retransmitimos el mensaje
						broadcast(clientMessage);
					}
					break;
				case LOGOUT:
					logout();
					break;
				default:
					break;
				}
			} catch (IOException | ClassNotFoundException | NullPointerException e) {
				System.err.println(
						"[" + sdf.format(new Date()) + "]" + " - Error waiting messages from client " + username);
				logout();
			}
		}

		/**
		 * logout desloggea al usuario
		 * 
		 */
		private void logout() {
			_killThread = true;
			System.out.println("[" + sdf.format(new Date()) + "] - Client " + username + " logged out");
			remove(this.id);
		}

	}
}
