package com.flowingcode.vaadin.addons.gridexporter.test;

import static org.junit.Assert.assertEquals;

import com.flowingcode.vaadin.addons.gridexporter.GridExporter;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class JoinedColumnsExportTest {

    private Grid<Person> grid;
    private List<Person> people;

    @Before
    public void setup() {
        grid = new Grid<>(Person.class);
        grid.removeAllColumns();
        grid.addColumn(Person::getFirstName).setHeader("Firstname").setKey("firstName");
        grid.addColumn(Person::getLastName).setHeader("Lastname").setKey("lastName");

        people = IntStream.range(0, 10).mapToObj(i -> new Person("Name" + i, "Surname" + i))
                .collect(Collectors.toList());
        grid.setItems(people);
    }

    @Test
    public void testCsvExportWithJoinedColumns() throws IOException {
        HeaderRow headerRow = grid.prependHeaderRow();
        Div cellTotal = new Div("Joined cells");
        headerRow.join(grid.getColumns().get(0), grid.getColumns().get(1)).setComponent(cellTotal);

        GridExporter<Person> exporter = GridExporter.createFor(grid);
        exporter.setCsvExportEnabled(true);
        exporter.setHeaderRowIndex(0); // Use the prepended header row

        StreamResource sr = exporter.getCsvStreamResource();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.doReturn(service).when(session).getService();
        Mockito.doNothing().when(service).runPendingAccessTasks(session);
        Mockito.doReturn(new ReentrantLock()).when(session).getLockInstance();
        Mockito.doReturn(new ConcurrentLinkedQueue<>()).when(session).getPendingAccessQueue();
        Mockito.doCallRealMethod().when(session).lock();
        Mockito.doCallRealMethod().when(session).unlock();

        sr.getWriter().accept(os, session);

        String csvOutput = os.toString(StandardCharsets.UTF_8);
        String[] lines = csvOutput.split("\\R");

        // The first line should contain the joined header
        assertEquals("\uFEFF\"Joined cells\",\"\"", lines[0]);
    }

    @Test
    public void testExcelExportWithChosenHeaderRow() throws IOException {
        HeaderRow headerRow = grid.prependHeaderRow();
        Div cellTotal = new Div("Joined cells");
        headerRow.join(grid.getColumns().get(0), grid.getColumns().get(1)).setComponent(cellTotal);

        GridExporter<Person> exporter = GridExporter.createFor(grid);
        exporter.setCustomHeader(grid.getColumns().get(0), "join_firstName");
        exporter.setCustomHeader(grid.getColumns().get(1), "join_lastName");
        exporter.setExcelExportEnabled(true);
        exporter.setHeaderRowIndex(0); // Choose the prepended header row

        StreamResource sr = exporter.getExcelStreamResource();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.doReturn(service).when(session).getService();
        Mockito.doNothing().when(service).runPendingAccessTasks(session);
        Mockito.doReturn(new ReentrantLock()).when(session).getLockInstance();
        Mockito.doReturn(new ConcurrentLinkedQueue<>()).when(session).getPendingAccessQueue();
        Mockito.doCallRealMethod().when(session).lock();
        Mockito.doCallRealMethod().when(session).unlock();

        sr.getWriter().accept(os, session);

        InputStream fis = new ByteArrayInputStream(os.toByteArray());
        Workbook workbook = WorkbookFactory.create(fis);
        Sheet sheet = workbook.getSheetAt(0);

        // Verify header row (row 1 in default template)
        Row header = sheet.getRow(1);
        assertEquals("join_firstName", header.getCell(0).getStringCellValue());

        // Data row (row 2 in default template)
        Row dataRow = sheet.getRow(2);
        assertEquals("Name0", dataRow.getCell(0).getStringCellValue());

        workbook.close();
        fis.close();
    }

    @Test
    public void testExcelExportWithAllHeaderRows() throws IOException {
        HeaderRow headerRow = grid.prependHeaderRow();
        Div cellTotal = new Div("Joined cells");
        headerRow.join(grid.getColumns().get(0), grid.getColumns().get(1)).setComponent(cellTotal);

        GridExporter<Person> exporter = GridExporter.createFor(grid);
        exporter.setExcelExportEnabled(true);
        exporter.setHeaderRowIndex(-1); // Default is -1 anyway

        StreamResource sr = exporter.getExcelStreamResource();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.doReturn(service).when(session).getService();
        Mockito.doNothing().when(service).runPendingAccessTasks(session);
        Mockito.doReturn(new ReentrantLock()).when(session).getLockInstance();
        Mockito.doReturn(new ConcurrentLinkedQueue<>()).when(session).getPendingAccessQueue();
        Mockito.doCallRealMethod().when(session).lock();
        Mockito.doCallRealMethod().when(session).unlock();

        sr.getWriter().accept(os, session);

        InputStream fis = new ByteArrayInputStream(os.toByteArray());
        Workbook workbook = WorkbookFactory.create(fis);
        Sheet sheet = workbook.getSheetAt(0);

        // Row 1: Joined Grid Header Row 0
        Row header1 = sheet.getRow(1);
        assertEquals("Joined cells", header1.getCell(0).getStringCellValue());

        // Row 2: Grid Header Row 1
        Row header2 = sheet.getRow(2);
        assertEquals("Firstname", header2.getCell(0).getStringCellValue());
        assertEquals("Lastname", header2.getCell(1).getStringCellValue());

        // Data row (row 3)
        Row dataRow = sheet.getRow(3);
        assertEquals("Name0", dataRow.getCell(0).getStringCellValue());

        workbook.close();
        fis.close();
    }
}
