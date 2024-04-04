package com.flowingcode.vaadin.addons.gridexporter;

import com.github.javafaker.Faker;

public class FakerInstance {

  private static final Faker faker = new Faker();

  public static Faker get() {
    return faker;
  }

}
