package com.flowingcode.vaadin.addons.gridexporter;

import java.time.LocalDate;

public class Person {

	private String name;
	private String lastName;
	private Integer age;
	private Double budget;
    private LocalDate favDate;
    private LocalDate worstDate;
	
    public Person(String name, String lastName, Integer age, Double budget) {
      this(name,lastName,age,budget,null,null);
    }

    public Person(String name, String lastName, Integer age, Double budget, LocalDate favDate, LocalDate worstDate) {
		super();
		this.name = name;
		this.lastName = lastName;
		this.age = age;
		this.budget = budget;
		this.favDate = favDate;
		this.worstDate = worstDate;
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
    public LocalDate getFavDate() {
        return favDate;
    }
    public void setFavDate(LocalDate favoriteDate) {
        this.favDate = favoriteDate;
    }
    public LocalDate getWorstDate() {
      return worstDate;
    }
    public void setWorstDate(LocalDate worstDate) {
      this.worstDate = worstDate;
    }
	
}
