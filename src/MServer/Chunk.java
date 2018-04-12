package MServer;

import java.io.Serializable;
import java.net.InetAddress;

public class Chunk implements Serializable{

	private static final long serialVersionUID = 1L;
	public int index;
	public String name;
	public InetAddress host_Server;
	public int size;
	public String ChunkName;

	public Chunk(int index, String name, InetAddress host_Server, int size) {
		this.index = index;
		this.name = name;
		this.host_Server = host_Server;
		this.size = size;
	}
}
