package com.google.codes.dryvalidator.dto;

import java.io.Serializable;

public class FormItem implements Serializable {
	private static final long serialVersionUID = 1L;

	private String id;
	private String label;
	private String messageDecorator;
	private Validations validations;

	public FormItem() {
		this.validations = new Validations();
	}

	public String getId() {
		return this.id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return this.label;
	}
	public void setLabel(String label) {
		this.label = label;
	}

	public String getMessageDecorator() {
		return messageDecorator;
	}

	public void setMessageDecorator(String messageDecorator) {
		this.messageDecorator = messageDecorator;
	}

	public Validations getValidations() {
		return validations;
	}

	@Override
	public String toString() {
		return "FormItem[" + id + "]={ label=" + label
				+ ", validations=[" + validations + "] }";
	}
}
