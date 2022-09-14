package com.flowingcode.vaadin.addons.gridexporter;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.flowingcode.vaadin.addons.gridhelpers.GridHelper;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.PropertyDefinition;
import com.vaadin.flow.data.binder.PropertySet;
import com.vaadin.flow.data.renderer.BasicRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.server.StreamResource;

@SuppressWarnings("serial")
public class GridExporter<T> implements Serializable {

  private final static Logger LOGGER = LoggerFactory.getLogger(ExcelInputStreamFactory.class);

  private boolean excelExportEnabled = true;
  private boolean docxExportEnabled = true;
  private boolean pdfExportEnabled = true;
  private boolean csvExportEnabled = true;
  
  static final String COLUMN_VALUE_PROVIDER_DATA = "column-value-provider-data";
  static final String COLUMN_EXPORTED_PROVIDER_DATA = "column-value-exported-data";

  Grid<T> grid;

  String titlePlaceHolder = "${title}";
  String headersPlaceHolder = "${headers}";
  String dataPlaceHolder = "${data}";
  String footersPlaceHolder = "${footers}";

  Collection<Grid.Column<T>> columns;
  PropertySet<T> propertySet;

  Map<String, String> additionalPlaceHolders = new HashMap<>();

  String title = "Grid Export";

  String fileName = "export";

  int sheetNumber = 0;

  boolean autoAttachExportButtons = true;

  boolean autoMergeTitle = true;
  
  SerializableSupplier<String> nullValueSupplier;

  public int totalcells = 0;

  private GridExporter(Grid<T> grid) {
    this.grid = grid;
  }

  public static <T> GridExporter<T> createFor(Grid<T> grid) {
    return createFor(grid, null, null);
  }

  public static <T> GridExporter<T> createFor(Grid<T> grid, String excelCustomTemplate,
      String docxCustomTemplate) {
    GridExporter<T> exporter = new GridExporter<>(grid);
    grid.getElement().addAttachListener(ev -> {
      if (exporter.autoAttachExportButtons) {
        HorizontalLayout hl = new HorizontalLayout();
        if (exporter.isExcelExportEnabled()) {
          Anchor excelLink = new Anchor("", FontAwesome.Regular.FILE_EXCEL.create());
          excelLink.setHref(exporter.getExcelStreamResource(excelCustomTemplate));
          excelLink.getElement().setAttribute("download", true);
          hl.add(excelLink);
        }
        if (exporter.isDocxExportEnabled()) {
          Anchor docLink = new Anchor("", FontAwesome.Regular.FILE_WORD.create());
          docLink.setHref(exporter.getDocxStreamResource(docxCustomTemplate));
          docLink.getElement().setAttribute("download", true);
          hl.add(docLink);
        }
        if (exporter.isPdfExportEnabled()) {
          Anchor docLink = new Anchor("", FontAwesome.Regular.FILE_PDF.create());
          docLink.setHref(exporter.getPdfStreamResource(docxCustomTemplate));
          docLink.getElement().setAttribute("download", true);
          hl.add(docLink);
        }
        if (exporter.isCsvExportEnabled()) {
          Anchor csvLink = new Anchor("", FontAwesome.Regular.FILE_LINES.create());
          csvLink.setHref(exporter.getCsvStreamResource());
          csvLink.getElement().setAttribute("download", true);
          hl.add(csvLink);
        }
        hl.setSizeFull();
        hl.setJustifyContentMode(JustifyContentMode.END);
        GridHelper.addToolbarFooter(grid, hl);
      }
    });
    return exporter;
  }
  
  Object extractValueFromColumn(T item, Column<T> column) {
    Object value = null;
    // first check if therer is a value provider for the current column
    @SuppressWarnings("unchecked")
    ValueProvider<T,String> customVP = (ValueProvider<T, String>) ComponentUtil.getData(column, GridExporter.COLUMN_VALUE_PROVIDER_DATA);
    if (customVP!=null) {
      value = customVP.apply(item);
    }
          
    // if there is a key, assume that the property can be retrieved from it
    if (value==null && column.getKey() != null) {
      Optional<PropertyDefinition<T, ?>> propertyDefinition =
          this.propertySet.getProperty(column.getKey());
      if (propertyDefinition.isPresent()) {
        value = propertyDefinition.get().getGetter().apply(item);
      } else {
        LOGGER.warn("Column key: " + column.getKey() + " is a property which cannot be found");
      }
    }

    // if the value still couldn't be retrieved then if the renderer is a LitRenderer, take the value only
    // if there is one value provider
    if (value==null && column.getRenderer() instanceof LitRenderer) {
      LitRenderer<T> r = (LitRenderer<T>) column.getRenderer();
      if (r.getValueProviders().values().size()==1) {
        value = r.getValueProviders().values().iterator().next().apply(item);
      }
    }    

    // at this point if the value is still null then take the only value from ColumPathRenderer VP
    if (value==null && column.getRenderer() instanceof Renderer) {
      Renderer<T> r = (Renderer<T>) column.getRenderer();
      if (r.getValueProviders().size()>0) {
        value = r.getValueProviders().values().iterator().next().apply(item);
      } else if (r instanceof BasicRenderer) {
        try {
          Method getValueProviderMethod = BasicRenderer.class.getDeclaredMethod("getValueProvider");
          getValueProviderMethod.setAccessible(true);
          @SuppressWarnings("unchecked")
          ValueProvider<T,?> vp = (ValueProvider<T, ?>) getValueProviderMethod.invoke(r);
          value = vp.apply(item);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new IllegalStateException("Problem obtaining value or exporting", e);
        }
      }
    }
    
    if (value==null) {
      if (nullValueSupplier!=null) {
        value = nullValueSupplier.get();
      } else {
        throw new IllegalStateException("It's not possible to obtain a value for column, please set a value provider by calling setExportValue()");
      }
    }
    return value;
  }

