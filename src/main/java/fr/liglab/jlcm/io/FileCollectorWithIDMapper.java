package fr.liglab.jlcm.io;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.Map;

public class FileCollectorWithIDMapper extends FileCollector {
	private final Map<Integer, String> map;
	
	public FileCollectorWithIDMapper(String path, Map<Integer, String> itemIDmap) throws IOException {
		super(path);
		this.map = itemIDmap;
	}
	
	@Override
	protected void putItem(int i) {
		try {
			byte[] asBytes = this.map.get(i).getBytes(charset);
			buffer.put(asBytes);
		} catch (BufferOverflowException e) {
			flush();
			putItem(i);
		}
	}
}
