package com.google.codes.dryvalidator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Validations implements Serializable {
	private static final long serialVersionUID = 1L;

	private List<Validation> validations = new ArrayList<Validation>();
	public void addValidation(Validation validation) {
		this.validations.add(validation);
	}
	
	public Validation[] getValidation() {
		return validations.toArray(new Validation[0]);
	}
	

}
