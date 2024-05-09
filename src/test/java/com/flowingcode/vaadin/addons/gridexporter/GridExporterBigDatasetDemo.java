/*-
 * #%L
 * Grid Exporter Add-on
 * %%
 * Copyright (C) 2022 - 2024 Flowing Code
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

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.poi.EncryptedDocumentException;
import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.github.javafaker.Faker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Grid Exporter Addon Big Dataset Demo")
@Route(value = "gridexporter/bigdataset", layout = GridExporterDemoView.class)
@SuppressWarnings("serial")
public class GridExporterBigDatasetDemo extends Div {

  private static final Faker faker = FakerInstance.get();

  public GridExporterBigDatasetDemo() throws EncryptedDocumentException, IOException {
    Grid<Person> grid = new Grid<>(Person.class);
    grid.removeAllColumns();
    grid.setColumnReorderingAllowed(true);
    Column<Person> nameCol = grid.addColumn("name").setHeader("Name");
    Column<Person> lastNameCol = grid.addColumn("lastName").setHeader("Last Name");
    Column<Person> budgetCol = grid.addColumn(item -> "$" + item.getBudget()).setHeader("Budget");
    BigDecimal[] total = new BigDecimal[1];
    total[0] = BigDecimal.ZERO;
    List<Person> persons =
        IntStream.range(0, 7000)
            .asLongStream()
            .mapToObj(
                number -> {
                  Double budget = faker.number().randomDouble(2, 10000, 100000);
                  total[0] = total[0].add(BigDecimal.valueOf(budget));
                  budgetCol.setFooter("$" + total[0]);
                  return new Person(
                      faker.name().firstName(),
                      faker.name().lastName(),
                      faker.number().numberBetween(15, 50),
                      budget);
                }).collect(Collectors.toList());
    grid.setItems(query->persons.stream().skip(query.getOffset()).limit(query.getLimit()));
    grid.setWidthFull();
    this.setSizeFull();
    GridExporter<Person> exporter = GridExporter.createFor(grid);
    exporter.setAutoSizeColumns(false);
    exporter.setExportValue(budgetCol, item -> "" + item.getBudget());
    exporter.setColumnPosition(lastNameCol, 1);
    exporter.setTitle("People information");
    exporter.setFileName(
        "GridExport" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));
    add(grid);
  }
}
