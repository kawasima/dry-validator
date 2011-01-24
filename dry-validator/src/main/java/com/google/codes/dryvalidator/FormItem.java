package com.google.codes.dryvalidator;

import java.io.Serializable;

public class FormItem implements Serializable {
	private static final long serialVersionUID = 1L;

	private String label;
	private Validations validations;
	
	public FormItem() {
		this.validations = new Validations();
	}
	
	public String getLabel() {
		return this.label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	public Validations getValidations() {
		return validations;
	}
}
