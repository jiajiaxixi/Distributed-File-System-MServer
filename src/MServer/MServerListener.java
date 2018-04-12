package MServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Logger;

import MServer.Message;

public class MServerListener implements Runnable {
	MServer MServer;
	private static final Logger logger = Logger.getLogger(MServerListener.class.getName());
	public MServerListener(MServer MServer) {
		this.MServer = MServer;
	}
	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(MServer.port);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (true) {
			try {
				InputStream inputStream = null;
				OutputStream outputStream = null;
				ObjectInputStream objectInputStream = null;
				Socket clientSocket = serverSocket.accept();
				outputStream = clientSocket.getOutputStream();
				objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
				Message message = (Message) objectInputStream.readObject();
				if (message.getMessage().equalsIgnoreCase("create")) {
					MServer.handleCreate(message, outputStream);
				} else if (message.getMessage().equalsIgnoreCase("read")) {
					MServer.handleRead(message, outputStream);
				} else if (message.getMessage().equalsIgnoreCase("append")) {
					MServer.handleAppend(message, outputStream);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
	}
}
