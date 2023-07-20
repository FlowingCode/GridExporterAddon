/*-
 * #%L
 * Grid Exporter Add-on
 * %%
 * Copyright (C) 2022 - 2023 Flowing Code
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
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.poi.EncryptedDocumentException;

@DemoSource
@PageTitle("Grid Exporter Hierarchical Data Demo")
@Route(value = "gridexporter/hierarchical", layout = GridExporterDemoView.class)
@SuppressWarnings("serial")
public class GridExporterHierarchicalDataDemo extends Div {

  private Map<Integer, PersonTreeEntry> ageToTreeEntryMap = new HashMap<>();
  private List<PersonTreeEntry> people;

  public GridExporterHierarchicalDataDemo() throws EncryptedDocumentException, IOException {
    TreeGrid<PersonTreeEntry> grid = new TreeGrid<>(PersonTreeEntry.class);
    grid.removeAllColumns();
    grid.setColumnReorderingAllowed(true);
    Column<PersonTreeEntry> groupCol =
        grid.addHierarchyColumn(item -> item.getAge() != null ? item.getAge() : "")
            .setHeader("Group");
    Column<PersonTreeEntry> nameCol =
        grid.addColumn(item -> item.hasPerson() ? item.getPerson().getName() : null)
            .setHeader("Name");
    Column<PersonTreeEntry> lastNameCol =
        grid.addColumn(item -> item.hasPerson() ? item.getPerson().getLastName() : null)
            .setHeader("Last Name");
    Column<PersonTreeEntry> ageCol =
        grid.addColumn(item -> item.hasPerson() ? item.getPerson().getAge() : null)
            .setHeader("Age");
    Column<PersonTreeEntry> budgetCol =
        grid.addColumn(item -> item.hasPerson() ? "$" + item.getPerson().getBudget() : null)
            .setHeader("Budget");
    BigDecimal[] total = new BigDecimal[1];
    total[0] = BigDecimal.ZERO;

    people =
        IntStream.range(0, 100)
            .asLongStream()
            .mapToObj(
                number -> {
                  Faker faker = new Faker();
                  Double budget = faker.number().randomDouble(2, 10000, 100000);
                  total[0] = total[0].add(BigDecimal.valueOf(budget));
                  budgetCol.setFooter("$" + total[0]);
                  Person p =
                      new Person(
                          faker.name().firstName(),
                          faker.name().lastName(),
                          faker.number().numberBetween(15, 50),
                          budget);
                  if (!ageToTreeEntryMap.containsKey(p.getAge())) {
                    ageToTreeEntryMap.put(p.getAge(), new PersonTreeEntry(p.getAge()));
                  }

                  return new PersonTreeEntry(p);
                })
            .collect(Collectors.toList());

    grid.setItems(
        ageToTreeEntryMap.values().stream(),
        parent ->
            parent.isRoot()
                ? people.stream()
                    .filter(entry -> entry.getPerson().getAge().equals(parent.getAge()))
                : Stream.empty());

    grid.setWidthFull();
    this.setSizeFull();
    GridExporter<PersonTreeEntry> exporter = GridExporter.createFor(grid);
    exporter.setExportValue(
        budgetCol, item -> item.hasPerson() ? "" + item.getPerson().getBudget() : "");
    exporter.setTitle("People information grouped by age");
    exporter.setFileName(
        "GridExport" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));
    add(grid);
  }
}
