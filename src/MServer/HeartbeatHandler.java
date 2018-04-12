package MServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class HeartbeatHandler implements Runnable {
	MServer MServer;
	private static final Logger logger = Logger.getLogger(MServer.class.getName());
	public HeartbeatHandler(MServer MServer) {
		this.MServer = MServer;
	}
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(MServer.portForHeartbeat);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (true) {
			try {
				InputStream inputStream = null;
				OutputStream outputStream = null;
				ObjectInputStream objectInputStream = null;
				Socket clientSocket = serverSocket.accept();
				objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
				Message message = (Message) objectInputStream.readObject();
				outputStream = clientSocket.getOutputStream();
//				logger.info("Receiving heartbeat message from server.");
				MServer.handleHeartbeat(message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
