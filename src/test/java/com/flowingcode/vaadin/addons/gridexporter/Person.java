package com.flowingcode.vaadin.addons.gridexporter;

public class Person {

	private String name;
	private String lastName;
	private Integer age;
	private Double budget;
	
	public Person(String name, String lastName, Integer age, Double budget) {
		super();
		this.name = name;
		this.lastName = lastName;
		this.age = age;
		this.budget = budget;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public Integer getAge() {
		return age;
	}
	public void setAge(Integer age) {
		this.age = age;
	}
	public Double getBudget() {
		return budget;
	}
	public void setBudget(Double budget) {
		this.budget = budget;
	}
	
}
