package MServer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Logger;

public class MServer {
	public final int port = 17000;
	static public final int portForHeartbeat = 16000;
	public Map<String, TreeMap<Integer, Chunk>> fileDict;
	public Map<InetAddress, Boolean> host_Server_Running;
	public Map<InetAddress, Long> last_Time_Update;
	public HashMap<InetAddress, String> serverMap;
	public Thread listener;
	private static final Logger logger = Logger.getLogger(MServer.class.getName());

	public MServer () {
		fileDict = new HashMap<>();
		host_Server_Running = new HashMap<>();
		last_Time_Update = new HashMap<>();
		serverMap = new ServerConfiguration("serverConfiguration.txt").getServerMap();
	}
	public void startup() throws IOException {
		logger.info("Starting Service at port " + port);
		executeHeartbeatHandler();
		ServerSocket serverSocket = new ServerSocket(port);
		InputStream inputStream = null;
		OutputStream outputStream = null;
		ObjectInputStream objectInputStream = null;
		while (true) {
			try {		
				logger.info("Waiting for request");
				// Block until a client connection 
				Socket Socket = serverSocket.accept();
				logger.info("Request received");
				outputStream = Socket.getOutputStream();
				objectInputStream = new ObjectInputStream(Socket.getInputStream());
				Message message = (Message) objectInputStream.readObject();
				System.out.println("message is " + message.getMessage());
				if (message.getMessage().equalsIgnoreCase("create")) {
					this.handleCreate(message, outputStream);
				} else if (message.getMessage().equalsIgnoreCase("read")) {
					this.handleRead(message, outputStream);
				} else if (message.getMessage().equalsIgnoreCase("append")) {
					this.handleAppend(message, outputStream);
				}
			} catch (Exception ex) {
				logger.info("Exception while processing request.");
				ex.printStackTrace();
			}
		} 
	}


	public void executeHeartbeatHandler() {
		Thread heartbeatHandler = new Thread(new HeartbeatHandler(this));
		heartbeatHandler.start();
	}
	//
	//	public void executeClientListener() {
	//		listener = new Thread(new MServerListener(this));
	//		listener.start();
	//	}

	//	public void handleHeartbeat(Message message) {
	//		Map<String, TreeMap<Integer, Chunk>> partfileDict = message.getFileDict();
	//		for (Map.Entry<String, TreeMap<Integer, Chunk>> en : partfileDict.entrySet()) {
	//			TreeMap<Integer, Chunk> fileMap = en.getValue();
	//			TreeMap<Integer, Chunk> megaFileMap = this.fileDict.get(en.getKey());
	//			for (Map.Entry<Integer, Chunk> e : fileMap.entrySet()) {
	//				if (!megaFileMap.containsKey(e.getKey())) {
	//					megaFileMap.put(e.getKey(), e.getValue());
	//				}
	//			}
	//		}
	//	}

	public void handleHeartbeat(Message message) {	
		this.last_Time_Update.put(message.getIp(), System.currentTimeMillis());
		Map<String, TreeMap<Integer, Chunk>> partfileDict = message.getFileDict();
		for (Map.Entry<String, TreeMap<Integer, Chunk>> en : partfileDict.entrySet()) {
			TreeMap<Integer, Chunk> fileMap = en.getValue();
			TreeMap<Integer, Chunk> megaFileMap = this.fileDict.get(en.getKey());
			for (Map.Entry<Integer, Chunk> e : fileMap.entrySet()) {
				megaFileMap.put(e.getKey(), e.getValue());
			}
		}
		for (Map.Entry<InetAddress, Long> i : last_Time_Update.entrySet()) {
			if (System.currentTimeMillis() - i.getValue() > 20000) host_Server_Running.put(i.getKey(), false);
			else host_Server_Running.put(i.getKey(), true);
		}
	}

