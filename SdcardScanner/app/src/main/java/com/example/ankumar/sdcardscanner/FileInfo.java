package com.example.ankumar.sdcardscanner;

import java.io.Serializable;

/**
 * Created by ankumar on 5/14/16.
 */
public class FileInfo implements Serializable{

	private final String path;
	private final long size;
	private final String extension;
	public FileInfo(String path, long size, String extension) {
		this.path = path;
		this.size = size;
		this.extension = extension;
	}

	public String getPath() {
		return  path;
	}

	public long getSize() {
		return size;
	}

	public String getExtension() {
		return extension;
	}
}
