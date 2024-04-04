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

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.github.javafaker.Faker;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.poi.EncryptedDocumentException;

@DemoSource
@PageTitle("Grid Exporter Addon Basic Demo")
@Route(value = "gridexporter/basic", layout = GridExporterDemoView.class)
@SuppressWarnings("serial")
public class GridExporterDemo extends Div {

  private static final Faker faker = FakerInstance.get();

  public GridExporterDemo() throws EncryptedDocumentException, IOException {
    Grid<Person> grid = new Grid<>(Person.class);
    grid.removeAllColumns();
    grid.setColumnReorderingAllowed(true);
    Column<Person> nameCol = grid.addColumn("name").setHeader("Name");
    Column<Person> lastNameCol = grid.addColumn("lastName").setHeader("Last Name");
    Column<Person> budgetCol = grid.addColumn(item -> "$" + item.getBudget()).setHeader("Budget");
    BigDecimal[] total = new BigDecimal[1];
    total[0] = BigDecimal.ZERO;
    Stream<Person> stream =
        IntStream.range(0, 100)
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
                });
    grid.setItems(DataProvider.fromStream(stream));
    grid.setWidthFull();
    this.setSizeFull();
    GridExporter<Person> exporter = GridExporter.createFor(grid);
    exporter.setExportValue(budgetCol, item -> "" + item.getBudget());
    exporter.setColumnPosition(lastNameCol, 1);
    exporter.setTitle("People information");
    exporter.setFileName(
        "GridExport" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));
    exporter.setCsvCharset(() -> StandardCharsets.UTF_8);

    TextField filterField = new TextField();
    filterField.setPlaceholder("Filter by");
    filterField.setWidth("120px");

    Select<String> shareSelect = new Select<>();
    shareSelect.setItems("Whatsapp", "Facebook", "X (Twitter)");
    shareSelect.setWidth("120px");
    
    Anchor zipLink = new Anchor("", FontAwesome.Regular.FILE_ZIPPER.create());
    zipLink.setTitle("Download zip file");
    zipLink.getElement().setAttribute("download", true);

    exporter.setFooterToolbarItems(
        List.of(new FooterToolbarItem(zipLink, FooterToolbarItemPosition.EXPORT_BUTTON),
            new FooterToolbarItem(new Button("Share", VaadinIcon.SHARE.create())),
            new FooterToolbarItem(shareSelect),
            new FooterToolbarItem(filterField, FooterToolbarItemPosition.BEFORE_EXPORT_BUTTONS),
            new FooterToolbarItem(new Button("Filter"),
                FooterToolbarItemPosition.BEFORE_EXPORT_BUTTONS)));

    add(grid);
  }
}
