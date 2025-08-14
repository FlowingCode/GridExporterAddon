package com.flowingcode.vaadin.addons.gridexporter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.flowingcode.vaadin.addons.gridexporter.GridExporter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.dom.Element;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GridExporter Internal Tooltip State Tests")
class GridExporterTest {

    private Grid<Object> mockGrid;
    private GridExporter<Object> exporter;

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get private field", e);
        }
    }

    @BeforeEach
    void setUp() {
        mockGrid = mock(Grid.class);
        Element mockElement = mock(Element.class);
        when(mockGrid.getElement()).thenReturn(mockElement);
        // Mock UI as it might be accessed by createFor or related methods
        UI mockUi = mock(UI.class);
        when(mockGrid.getUI()).thenReturn(Optional.of(mockUi));

        exporter = GridExporter.createFor(mockGrid);
    }

    @Nested
    @DisplayName("Tooltip Field Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("All tooltip fields should be null initially")
        void testInitialTooltipFieldsAreNull() {
            assertNull(getPrivateField(exporter, "defaultExportIconTooltip"), "defaultExportIconTooltip should be null");
            assertNull(getPrivateField(exporter, "excelIconTooltip"), "excelIconTooltip should be null");
            assertNull(getPrivateField(exporter, "docxIconTooltip"), "docxIconTooltip should be null");
            assertNull(getPrivateField(exporter, "pdfIconTooltip"), "pdfIconTooltip should be null");
            assertNull(getPrivateField(exporter, "csvIconTooltip"), "csvIconTooltip should be null");
        }
    }

    @Nested
    @DisplayName("setDefaultExportIconTooltip Setter Tests")
    class DefaultTooltipSetterTests {

        @Test
        @DisplayName("Should set defaultExportIconTooltip to a given string")
        void testSetDefaultTooltip() {
            exporter.setDefaultExportIconTooltip("Test Default");
            assertEquals("Test Default", getPrivateField(exporter, "defaultExportIconTooltip"));
        }

        @Test
        @DisplayName("Should set defaultExportIconTooltip to null")
        void testSetDefaultTooltipToNull() {
            exporter.setDefaultExportIconTooltip("Initial Value"); // Set a value first
            exporter.setDefaultExportIconTooltip(null);
            assertNull(getPrivateField(exporter, "defaultExportIconTooltip"));
        }

        @Test
        @DisplayName("Should set defaultExportIconTooltip to an empty string")
        void testSetDefaultTooltipToEmptyString() {
            exporter.setDefaultExportIconTooltip("Initial Value"); // Set a value first
            exporter.setDefaultExportIconTooltip("");
            assertEquals("", getPrivateField(exporter, "defaultExportIconTooltip"));
        }
    }

    @Nested
    @DisplayName("Specific Tooltip Setter Tests")
    class SpecificTooltipSetterTests {

        @Test
        @DisplayName("setExcelExportIconTooltip: sets, nullifies, and empties field")
        void testSetExcelExportIconTooltip() {
            exporter.setExcelExportIconTooltip("Excel Tip");
            assertEquals("Excel Tip", getPrivateField(exporter, "excelIconTooltip"));

            exporter.setExcelExportIconTooltip(null);
            assertNull(getPrivateField(exporter, "excelIconTooltip"));

            exporter.setExcelExportIconTooltip("");
            assertEquals("", getPrivateField(exporter, "excelIconTooltip"));
        }

        @Test
        @DisplayName("setDocxExportIconTooltip: sets, nullifies, and empties field")
        void testSetDocxExportIconTooltip() {
            exporter.setDocxExportIconTooltip("Docx Tip");
            assertEquals("Docx Tip", getPrivateField(exporter, "docxIconTooltip"));

            exporter.setDocxExportIconTooltip(null);
            assertNull(getPrivateField(exporter, "docxIconTooltip"));

            exporter.setDocxExportIconTooltip("");
            assertEquals("", getPrivateField(exporter, "docxIconTooltip"));
        }

        @Test
        @DisplayName("setPdfExportIconTooltip: sets, nullifies, and empties field")
        void testSetPdfExportIconTooltip() {
            exporter.setPdfExportIconTooltip("PDF Tip");
            assertEquals("PDF Tip", getPrivateField(exporter, "pdfIconTooltip"));

            exporter.setPdfExportIconTooltip(null);
            assertNull(getPrivateField(exporter, "pdfIconTooltip"));

            exporter.setPdfExportIconTooltip("");
            assertEquals("", getPrivateField(exporter, "pdfIconTooltip"));
        }

        @Test
        @DisplayName("setCsvExportIconTooltip: sets, nullifies, and empties field")
        void testSetCsvExportIconTooltip() {
            exporter.setCsvExportIconTooltip("CSV Tip");
            assertEquals("CSV Tip", getPrivateField(exporter, "csvIconTooltip"));

            exporter.setCsvExportIconTooltip(null);
            assertNull(getPrivateField(exporter, "csvIconTooltip"));

            exporter.setCsvExportIconTooltip("");
            assertEquals("", getPrivateField(exporter, "csvIconTooltip"));
        }
    }
}
