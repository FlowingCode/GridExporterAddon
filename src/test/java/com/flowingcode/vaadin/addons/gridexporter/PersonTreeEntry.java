package com.flowingcode.vaadin.addons.gridexporter;

public class PersonTreeEntry {

  private Integer age;
  private Person person;

  public PersonTreeEntry(Integer age) {
    this(age, null);
  }

  public PersonTreeEntry(Person person) {
    this(null, person);
  }

  public PersonTreeEntry(Integer age, Person person) {
    this.age = age;
    this.person = person;
  }

  public Integer getAge() {
    return age;
  }

  public Person getPerson() {
    return person;
  }

  public boolean hasPerson() {
    return person != null;
  }

  public boolean isRoot() {
    return person == null;
  }
}
