package com.flowingcode.vaadin.addons.gridexporter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.poi.EncryptedDocumentException;

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.github.javafaker.Faker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Grid Exporter Addon Basic Demo")
@Route(value = "gridexporter/basic", layout = GridExporterDemoView.class)
@SuppressWarnings("serial")
public class GridExporterDemo extends Div {

  public GridExporterDemo() throws EncryptedDocumentException, IOException {
    Grid<Person> grid = new Grid<>(Person.class);
    grid.removeAllColumns();
    grid.addColumn("name").setHeader("Name");
    grid.addColumn("lastName").setHeader("Last Name");
    Column<Person> c = grid.addColumn(item->"$" + item.getBudget()).setHeader("Budget");
    BigDecimal[] total = new BigDecimal[1];
    total[0] = BigDecimal.ZERO;
    Stream<Person> stream = IntStream.range(0, 100).asLongStream().mapToObj(number->{
        Faker faker = new Faker();
        Double budget = faker.number().randomDouble(2, 10000, 100000);
        total[0] = total[0].add(BigDecimal.valueOf(budget));
        c.setFooter("$" + total[0]);
        return new Person(faker.name().firstName(), faker.name().lastName(), faker.number().numberBetween(15, 50)
        		, budget);
    });
    grid.setItems(DataProvider.fromStream(stream));
    grid.setWidthFull();
    this.setSizeFull();
    GridExporter<Person> exporter = GridExporter.createFor(grid);
    exporter.setTitle("People information");
    exporter.setFileName("GridExport" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));
    add(grid);
  }
}
