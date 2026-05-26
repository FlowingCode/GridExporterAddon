/*-
 * #%L
 * Grid Exporter Add-on
 * %%
 * Copyright (C) 2022 - 2026 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.flowingcode.vaadin.addons.gridexporter;

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.github.javafaker.Faker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@DemoSource
@PageTitle("Joined Headers with Custom Header")
@Route(value = "gridexporter/joined-custom-header", layout = GridExporterDemoView.class)
@SuppressWarnings("serial")
public class GridExporterJoinedHeadersWithCustomHeaderDemo extends Div {

  public GridExporterJoinedHeadersWithCustomHeaderDemo() {
    Grid<Person> grid = new Grid<>(Person.class);
    grid.removeAllColumns();

    Column<Person> firstNameCol = grid.addColumn("name").setHeader("First Name");
    Column<Person> lastNameCol = grid.addColumn("lastName").setHeader("Last Name");

    Faker faker = FakerInstance.get();
    List<Person> people = IntStream.range(0, 20)
        .mapToObj(i -> new Person(faker.name().firstName(), faker.name().lastName(),
            faker.number().numberBetween(15, 50),
            faker.number().randomDouble(2, 10000, 100000)))
        .collect(Collectors.toList());
    grid.setItems(people);

    HeaderRow joinedHeaderRow = grid.prependHeaderRow();
    Div joinedCell = new Div("Full name");
    joinedCell.getStyle().set("text-align", "center");
    joinedHeaderRow.join(firstNameCol, lastNameCol).setComponent(joinedCell);

    GridExporter<Person> exporter = GridExporter.createFor(grid);
    exporter.setTitle("People information");

    // Custom headers are applied only to the header row closest to the data.
    // CSV/DOCX/PDF export this single row; Excel keeps the joined "Full name" above it.
    exporter.setCustomHeader(firstNameCol, "Given name");
    exporter.setCustomHeader(lastNameCol, "Family name");

    grid.setWidthFull();
    setSizeFull();
    add(grid);
  }
}
