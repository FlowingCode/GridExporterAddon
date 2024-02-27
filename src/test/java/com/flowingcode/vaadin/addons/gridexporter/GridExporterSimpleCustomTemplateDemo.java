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
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.poi.EncryptedDocumentException;

@DemoSource
@PageTitle("Grid Exporter Addon Simple Custom Templates Demo")
@Route(value = "gridexporter/simple-custom", layout = GridExporterDemoView.class)
@SuppressWarnings("serial")
public class GridExporterSimpleCustomTemplateDemo extends Div {

  public GridExporterSimpleCustomTemplateDemo() throws EncryptedDocumentException, IOException {
    Faker faker = new Faker();
    DecimalFormat decimalFormat = new DecimalFormat("$#,###.##");
    Grid<Person> grid = new Grid<>(Person.class);
    grid.removeAllColumns();
    grid.addColumn(
            LitRenderer.<Person>of("<b>${item.name}</b>").withProperty("name", Person::getName))
        .setHeader("Name");
    grid.addColumn("lastName").setHeader("Last Name");
    Column<Person> minBudgetColumn =
        grid.addColumn(new NumberRenderer<>(Person::getBudget, NumberFormat.getCurrencyInstance()))
            .setHeader("Min. Budget")
            .setTextAlign(ColumnTextAlign.END);
    Column<Person> maxBudgetColumn =
        grid.addColumn(item -> decimalFormat.format(item.getBudget() + (item.getBudget() / 2)))
            .setHeader("Max. Budget")
            .setTextAlign(ColumnTextAlign.END);
    Column<Person> quantityColumn =
        grid.addColumn(item -> decimalFormat.format(item.getBudget() + (new Random().nextInt(10))))
            .setHeader("Quantity")
            .setTextAlign(ColumnTextAlign.END);
    Column<Person> dateColumn1 =
        grid.addColumn(
                new LocalDateRenderer<>(
                    Person::getFavDate, DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            .setHeader("Fav Date")
            .setTextAlign(ColumnTextAlign.CENTER);
    Column<Person> dateColumn2 =
        grid.addColumn(
                item -> item.getWorstDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            .setHeader("Worst Date")
            .setTextAlign(ColumnTextAlign.CENTER);
    BigDecimal[] total = new BigDecimal[1];
    total[0] = BigDecimal.ZERO;
    Stream<Person> stream =
        IntStream.range(0, 100)
            .asLongStream()
            .mapToObj(
                number -> {
                  Double budget = faker.number().randomDouble(2, 10000, 100000);
                  return new Person(
                      faker.name().firstName(),
                      faker.name().lastName(),
                      faker.number().numberBetween(15, 50),
                      budget,
                      createRandomDate(faker),
                      createRandomDate(faker));
                });
    grid.setItems(DataProvider.fromStream(stream));
    grid.setWidthFull();
    this.setSizeFull();
    GridExporter<Person> exporter =
        GridExporter.createFor(
            grid, "/simple-custom-template.xlsx", "/simple-custom-template.docx");
    exporter.setSheetNumber(1);
    exporter.setCsvExportEnabled(false);
    exporter.setNumberColumnFormat(minBudgetColumn, "$#.###,##");
    exporter.setNumberColumnFormat(maxBudgetColumn, decimalFormat, "$#,###.##");
    exporter.setNumberColumnFormatProvider(quantityColumn, decimalFormat, (person)->person==null?"":
      person.getBudget()>50000?"#,###.## \"kg\"":"#,###.## \"l\"");
    exporter.setDateColumnFormat(dateColumn1, "dd/MM/yyyy");
    exporter.setDateColumnFormat(dateColumn2, new SimpleDateFormat("dd/MM/yyyy"), "dd/MM/yyyy");
    exporter.setFileName(
        "GridExport" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));
    add(grid);
  }

  protected LocalDate createRandomDate(Faker faker) {
    return Instant.ofEpochMilli(faker.date().past(10000, TimeUnit.DAYS).getTime())
        .atZone(ZoneId.systemDefault())
        .toLocalDate();
  }
}
