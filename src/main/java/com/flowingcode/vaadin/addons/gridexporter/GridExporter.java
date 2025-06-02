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

import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.flowingcode.vaadin.addons.gridhelpers.GridHelper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnPathRenderer;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.PropertyDefinition;
import com.vaadin.flow.data.binder.PropertySet;
import com.vaadin.flow.data.renderer.BasicRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class GridExporter<T> implements Serializable {

  private static final Logger LOGGER = LoggerFactory.getLogger(GridExporter.class);

  private boolean excelExportEnabled = true;
  private boolean docxExportEnabled = true;
  private boolean pdfExportEnabled = true;
  private boolean csvExportEnabled = true;
  private boolean autoSizeColumns = true;

  /** Represents all the permits available to the semaphore. */
  public static final float MAX_COST = ConcurrentStreamResourceWriter.MAX_COST;

  /** A fractional cost that acquires only one permit. */
  public static final float MIN_COST = ConcurrentStreamResourceWriter.MIN_COST;

  /** The standard unit of resource usage for concurrent downloads. */
  public static final float DEFAULT_COST = 1.0f;

  private boolean disableOnClick;

  private float concurrentDownloadCost = DEFAULT_COST;
  private final List<SerializableConsumer<ConcurrentDownloadTimeoutEvent>> instanceDownloadTimeoutListeners =
      new CopyOnWriteArrayList<>();

  static final String COLUMN_VALUE_PROVIDER_DATA = "column-value-provider-data";
  static final String COLUMN_EXPORTED_PROVIDER_DATA = "column-value-exported-data";
  static final String COLUMN_PARSING_FORMAT_PATTERN_DATA = "column-parsing-format-pattern-data";
  static final String COLUMN_EXCEL_FORMAT_DATA = "column-excel-format-data";
  static final String COLUMN_EXCEL_FORMAT_DATA_PROVIDER = "column-excel-format-data-provider";
  static final String COLUMN_TYPE_DATA = "column-type-data";
  static final String COLUMN_TYPE_NUMBER = "number";
  static final String COLUMN_TYPE_DATE = "date";
  static final String COLUMN_HEADER = "column-header";
  static final String COLUMN_FOOTER = "column-footer";
  static final String COLUMN_POSITION = "column-position";

  @Getter
  private Grid<T> grid;

  String titlePlaceHolder = "${title}";
  String headersPlaceHolder = "${headers}";
  String dataPlaceHolder = "${data}";
  String footersPlaceHolder = "${footers}";

  List<Grid.Column<T>> columns;
  private PropertySet<T> propertySet;

  Map<String, String> additionalPlaceHolders = new HashMap<>();

  String title = "Grid Export";

  private SerializableSupplier<String> fileNameSupplier = () -> "export";

  int sheetNumber = 0;

  boolean autoAttachExportButtons = true;

  boolean autoMergeTitle = true;

  SerializableSupplier<String> nullValueSupplier;

  /** @deprecated. This attribute is incremented only when exporting DOCX, but it's never reset. */
  @Deprecated(since = "2.5.0", forRemoval = true)
  public int totalcells = 0;

  private ButtonsAlignment buttonsAlignment = ButtonsAlignment.RIGHT;

  private List<FooterToolbarItem> footerToolbarItems;

  private SerializableSupplier<Charset> csvCharset;

  private String defaultExportIconTooltip = null;
  private String excelIconTooltip = null;
  private String docxIconTooltip = null;
  private String pdfIconTooltip = null;
  private String csvIconTooltip = null;

  private GridExporter(Grid<T> grid) {
    this.grid = grid;
  }

  public static <T> GridExporter<T> createFor(Grid<T> grid) {
    return createFor(grid, null, null);
  }

  public static <T> GridExporter<T> createFor(
      Grid<T> grid, String excelCustomTemplate, String docxCustomTemplate) {
    GridExporter<T> exporter = new GridExporter<>(grid);
    grid.getElement()
        .addAttachListener(
            ev -> {
              FooterToolbar footerToolbar = new FooterToolbar();

              if (exporter.autoAttachExportButtons) {
                if (exporter.isExcelExportEnabled()) {
                  Anchor excelLink = new Anchor("", FontAwesome.Regular.FILE_EXCEL.create());
                  excelLink
                      .setHref(exporter.getExcelStreamResource(excelCustomTemplate)
                          .forComponent(excelLink));
                  excelLink.getElement().setAttribute("download", true);
                  excelLink
                      .setHref(exporter.getExcelStreamResource(excelCustomTemplate)
                          .forComponent(excelLink));
                  excelLink.getElement().setAttribute("download", true);
                  exporter.applyTooltip(excelLink, exporter.excelIconTooltip);
                  footerToolbar.add(
                      new FooterToolbarItem(excelLink, FooterToolbarItemPosition.EXPORT_BUTTON));
                }
                if (exporter.isDocxExportEnabled()) {
                  Anchor docLink = new Anchor("", FontAwesome.Regular.FILE_WORD.create());
                  docLink.setHref(
                      exporter.getDocxStreamResource(docxCustomTemplate).forComponent(docLink));
                  docLink.getElement().setAttribute("download", true);
                  exporter.applyTooltip(docLink, exporter.docxIconTooltip);
                  footerToolbar
                      .add(new FooterToolbarItem(docLink, FooterToolbarItemPosition.EXPORT_BUTTON));
                }
                if (exporter.isPdfExportEnabled()) {
                  Anchor pdfLink = new Anchor("", FontAwesome.Regular.FILE_PDF.create());
                  pdfLink.setHref(
                      exporter.getPdfStreamResource(null).forComponent(pdfLink));
                  pdfLink.getElement().setAttribute("download", true);
                  exporter.applyTooltip(pdfLink, exporter.pdfIconTooltip);
                  footerToolbar
                      .add(new FooterToolbarItem(pdfLink, FooterToolbarItemPosition.EXPORT_BUTTON));
                }
                if (exporter.isCsvExportEnabled()) {
                  Anchor csvLink = new Anchor("", FontAwesome.Regular.FILE_LINES.create());
                  csvLink.setHref(exporter.getCsvStreamResource());
                  csvLink.getElement().setAttribute("download", true);
                  exporter.applyTooltip(csvLink, exporter.csvIconTooltip);
                  footerToolbar
                      .add(new FooterToolbarItem(csvLink, FooterToolbarItemPosition.EXPORT_BUTTON));
                }
              }

              if (exporter.footerToolbarItems != null) {
                footerToolbar.add(exporter.footerToolbarItems);
              }

              if (footerToolbar.hasItems()) {
                footerToolbar.getContent().setJustifyContentMode(exporter.getJustifyContentMode());
                GridHelper.addToolbarFooter(grid, footerToolbar);
              }
            });
    return exporter;
  }

  private JustifyContentMode getJustifyContentMode() {
    JustifyContentMode justifyContentMode;
    if (buttonsAlignment == ButtonsAlignment.LEFT) {
      justifyContentMode = JustifyContentMode.START;
    } else {
      justifyContentMode = JustifyContentMode.END;
    }
    return justifyContentMode;
  }

  public void setButtonsAlignment(ButtonsAlignment buttonsAlignment) {
    this.buttonsAlignment = buttonsAlignment;
  }

  Object extractValueFromColumn(T item, Column<T> column) {
    Object value = null;
    // first check if therer is a value provider for the current column
    @SuppressWarnings("unchecked")
    ValueProvider<T, String> customVP =
        (ValueProvider<T, String>)
            ComponentUtil.getData(column, GridExporter.COLUMN_VALUE_PROVIDER_DATA);
    if (customVP != null) {
      value = customVP.apply(item);
    }

    // if there is a key, assume that the property can be retrieved from it
    if (value == null && column.getKey() != null) {
      if (propertySet == null) {
        propertySet = (PropertySet<T>) BeanPropertySet.get(item.getClass());
      }
      Optional<PropertyDefinition<T, ?>> propertyDefinition =
          propertySet.getProperty(column.getKey());
      if (propertyDefinition.isPresent()) {
        value = propertyDefinition.get().getGetter().apply(item);
      } else {
        LOGGER.debug("Column key: " + column.getKey() + " is a property which cannot be found");
      }
    }

    // if the value still couldn't be retrieved then if the renderer is a LitRenderer, take the
    // value only
    if (value == null && column.getRenderer() instanceof LitRenderer) {
      LitRenderer<T> r = (LitRenderer<T>) column.getRenderer();
      // if there is one value provider
      if (r.getValueProviders().values().size() == 1) {
        value = r.getValueProviders().values().iterator().next().apply(item);
      }
      // the hierarchy column defines two value providers: "children" that serves the number of
      // children and "name" that serves the shown value.
      // so we need to get the value provider named "name" and returning its value
      else if (r.getValueProviders().containsKey("name")) {
        value = r.getValueProviders().get("name").apply(item);
      }
    }

    // at this point if the value is still null then take the only value from ColumPathRenderer VP
    if (value == null && column.getRenderer() instanceof Renderer) {
      Renderer<T> renderer = column.getRenderer();
      if (renderer instanceof ColumnPathRenderer) {
        try {
          Field provider = ColumnPathRenderer.class.getDeclaredField("provider");
          provider.setAccessible(true);
          ValueProvider<T, ?> vp = (ValueProvider<T, ?>) provider.get(renderer);
          value = vp.apply(item);
        } catch (NoSuchFieldException | IllegalAccessException e) {
          throw new IllegalStateException("Problem obtaining value or exporting", e);
        }
      } else if (renderer instanceof BasicRenderer) {
        try {
          Method getValueProviderMethod = BasicRenderer.class.getDeclaredMethod("getValueProvider");
          getValueProviderMethod.setAccessible(true);
          @SuppressWarnings("unchecked")
          ValueProvider<T, ?> vp = (ValueProvider<T, ?>) getValueProviderMethod.invoke(renderer);
          value = vp.apply(item);
        } catch (NoSuchMethodException
            | SecurityException
            | IllegalAccessException
            | IllegalArgumentException
            | InvocationTargetException e) {
          throw new IllegalStateException("Problem obtaining value or exporting", e);
        }
      }
    }

    if (value == null) {
      if (nullValueSupplier != null) {
        value = nullValueSupplier.get();
      } else {
        String colKey = "n/a";
        if (column.getKey() != null) {
          colKey = column.getKey();
        }
        throw new IllegalStateException(
            "It's not possible to obtain a value for column with key '"
                + colKey
                + "', please set a value provider by calling setExportValue()");
      }
    }
    return value;
  }

  public GridExporterStreamResource getDocxStreamResource() {
    return getDocxStreamResource(null);
  }

  public GridExporterStreamResource getDocxStreamResource(String template) {
    return new GridExporterStreamResource(getFileName("docx"),
        makeConcurrentWriter(new DocxStreamResourceWriter<>(this, template)));
  }

  public GridExporterStreamResource getPdfStreamResource() {
    return getPdfStreamResource(null);
  }

  public GridExporterStreamResource getPdfStreamResource(String template) {
    return new GridExporterStreamResource(getFileName("pdf"),
        makeConcurrentWriter(new PdfStreamResourceWriter<>(this, template)));
  }

  public StreamResource getCsvStreamResource() {
    return new StreamResource(getFileName("csv"), new CsvStreamResourceWriter<>(this));
  }

  public GridExporterStreamResource getExcelStreamResource() {
    return getExcelStreamResource(null);
  }

  public GridExporterStreamResource getExcelStreamResource(String template) {
    return new GridExporterStreamResource(getFileName("xlsx"),
        makeConcurrentWriter(new ExcelStreamResourceWriter<>(this, template)));
  }

  private GridExporterConcurrentStreamResourceWriter makeConcurrentWriter(
      StreamResourceWriter writer) {
    return new GridExporterConcurrentStreamResourceWriter(writer);
  }

  public class GridExporterStreamResource extends StreamResource {
    private final GridExporterConcurrentStreamResourceWriter writer;

    GridExporterStreamResource(String name, GridExporterConcurrentStreamResourceWriter writer) {
      super(name, writer);
      this.writer = Objects.requireNonNull(writer);
    }

    public GridExporterStreamResource forComponent(Component component) {
      writer.button = component;
      return this;
    }
  }

  private class GridExporterConcurrentStreamResourceWriter extends ConcurrentStreamResourceWriter {

    GridExporterConcurrentStreamResourceWriter(StreamResourceWriter delegate) {
      super(delegate);
    }

    private Component button;

      @Override
      public float getCost(VaadinSession session) {
        return concurrentDownloadCost;
      }

      @Override
      public long getTimeout() {
        // It would have been possible to specify a different timeout for each instance but I cannot
        // figure out a good use case for that. The timeout returned herebecomes relevant when the
        // semaphore has been acquired by any other download, so the timeout must reflect how long
        // it is reasonable to wait for "any other download" to complete and release the semaphore.
        //
        // Since the reasonable timeout would depend on the duration of "any other download", it
        // makes sense that it's a global setting instead of a per-instance setting.
        return GridExporterConcurrentSettings.getConcurrentDownloadTimeout(TimeUnit.NANOSECONDS);
      }

      @Override
      protected UI getUI() {
        return grid.getUI().orElse(null);
      }

      @Override
      protected void onTimeout() {
        fireConcurrentDownloadTimeout();
      }

    @Override
    protected void onAccept() {
      if (disableOnClick) {
        setButtonEnabled(false);
      }
    }

    @Override
    protected void onFinish() {
      setButtonEnabled(true);
  }

    private void setButtonEnabled(boolean enabled) {
      if (button instanceof HasEnabled) {
        grid.getUI().ifPresent(ui -> ui.access(() -> ((HasEnabled) button).setEnabled(enabled)));
      }
    }
  }

  /**
   * Handles the timeout event by notifying all registered listeners.
   * <p>
   * This method is called when a timeout occurs during a concurrent download. It creates a
   * {@link ConcurrentDownloadTimeoutEvent} and notifies all instance and global listeners. If any
   * listener stops the event propagation, subsequent listeners will not be notified.
   */
  private void fireConcurrentDownloadTimeout() {
    var globalListeners = GridExporterConcurrentSettings.getGlobalDownloadTimeoutListeners();
    if (!instanceDownloadTimeoutListeners.isEmpty() || !globalListeners.isEmpty()) {
      grid.getUI().ifPresent(ui -> ui.access(() -> {
        ConcurrentDownloadTimeoutEvent ev = new ConcurrentDownloadTimeoutEvent(GridExporter.this);
        Stream.concat(instanceDownloadTimeoutListeners.stream(),
            globalListeners.stream()).forEach(listener -> {
              if (!ev.isPropagationStopped()) {
                listener.accept(ev);
              }
            });
      }));
    }
  }

  /**
   * Adds a listener for concurrent download timeout events specific to this instance.
   * <p>
   * The listener will be called whenever a concurrent download timeout event occurs.
   *
   * @param listener the listener to be added
   * @return a {@link Registration} object that can be used to remove the listener
   */
  public Registration addConcurrentDownloadTimeoutEvent(
      SerializableConsumer<ConcurrentDownloadTimeoutEvent> listener) {
    instanceDownloadTimeoutListeners.add(0, listener);
    return () -> instanceDownloadTimeoutListeners.remove(listener);
  }

  /**
   * Configures the behavior of the system when a download is in progress.
   * <p>
   * When {@code disableOnClick} is set to {@code true}, the system prevents the UI from starting an
   * additional download of the same kind while one is already in progress. Downloads from other UIs
   * are still allowed. When set to {@code false}, concurrent downloads are permitted.
   * </p>
   *
   * @param disableOnClick Whether to prevent additional downloads during an ongoing download.
   */
  public void setDisableOnClick(boolean disableOnClick) {
    this.disableOnClick = disableOnClick;
  }

  /**
   * Sets the cost for concurrent downloads. This cost is used to determine the number of permits
   * required for downloads to proceed, thereby controlling the concurrency level. At any given
   * time, the sum of the costs of all concurrent downloads will not exceed the limit set by
   * {@link GridExporterConcurrentSettings#setConcurrentDownloadLimit(float)}.
   * <p>
   *
   * The cost is represented as a float to allow for more granular control over resource usage. By
   * using a floating-point number, fractional costs can be expressed, providing flexibility in
   * determining the resource consumption for different downloads.
   * <p>
   *
   * The cost is converted to a number of permits by capping it to stay within the limit. A cost of
   * 1.0 ({@link #DEFAULT_COST}) represents a standard unit of resource usage, while a cost of 0.5
   * represents half a unit, and a cost above 1.0 indicates higher than normal resource usage.
   * <p>
   *
   * If the cost is zero or negative, no permits are needed. However, any positive cost, no matter
   * how small, will require at least one permit to prevent downloads with very low costs from
   * bypassing the semaphore. {@link #MIN_COST} represents the minimal fractional cost that acquires
   * only one permit (hence {@code 2*MIN_COST} acquires two permits and so on). A cost of
   * {@link #MAX_COST} prevents any other downloads from acquiring permits simultaneously.
   *
   * @param concurrentDownloadCost the cost associated with concurrent downloads for this instance.
   */
  public void setConcurrentDownloadCost(float concurrentDownloadCost) {
    this.concurrentDownloadCost = concurrentDownloadCost;
  }

  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the exported file
   *
   * @param title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  public String getFileName() {
    return fileNameSupplier.get();
  }

  private String getFileName(String extension) {
    return Objects.requireNonNull(getFileName()) + "." + extension;
  }

  /**
   * Sets the filename of the exported file
   *
   * @param fileName
   */
  public void setFileName(String fileName) {
    Objects.requireNonNull(fileName, "File name cannot be null");
    fileNameSupplier = () -> fileName;
  }

  /**
   * Sets a dynamic filename for the exported file.
   *
   * @param fileNameSupplier a supplier that returns the name of the exported file, without
   *        extension.
   */
  public void setFileName(SerializableSupplier<String> fileNameSupplier) {
    this.fileNameSupplier =
        Objects.requireNonNull(fileNameSupplier, "File name supplier cannot be null");
  }

  public boolean isAutoAttachExportButtons() {
    return autoAttachExportButtons;
  }

  /**
   * If true, it will automatically generate export buttons in the asociated grid
   *
   * @param autoAttachExportButtons
   */
  public void setAutoAttachExportButtons(boolean autoAttachExportButtons) {
    this.autoAttachExportButtons = autoAttachExportButtons;
  }

  public Map<String, String> getAdditionalPlaceHolders() {
    return additionalPlaceHolders;
  }

  /**
   * Sets a map that will contain additional place holders that will be replaced with values when
   * processing the exported file
   *
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
   *
   * @param sheetNumber
   */
  public void setSheetNumber(int sheetNumber) {
    this.sheetNumber = sheetNumber;
  }

  public boolean isAutoMergeTitle() {
    return autoMergeTitle;
  }

  /**
   * If true the title cell will be merged with the next ones to create a single title cell that
   * will span across the columns
   *
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

  public boolean isAutoSizeColumns() {
    return autoSizeColumns;
  }

  public void setAutoSizeColumns(boolean autoSizeColumns) {
    this.autoSizeColumns = autoSizeColumns;
  }

  /**
   * Configure a value provider for a given column. If there is a value provider, that will be taken
   * into account when exporting the column
   *
   * @param column
   * @param vp
   */
  public void setExportValue(Column<T> column, ValueProvider<T, ?> vp) {
    ComponentUtil.setData(column, COLUMN_VALUE_PROVIDER_DATA, vp);
  }

  /**
   * Configure if the column is exported or not
   *
   * @param column
   * @param export true will be included in the exported file, false will not be included
   */
  public void setExportColumn(Column<T> column, boolean export) {
    ComponentUtil.setData(column, COLUMN_EXPORTED_PROVIDER_DATA, export);
  }

  public void setNullValueHandler(SerializableSupplier<String> nullValueSupplier) {
    this.nullValueSupplier = nullValueSupplier;
  }

  /**
   * If the column is based on a String, it configures a DecimalFormat to parse a number from the
   * value of the column so it can be converted to a Double, and then allows to specify the excel
   * format to be applied to the cell when exported to excel, so the resulting cell is not a string
   * but a number that can be used in formulas.
   *
   * @param column
   * @param decimalFormat
   * @param excelFormat
   */
  public void setNumberColumnFormat(
      Column<T> column, DecimalFormat decimalFormat, String excelFormat) {
    ComponentUtil.setData(column, COLUMN_PARSING_FORMAT_PATTERN_DATA, decimalFormat);
    ComponentUtil.setData(column, COLUMN_EXCEL_FORMAT_DATA, excelFormat);
    ComponentUtil.setData(column, COLUMN_TYPE_DATA, COLUMN_TYPE_NUMBER);
  }

  /**
   * If the column is based on a String, it configures a DecimalFormat to parse a number from the
   * value of the column so it can be converted to a Double, and then allows to specify the excel
   * format to be applied to the cell when exported to excel with a provider, so the resulting cell
   * is not a string but a number that can be used in formulas.
   *
   * @param column
   * @param decimalFormat
   * @param excelFormatProvider
   */
  public void setNumberColumnFormatProvider(Column<T> column, DecimalFormat decimalFormat,
      ValueProvider<T, String> excelFormatProvider) {
    ComponentUtil.setData(column, COLUMN_PARSING_FORMAT_PATTERN_DATA, decimalFormat);
    ComponentUtil.setData(column, COLUMN_EXCEL_FORMAT_DATA_PROVIDER, excelFormatProvider);
    ComponentUtil.setData(column, COLUMN_TYPE_DATA, COLUMN_TYPE_NUMBER);
  }

  /**
   * If the column is based on a String, it configures a DateFormat to parse a date from the value
   * of the column so it can be converted to a java.util.Date, and then allows to specify the excel
   * format to be applied to the cell when exported to excel, so the resulting cell is not a string
   * but a date that can be used in formulas.
   *
   * @param column
   * @param dateFormat
   * @param excelFormat
   */
  public void setDateColumnFormat(Column<T> column, DateFormat dateFormat, String excelFormat) {
    ComponentUtil.setData(column, COLUMN_PARSING_FORMAT_PATTERN_DATA, dateFormat);
    ComponentUtil.setData(column, COLUMN_EXCEL_FORMAT_DATA, excelFormat);
    ComponentUtil.setData(column, COLUMN_TYPE_DATA, COLUMN_TYPE_DATE);
  }

  /**
   * If the column is based on a String, it configures a DateFormat to parse a date from the value
   * of the column so it can be converted to a java.util.Date, and then allows to specify the excel
   * format to be applied to the cell when exported to excel, so the resulting cell is not a string
   * but a date that can be used in formulas.
   *
   * @param column
   * @param dateFormat
   * @param excelFormatProvider
   */
  public void setDateColumnFormatProvider(Column<T> column, DateFormat dateFormat,
      ValueProvider<T, String> excelFormatProvider) {
    ComponentUtil.setData(column, COLUMN_PARSING_FORMAT_PATTERN_DATA, dateFormat);
    ComponentUtil.setData(column, COLUMN_EXCEL_FORMAT_DATA_PROVIDER, excelFormatProvider);
    ComponentUtil.setData(column, COLUMN_TYPE_DATA, COLUMN_TYPE_DATE);
  }

  /**
   * If the column is based on a number attribute of the item, rendered with a NumberRenderer, it
   * configures the excel format to be applied to the cell when exported to excel, so the resulting
   * cell is not a string but a number that can be used in formulas.
   *
   * @param column
   * @param excelFormat
   */
  public void setNumberColumnFormat(Column<T> column, String excelFormat) {
    ComponentUtil.setData(column, COLUMN_EXCEL_FORMAT_DATA, excelFormat);
    ComponentUtil.setData(column, COLUMN_TYPE_DATA, COLUMN_TYPE_NUMBER);
  }

  /**
   * If the column is based on a LocalDate attribute of the item, rendered with a LocalDateRenderer,
   * it configures the excel format to be applied to the cell when exported to excel, so the
   * resulting cell is not a string but a date that can be used in formulas.
   *
   * @param column
   * @param excelFormat
   */
  public void setDateColumnFormat(Column<T> column, String excelFormat) {
    ComponentUtil.setData(column, COLUMN_EXCEL_FORMAT_DATA, excelFormat);
    ComponentUtil.setData(column, COLUMN_TYPE_DATA, COLUMN_TYPE_DATE);
  }

  /**
   * Configures the exporter to use a custom string for a specific column's header. Usefull when the
   * header is a custom component.
   *
   * @param column
   * @param header
   */
  public void setCustomHeader(Column<T> column, String header) {
    ComponentUtil.setData(column, COLUMN_HEADER, header);
  }

  /**
   * Configures the exporter to use a custom string for a specific column's footer. Usefull when the
   * footer is a custom component.
   *
   * @param column
   * @param header
   */
  public void setCustomFooter(Column<T> column, String header) {
    ComponentUtil.setData(column, COLUMN_FOOTER, header);
  }

  /**
   * Assigns the position of the column in the exported file.
   *
   * @param column
   * @param position
   */
  public void setColumnPosition(Column<T> column, int position) {
    ComponentUtil.setData(column, COLUMN_POSITION, position);
  }

  private int getColumnPosition(Column<T> column) {
    return Optional.ofNullable(ComponentUtil.getData(column, COLUMN_POSITION))
        .map(value -> Integer.class.cast(value))
        .orElse(Integer.MAX_VALUE);
  }

  public void setColumns(List<Grid.Column<T>> columns) {
    this.columns = columns;
  }

  public List<Column<T>> getColumns() {
    return columns;
  }

  /**
   * Get columns in the positions specified by {@link GridExporter#setColumnPosition(Column, int)}
   */
  public List<Column<T>> getColumnsOrdered() {
    return columns == null
        ? columns
        : columns.stream()
            .sorted(Comparator.comparing(this::getColumnPosition))
            .collect(Collectors.toList());
  }

  public void setFooterToolbarItems(List<FooterToolbarItem> footerToolbarItems) {
    this.footerToolbarItems = footerToolbarItems;
  }

  /**
   * Charset to use when exporting the CSV file.
   *
   * @return CSV file charset or default one.
   */
  public Charset getCsvCharset() {
    return csvCharset == null ? Charset.defaultCharset() : csvCharset.get();
  }

  public void setCsvCharset(SerializableSupplier<Charset> charset) {
    csvCharset = charset;
  }

  private void applyTooltip(Anchor link, String specificTooltipText) {
    String finalTooltip = specificTooltipText;
    if (finalTooltip == null) {
        finalTooltip = this.defaultExportIconTooltip;
    }
    link.setTitle(finalTooltip);
  }

  /**
   * Sets the default tooltip text for all export icons.
   * This tooltip will be used for any export icon that does not have a specific tooltip set
   * via methods like {@link #setExcelExportIconTooltip(String)}.
   *
   * @param tooltipText The text to display as the tooltip. Passing {@code null} removes the
   *        default tooltip. An empty string ({@code ""}) should result in the tooltip being cleared.
   */
  public void setDefaultExportIconTooltip(String tooltipText) {
    this.defaultExportIconTooltip = tooltipText;
  }

  /**
   * Sets the tooltip text for the Excel export icon.
   * This overrides any tooltip set by {@link #setDefaultExportIconTooltip(String)} for the Excel icon.
   *
   * @param tooltipText The text to display as the tooltip for the Excel icon. Passing {@code null}
   *        removes this specific tooltip (the default tooltip may then apply). An empty string
   *        ({@code ""}) should result in the tooltip being cleared.
   */
  public void setExcelExportIconTooltip(String tooltipText) {
    this.excelIconTooltip = tooltipText;
  }

  /**
   * Sets the tooltip text for the DOCX (Word) export icon.
   * This overrides any tooltip set by {@link #setDefaultExportIconTooltip(String)} for the DOCX icon.
   *
   * @param tooltipText The text to display as the tooltip for the DOCX icon. Passing {@code null}
   *        removes this specific tooltip (the default tooltip may then apply). An empty string
   *        ({@code ""}) should result in the tooltip being cleared.
   */
  public void setDocxExportIconTooltip(String tooltipText) {
    this.docxIconTooltip = tooltipText;
  }

  /**
   * Sets the tooltip text for the PDF export icon.
   * This overrides any tooltip set by {@link #setDefaultExportIconTooltip(String)} for the PDF icon.
   *
   * @param tooltipText The text to display as the tooltip for the PDF icon. Passing {@code null}
   *        removes this specific tooltip (the default tooltip may then apply). An empty string
   *        ({@code ""}) should result in the tooltip being cleared.
   */
  public void setPdfExportIconTooltip(String tooltipText) {
    this.pdfIconTooltip = tooltipText;
  }

  /**
   * Sets the tooltip text for the CSV export icon.
   * This overrides any tooltip set by {@link #setDefaultExportIconTooltip(String)} for the CSV icon.
   *
   * @param tooltipText The text to display as the tooltip for the CSV icon. Passing {@code null}
   *        removes this specific tooltip (the default tooltip may then apply). An empty string
   *        ({@code ""}) should result in the tooltip being cleared.
   */
  public void setCsvExportIconTooltip(String tooltipText) {
    this.csvIconTooltip = tooltipText;
  }

}
