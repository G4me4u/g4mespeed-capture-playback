package com.g4mesoft.captureplayback.repository;

public class GSRepository {

	private String name;

	public GSRepository() {
		this("");
	}

	public GSRepository(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		if (!name.equals(this.name))
			this.name = name;
	}
}