  public StreamResource getDocxStreamResource() {
    return getDocxStreamResource(null);
  }

  public StreamResource getDocxStreamResource(String template) {
    return new StreamResource(fileName + ".docx", new DocxInputStreamFactory<>(this, template));
  }

  public StreamResource getPdfStreamResource() {
    return getPdfStreamResource(null);
  }

  public StreamResource getPdfStreamResource(String template) {
    return new StreamResource(fileName + ".pdf", new PdfInputStreamFactory<>(this, template));
  }

  public StreamResource getCsvStreamResource() {
    return new StreamResource(fileName + ".csv", new CsvInputStreamFactory<>(this));
  }

  public StreamResource getExcelStreamResource() {
    return getExcelStreamResource(null);
  }

  public StreamResource getExcelStreamResource(String template) {
    return new StreamResource(fileName + ".xlsx", new ExcelInputStreamFactory<>(this, template));
  }

  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the exported file
   * @param title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  public String getFileName() {
    return fileName;
  }

  /**
   * Sets the filename of the exported file
   * @param fileName
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public boolean isAutoAttachExportButtons() {
    return autoAttachExportButtons;
  }

  /**
   * If true, it will automatically generate export buttons in the asociated grid
   * @param autoAttachExportButtons
   */
  public void setAutoAttachExportButtons(boolean autoAttachExportButtons) {
    this.autoAttachExportButtons = autoAttachExportButtons;
  }

  public Map<String, String> getAdditionalPlaceHolders() {
    return additionalPlaceHolders;
  }

  /**
   * Sets a map that will contain additional place holders that will be replaced with values
   * when processing the exported file
   * @param additionalPlaceHolders
   */
  public void setAdditionalPlaceHolders(Map<String, String> additionalPlaceHolders) {
    this.additionalPlaceHolders = additionalPlaceHolders;
  }

  public int getSheetNumber() {
    return sheetNumber;
  }

  /**
   * Configures the excel sheet that will be inspected for placeholders to export the data
   * @param sheetNumber
   */
  public void setSheetNumber(int sheetNumber) {
    this.sheetNumber = sheetNumber;
  }

  public boolean isAutoMergeTitle() {
    return autoMergeTitle;
  }

  /**
   * If true the title cell will be merged with the next ones to create a single title cell
   * that will span across the columns
   * @param autoMergeTitle
   */
  public void setAutoMergeTitle(boolean autoMergeTitle) {
    this.autoMergeTitle = autoMergeTitle;
  }

  public boolean isExcelExportEnabled() {
    return excelExportEnabled;
  }

  public void setExcelExportEnabled(boolean excelExportEnabled) {
    this.excelExportEnabled = excelExportEnabled;
  }

  public boolean isDocxExportEnabled() {
    return docxExportEnabled;
  }

  public void setDocxExportEnabled(boolean docxExportEnabled) {
    this.docxExportEnabled = docxExportEnabled;
  }

  public boolean isPdfExportEnabled() {
    return pdfExportEnabled;
  }

  public void setPdfExportEnabled(boolean pdfExportEnabled) {
    this.pdfExportEnabled = pdfExportEnabled;
  }
  
  public boolean isCsvExportEnabled() {
    return csvExportEnabled;
  }

  public void setCsvExportEnabled(boolean csvExportEnabled) {
    this.csvExportEnabled = csvExportEnabled;
  }

  /**
   * Configure a value provider for a given column. If there is a value provider,
   * that will be taken into account when exporting the column
   * @param column
   * @param vp
   */
  public void setExportValue(Column<T> column, ValueProvider<T, String> vp) {
    ComponentUtil.setData(column, COLUMN_VALUE_PROVIDER_DATA, vp);
  }
  
  /**
   * Configure if the column is exported or not
   * @param column
   * @param export: true will be included in the exported file, false will not be included
   */
  public void setExportColumn(Column<T> column, boolean export) {
    ComponentUtil.setData(column, COLUMN_EXPORTED_PROVIDER_DATA, export);
  }
  
  public void setNullValueHandler(SerializableSupplier<String> nullValueSupplier) {
    this.nullValueSupplier = nullValueSupplier;
  }
  

}
