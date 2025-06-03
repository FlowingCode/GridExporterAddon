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
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.HeaderRow.HeaderCell;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.poi.EncryptedDocumentException;

@DemoSource
@PageTitle("Multiple Header Rows")
@Route(value = "gridexporter/headers", layout = GridExporterDemoView.class)
@SuppressWarnings("serial")
public class GridExporterMultipleHeaderRowsDemo extends Div {

  private static final String NUMBER_FORMAT_PATTERN = "$#,###.##";
  private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
  private static final String EXCEL_TEMPLATE_PATH = "/custom-template.xlsx";
  private static final String WORD_TEMPLATE_PATH = "/custom-template.docx";

  public GridExporterMultipleHeaderRowsDemo() throws EncryptedDocumentException, IOException {
    Grid<Person> grid = new Grid<>(Person.class);
    DecimalFormat decimalFormat = new DecimalFormat(NUMBER_FORMAT_PATTERN);
    grid.removeAllColumns();
    Column<Person> firstNameColumn = grid.addColumn(
        LitRenderer.<Person>of("<b>${item.name}</b>").withProperty("name", Person::getName))
        .setHeader("Name");
    Column<Person> lastNameColumn = grid.addColumn("lastName").setHeader("Last Name");
    Column<Person> bigColumn =
        grid.addColumn(item -> Faker.instance().lorem().characters(30, 50)).setHeader("Big column");
    Column<Person> budgetColumn = grid.addColumn(item -> decimalFormat.format(item.getBudget()))
        .setHeader("Budget").setTextAlign(ColumnTextAlign.END);
    List<Person> people = IntStream.range(0, 100).asLongStream().mapToObj(number -> {
      Faker faker = new Faker();
      Double budget = faker.number().randomDouble(2, 10000, 100000);
      return new Person(faker.name().firstName(),
          (Math.random() > 0.3 ? faker.name().lastName() : null),
          faker.number().numberBetween(15, 50), budget);
    }).collect(Collectors.toList());
    @SuppressWarnings("null")
    BigDecimal total = people.stream().map(Person::getBudget).map(BigDecimal::valueOf)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    budgetColumn.setFooter(new DecimalFormat(NUMBER_FORMAT_PATTERN).format(total));

    grid.setItems(people);
    grid.setWidthFull();
    setSizeFull();

    HeaderRow joinedHeaderRow = grid.prependHeaderRow();
    joinedHeaderRow.join(firstNameColumn, lastNameColumn).setText("Full name");
    joinedHeaderRow.join(bigColumn, budgetColumn).setText("Big column and budget");

    HeaderRow firstExtraHeaderRow = grid.appendHeaderRow();
    HeaderRow secondExtraHeaderRow = grid.appendHeaderRow();
    for (Column<Person> column : grid.getColumns()) {
      String columnHeader = grid.getHeaderRows().get(1).getCell(column).getText();

      HeaderCell firstHeaderCell = firstExtraHeaderRow.getCell(column);
      firstHeaderCell.setComponent(new Span(columnHeader + " 1"));
      HeaderCell secondHeaderCell = secondExtraHeaderRow.getCell(column);
      secondHeaderCell.setComponent(new Span(columnHeader + " 2"));
    }

    GridExporter<Person> exporter = GridExporter.createFor(grid, EXCEL_TEMPLATE_PATH, WORD_TEMPLATE_PATH);
    HashMap<String, String> placeholders = new HashMap<>();
    placeholders.put("${date}", new SimpleDateFormat(DATE_FORMAT_PATTERN).format(Calendar.getInstance().getTime()));
    exporter.setAdditionalPlaceHolders(placeholders);
    exporter.setSheetNumber(1);
    exporter.setCsvExportEnabled(false);
    exporter.setNumberColumnFormat(budgetColumn, decimalFormat, NUMBER_FORMAT_PATTERN);
    exporter.setTitle("People information");
    exporter.setNullValueHandler(() -> "(No lastname)");
    exporter.setFileName(
        "GridExport" + new SimpleDateFormat(DATE_FORMAT_PATTERN).format(Calendar.getInstance().getTime()));
    add(grid);
  }
}
