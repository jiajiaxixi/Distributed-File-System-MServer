package MServer;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Message implements Serializable{
	private static final long serialVersionUID = 1L;
	private String message;
	private Map<String, TreeMap<Integer, Chunk>> fileDict;
	private String fileName;
	private Chunk chunk;
	private int appendSize;
	private int indexOfFile;
	private String toAppendContent;
	private InetAddress ip;
	public Message(String message, Map<String, TreeMap<Integer, Chunk>> fileDict, InetAddress ip) {
		this.setMessage(message);
		this.setFileDict(fileDict);
		this.setIp(ip);
	}
	public Message(String message, String fileName) {
		this.setMessage(message);
		this.setFileName(fileName);
	}
	public Message(String message, Chunk chunk) {
		this.setMessage(message);
		this.setChunk(chunk);
	}
	public Message(String message, String fileName, int appendSize) {
		this.setMessage(message);
		this.setFileName(fileName);
		this.setAppendSize(appendSize);
	}
	public Message(String message, String fileName, int appendSize, int indexOfFile) {
		this.setMessage(message);
		this.setFileName(fileName);
		this.setAppendSize(appendSize);
		this.setIndexOfFile(indexOfFile);
	}
	public Message(String message) {
		this.setMessage(message);
	}
	public Message(String message, String fileName, String toAppendContent) {
		this.setMessage(message);
		this.setFileName(fileName);
		this.setToAppendContent(toAppendContent);
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Map<String, TreeMap<Integer, Chunk>> getFileDict() {
		return fileDict;
	}
	public void setFileDict(Map<String, TreeMap<Integer, Chunk>> fileDict) {
		this.fileDict = fileDict;
	}
	public Chunk getChunk() {
		return chunk;
	}
	public void setChunk(Chunk chunk) {
		this.chunk = chunk;
	}
	public int getAppendSize() {
		return appendSize;
	}
	public void setAppendSize(int appendSize) {
		this.appendSize = appendSize;
	}
	public int getIndexOfFile() {
		return indexOfFile;
	}
	public void setIndexOfFile(int indexOfFile) {
		this.indexOfFile = indexOfFile;
	}
	public String getToAppendContent() {
		return toAppendContent;
	}
	public void setToAppendContent(String toAppendContent) {
		this.toAppendContent = toAppendContent;
	}
	public InetAddress getIp() {
		return ip;
	}
	public void setIp(InetAddress ip) {
		this.ip = ip;
	}
}
