package com.example.ankumar.sdcardscanner;

import java.io.Serializable;

/**
 * Created by ankumar on 5/14/16.
 */
public class FileTypeFrequency implements Serializable {

	String fileExtension;
	long frequency;

	public FileTypeFrequency(String fileExtension, long frequency){
		this.fileExtension = fileExtension;
		this.frequency = frequency;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public long getFrequency() {
		return frequency;
	}
}
