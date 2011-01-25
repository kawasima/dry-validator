package com.google.codes.dryvalidator.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Validation implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String value;
	
	private List<String> messages = new ArrayList<String>();
	
	public Validation(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public void addMessage(String message) { this.messages.add(message); }
	public Object[] getMessages() { return this.messages.toArray(); }

	public String getValue() { return value; }
	public void setValue(String value) { this.value = value; }
}
