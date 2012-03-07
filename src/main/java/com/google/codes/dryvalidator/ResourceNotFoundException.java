package com.google.codes.dryvalidator;

import java.io.IOException;

public class ResourceNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public ResourceNotFoundException(IOException e) {
		super(e);
	}
}
