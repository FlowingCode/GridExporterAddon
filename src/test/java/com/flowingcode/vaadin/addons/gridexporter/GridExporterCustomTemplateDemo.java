package com.flowingcode.vaadin.addons.gridexporter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.poi.EncryptedDocumentException;
import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.github.javafaker.Faker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Grid Exporter Addon Custom Templates Demo")
@Route(value = "gridexporter/custom", layout = GridExporterDemoView.class)
@SuppressWarnings("serial")
public class GridExporterCustomTemplateDemo extends Div {

  public GridExporterCustomTemplateDemo() throws EncryptedDocumentException, IOException {
    Grid<Person> grid = new Grid<>(Person.class);
    grid.removeAllColumns();
    grid.addColumn(LitRenderer
        .<Person>of("<b>${item.name}</b>").withProperty("name", Person::getName)).setHeader("Name");
    grid.addColumn("lastName").setHeader("Last Name");
    grid.addColumn(item->Faker.instance().lorem().characters(30, 50)).setHeader("Big column");
    Column<Person> c = grid.addColumn(item->"$" + item.getBudget()).setHeader("Budget");
    BigDecimal[] total = new BigDecimal[1];
    total[0] = BigDecimal.ZERO;
    Stream<Person> stream = IntStream.range(0, 100).asLongStream().mapToObj(number->{
        Faker faker = new Faker();
        Double budget = faker.number().randomDouble(2, 10000, 100000);
        total[0] = total[0].add(BigDecimal.valueOf(budget));
        c.setFooter("$" + total[0]);
        return new Person(faker.name().firstName(), (Math.random()>0.3?faker.name().lastName():null), faker.number().numberBetween(15, 50)
        		, budget);
    });
    grid.setItems(DataProvider.fromStream(stream));
    grid.setWidthFull();
    this.setSizeFull();
    GridExporter<Person> exporter = GridExporter.createFor(grid, "/custom-template.xlsx", "/custom-template.docx");
    HashMap<String,String> placeholders = new HashMap<>();
    placeholders.put("${date}", new SimpleDateFormat().format(Calendar.getInstance().getTime()));
    exporter.setAdditionalPlaceHolders(placeholders);
    exporter.setSheetNumber(1);
    exporter.setCsvExportEnabled(false);
    exporter.setTitle("People information");
    exporter.setNullValueHandler(()->"(No lastname)");
    exporter.setFileName("GridExport" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));
    add(grid);
  }
}