	public synchronized void handleCreate(Message message, OutputStream outputStream) {
		String fileName = message.getFileName();
		if (!this.fileDict.containsKey(fileName)) {
			this.fileDict.put(fileName, new TreeMap<Integer, Chunk>());
			String host_Server = null;
			Chunk toCreateChunk = null;
			String randomPort = null;
			while (true) {
				List<String> valuesList = new ArrayList<>(this.serverMap.values());
				String randomValue = valuesList.get(new Random().nextInt(valuesList.size()));
				try {
					String randomAddress = randomValue.split(" ")[0];
					randomPort = randomValue.split(" ")[1];
					System.out.println("randomAddress is " + randomAddress);
					if (host_Server_Running.get(InetAddress.getByName(randomAddress))) {
						host_Server = randomAddress;
						break;
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
			try {
				toCreateChunk = new Chunk(0, fileName, InetAddress.getByName(host_Server), 0);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			this.fileDict.get(fileName).put(0, toCreateChunk);
			//Send create request to host server
			try {
				Socket socket = new Socket();
				SocketAddress addr = new InetSocketAddress(host_Server, Integer.parseInt(randomPort));
				socket.connect(addr);
				OutputStream outputStream2 = socket.getOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream2);
				logger.info("Sending 'Create' message to the host server");
				objectOutputStream.writeObject(new Message("create", toCreateChunk));
				outputStream2.flush();
				ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
				Message replyMessage = (Message) objectInputStream.readObject();
				if (!replyMessage.getMessage().equals("OK")) {
				} else {
					logger.info("Succeeding in creating chunk");
					logger.info("Sending response to the client");
					objectOutputStream = new ObjectOutputStream(outputStream);
					logger.info("Sending 'OK' message to the client");
					objectOutputStream.writeObject(new Message("OK"));
					outputStream.flush();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			logger.info("Cannot create file with duplicate name!");
			ObjectOutputStream objectOutputStream;
			try {
				objectOutputStream = new ObjectOutputStream(outputStream);
				logger.info("Sending response to the client");
				objectOutputStream.writeObject(new Message("Cannot create file with duplicate name!"));
				outputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void handleRead(Message message, OutputStream outputStream) {
		String fileName = message.getFileName();
		if (this.fileDict.containsKey(fileName)) {
			TreeMap<Integer, Chunk> fileMap = this.fileDict.get(fileName);
			Chunk toReadChunk = fileMap.getOrDefault(message.getIndexOfFile(), null);
			if (toReadChunk == null) {
				logger.info(fileName + " doesn't have that much index to read!");
				//Send read response to client
				try {
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
					logger.info("Sending 'Read' response to the client");
					objectOutputStream.writeObject(new Message("read response", toReadChunk));
					outputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (!host_Server_Running.get(toReadChunk.host_Server)) {
				try {
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
					logger.info("The server trying to read from is down");
					objectOutputStream.writeObject(new Message("Server is down!"));
					outputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				//Send read response to client
				try {
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
					logger.info("Sending 'Read' response to the client");
					objectOutputStream.writeObject(new Message("read response", toReadChunk));
					outputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			logger.info("Cannot read non-existent file!");
			ObjectOutputStream objectOutputStream;
			try {
				objectOutputStream = new ObjectOutputStream(outputStream);
				logger.info("Sending response to the client");
				objectOutputStream.writeObject(new Message("Cannot read non-existent file!"));
				outputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	} 


	public synchronized void handleAppend(Message message, OutputStream outputStream) {
		String fileName = message.getFileName();
		if (this.fileDict.containsKey(fileName)) {
			TreeMap<Integer, Chunk> fileMap = this.fileDict.get(fileName);
			Chunk toAppendChunk = fileMap.get(fileMap.lastKey());
			System.out.println(message.getAppendSize());
			System.out.println(toAppendChunk.size);
			if (!host_Server_Running.get(toAppendChunk.host_Server)) {
				try {
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
					logger.info("The server trying to read from is down");
					objectOutputStream.writeObject(new Message("Server is down!"));
					outputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (message.getAppendSize() + toAppendChunk.size <= 8192) {
				//Send append response to client
				try {
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
					logger.info("Sending 'Append' response to the client");
					objectOutputStream.writeObject(new Message("append response", toAppendChunk));
					outputStream.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				//Send request to tail of the file in host server to pad the chunk
				//Send create request to a random host server to create a new chunk
				String host_Server;
				String randomPort = null;
				while (true) {
					List<String> valuesList = new ArrayList<>(this.serverMap.values());
					String randomValue = valuesList.get(new Random().nextInt(valuesList.size()));
					String randomAddress = randomValue.split(" ")[0];
					randomPort = randomValue.split(" ")[1];
					System.out.println("randomAddress is " + randomAddress);
					try {
						if (host_Server_Running.get(InetAddress.getByName(randomAddress))) {
							host_Server = randomValue.split(" ")[0];
							break;
						}
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
				}
				Chunk toCreateChunk = null;
				try {
					toCreateChunk = new Chunk(toAppendChunk.index + 1, fileName, InetAddress.getByName(host_Server), 0);
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
				this.fileDict.get(fileName).put(toCreateChunk.index, toCreateChunk);
				//Send create request to host server
				try {
					Socket socket = new Socket();
					SocketAddress addr = new InetSocketAddress(host_Server, Integer.parseInt(randomPort));
					socket.connect(addr);
					OutputStream outputStream2 = socket.getOutputStream();
					ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream2);
					logger.info("Sending 'Create' message to the host server");
					objectOutputStream.writeObject(new Message("create", toCreateChunk));
					outputStream2.flush();
					ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
					Message replyMessage = (Message) objectInputStream.readObject();
					if (!replyMessage.getMessage().equals("OK")) {
					} else {
						logger.info("Succeeding in creating chunk for oversize information.");
						logger.info("Sending response to the client");
						//Send read response to client
						try {
							objectOutputStream = new ObjectOutputStream(outputStream);
							logger.info("Sending 'Append' response to the client");
							objectOutputStream.writeObject(new Message("append response", toCreateChunk));
							outputStream.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} else {
			logger.info("Cannot append non-existent file!");
			ObjectOutputStream objectOutputStream;
			try {
				objectOutputStream = new ObjectOutputStream(outputStream);
				logger.info("Sending response to the client");
				objectOutputStream.writeObject(new Message("Cannot append non-existent file!"));
				outputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[]) {
		// Start the server
		MServer MServer = new MServer();
		try {
			MServer.startup();
		}
		catch (IOException ex) {
			logger.info("Unable to start MServer. " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
