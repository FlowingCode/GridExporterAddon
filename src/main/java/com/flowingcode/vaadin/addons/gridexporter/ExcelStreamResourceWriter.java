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
/** */
package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.HeaderRow.HeaderCell;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.server.VaadinSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ConditionalFormatting;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author mlopez
 */
@SuppressWarnings("serial")
class ExcelStreamResourceWriter<T> extends BaseStreamResourceWriter<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExcelStreamResourceWriter.class);
  private static final String DEFAULT_TEMPLATE = "/template.xlsx";
  private static final String COLUMN_CELLSTYLE_MAP = "colum-cellstyle-map";
  private static enum ExcelCellType {HEADER,CELL,FOOTER};

  public ExcelStreamResourceWriter(GridExporter<T> exporter, String template) {
    super(exporter, template, DEFAULT_TEMPLATE);
  }

  @Override
  public void accept(OutputStream out, VaadinSession session) throws IOException {
    createWorkbook(session).write(out);
  }

  private Workbook createWorkbook(VaadinSession session) {
    session.lock();
    try {
      Grid<T> grid = exporter.getGrid();
      exporter.setColumns(grid.getColumns().stream().filter(this::isExportable)
          .peek(col -> ComponentUtil.setData(col, COLUMN_CELLSTYLE_MAP, null))
          .collect(Collectors.toList()));
      Workbook wb = getBaseTemplateWorkbook();
      Sheet sheet = wb.getSheetAt(exporter.sheetNumber);

      Cell titleCell = findCellWithPlaceHolder(sheet, exporter.titlePlaceHolder);
      Cell headersPlaceholderCell = findCellWithPlaceHolder(sheet, exporter.headersPlaceHolder);

      if (titleCell != null) {
        titleCell.setCellValue(exporter.title);
      }

      boolean allRows = exporter.getHeaderRowIndex() < 0;
      List<GridHeader<T>> headers = getGridHeaders(grid, allRows);

      int headerRowsCount = allRows ? grid.getHeaderRows().size() : 1;

      if (headersPlaceholderCell != null) {
          fillHeaderRows(sheet, headersPlaceholderCell, headers, headerRowsCount, allRows);
      }

      if (exporter.autoMergeTitle && titleCell != null && exporter.getColumns().size()>1) {
        sheet.addMergedRegion(new CellRangeAddress(titleCell.getRowIndex(), titleCell.getRowIndex(),
            titleCell.getColumnIndex(), titleCell.getColumnIndex() + exporter.getColumns().size() - 1));
      }

      // Re-find data placeholder because it might have been shifted by fillHeaderRows
      Cell dataPlaceholderCell = findCellWithPlaceHolder(sheet, exporter.dataPlaceHolder);
      
      int dataStartingRow = dataPlaceholderCell != null ? dataPlaceholderCell.getRowIndex() : 0;
      int dataStartingColumn = dataPlaceholderCell != null ? dataPlaceholderCell.getColumnIndex() : 0;

      CellRangeAddress dataRange = new CellRangeAddress(dataStartingRow, dataStartingRow,
          dataStartingColumn, dataStartingColumn);

      Sheet tempSheet = wb.cloneSheet(exporter.sheetNumber);

      int lastRow =
          fillData(sheet, dataPlaceholderCell, grid.getDataProvider(), dataRange, titleCell != null);

      applyConditionalFormattings(sheet, dataRange);

      copyBottomOfSheetStartingOnRow(wb, tempSheet, sheet, dataStartingRow + 1, lastRow);

      wb.removeSheetAt(exporter.sheetNumber + 1);

      Cell footersPlaceholderCell = findCellWithPlaceHolder(sheet, exporter.footersPlaceHolder);
      if (footersPlaceholderCell != null) {
        List<GridFooter<T>> footers = getGridFooters(grid);
        fillFooter(sheet, footersPlaceholderCell, footers, false);
      }

      if (exporter.isAutoSizeColumns()) {
        for (int i = 0; i < exporter.getColumns().size(); i++) {
           sheet.autoSizeColumn(dataStartingColumn + i);
        }
      }

      exporter.additionalPlaceHolders.entrySet().forEach(entry -> {
        Cell cellwp;
        cellwp = findCellWithPlaceHolder(sheet, entry.getKey());
        while (cellwp != null) {
          cellwp.setCellValue(entry.getValue());
          cellwp = findCellWithPlaceHolder(sheet, entry.getKey());
        }
      });

      return wb;
    } finally {
      session.unlock();
    }
  }

  private void copyBottomOfSheetStartingOnRow(Workbook workbook, Sheet sourceSheet,
      Sheet targetSheet, int rowIndex, int targetRow) {
    int fRow = rowIndex;
    int lRow = sourceSheet.getLastRowNum();
    for (int iRow = fRow; iRow <= lRow; iRow++) {
      Row row = sourceSheet.getRow(iRow);
      Row myRow = targetSheet.createRow(targetRow++);
      if (row != null) {
        short fCell = row.getFirstCellNum();
        short lCell = row.getLastCellNum();
        for (int iCell = fCell; iCell < lCell; iCell++) {
          Cell cell = row.getCell(iCell);
          Cell newCell = myRow.createCell(iCell);
          newCell.setCellStyle(cell.getCellStyle());
          if (cell != null) {
            switch (cell.getCellType()) {
              case BLANK:
                newCell.setCellValue("");
                break;

              case BOOLEAN:
                newCell.setCellValue(cell.getBooleanCellValue());
                break;

              case ERROR:
                newCell.setCellErrorValue(cell.getErrorCellValue());
                break;

              case FORMULA:
                newCell.setCellFormula(cell.getCellFormula());
                break;

              case NUMERIC:
                newCell.setCellValue(cell.getNumericCellValue());
                break;

              case STRING:
                newCell.setCellValue(cell.getStringCellValue());
                break;
              default:
                newCell.setCellFormula(cell.getCellFormula());
            }
          }
        }
      }
    }
  }

  private void applyConditionalFormattings(Sheet sheet, CellRangeAddress targetCellRange) {
    SheetConditionalFormatting sheetCondFormatting = sheet.getSheetConditionalFormatting();

    for(int i = 0; i < sheetCondFormatting.getNumConditionalFormattings(); i++) {
      // override range for all conditional formattings with the range of the resulting data
      ConditionalFormatting condFormatting = sheetCondFormatting.getConditionalFormattingAt(i);
      condFormatting.setFormattingRanges(ArrayUtils.toArray(targetCellRange));
    }

  }

  private int fillData(
      Sheet sheet, Cell dataCell, DataProvider<T, ?> dataProvider, CellRangeAddress dataRange, boolean titleExists) {
    Stream<T> dataStream = obtainDataStream(dataProvider);

    boolean notFirstRow = false;
    Cell startingCell = dataCell;

    Iterable<T> items = dataStream::iterator;
    for (T item : items) {
      if (notFirstRow) {
        CellStyle cellStyle = startingCell.getCellStyle();
        Row newRow = sheet.createRow(startingCell.getRowIndex() + 1);
        startingCell = newRow.createCell(startingCell.getColumnIndex());
        startingCell.setCellStyle(cellStyle);
      }
      // update the data range by updating last row
      dataRange.setLastRow(dataRange.getLastRow() + 1);
      buildRow(item, sheet, startingCell);
      notFirstRow = true;
    }
    // since we initialized the cell range with the data placeholder cell, we use
    // the existing 'getLastColumn' to keep the offset of the data range
    dataRange.setLastColumn(dataRange.getLastColumn() + exporter.getColumns().size() - 1);
    return dataRange.getLastRow();
  }

  private void buildRow(T item, Sheet sheet, Cell startingCell) {

    if (exporter.getColumns().isEmpty()) {
      throw new IllegalStateException("Grid has no columns");
    }

    int[] currentColumn = new int[1];
    currentColumn[0] = startingCell.getColumnIndex();
    exporter.getColumnsOrdered().stream()
        .forEach(
            column -> {
              Object value = exporter.extractValueFromColumn(item, column);
              value = transformToType(value, column);
              Cell currentCell = startingCell;
              if (startingCell.getColumnIndex() < currentColumn[0]) {
                currentCell = startingCell.getRow().createCell(currentColumn[0]);
                currentCell.setCellStyle(startingCell.getCellStyle());

                configureAlignment(column, currentCell, ExcelCellType.CELL);
              }
              currentColumn[0] = currentColumn[0] + 1;
              buildCell(value, currentCell, column, item);
            });
  }

  private Object transformToType(Object value, Column<T> column) {
    Object result = value;
    if (value instanceof String && StringUtils.isNotBlank((String) value)) {
      String stringValue = (String) value;
      try {
        if (ComponentUtil.getData(column, GridExporter.COLUMN_PARSING_FORMAT_PATTERN_DATA)
            != null) {
          switch ((String) ComponentUtil.getData(column, GridExporter.COLUMN_TYPE_DATA)) {
            case GridExporter.COLUMN_TYPE_NUMBER:
              DecimalFormat decimalFormat =
                  (DecimalFormat)
                      ComponentUtil.getData(
                          column, GridExporter.COLUMN_PARSING_FORMAT_PATTERN_DATA);
              decimalFormat.setParseBigDecimal(true);
              result = decimalFormat.parse(stringValue).doubleValue();
              break;
            case GridExporter.COLUMN_TYPE_DATE:
              result =
                  ((DateFormat)
                          ComponentUtil.getData(
                              column, GridExporter.COLUMN_PARSING_FORMAT_PATTERN_DATA))
                      .parse(stringValue);
              break;
          }
        }
      } catch (ParseException e) {
        throw new IllegalStateException("Problem parsing grid cell value", e);
      }
    }
    return result;
  }

  private void configureAlignment(Column<T> column, Cell currentCell, ExcelCellType type) {
    ColumnTextAlign columnTextAlign = column.getTextAlign();
    switch (columnTextAlign) {
      case START:
        if (!currentCell.getCellStyle().getAlignment().equals(HorizontalAlignment.LEFT)) {
          currentCell.setCellStyle(
              createOrRetrieveCellStyle(HorizontalAlignment.LEFT, currentCell, column, type));
        }
        break;
      case CENTER:
        if (!currentCell.getCellStyle().getAlignment().equals(HorizontalAlignment.CENTER)) {
          currentCell.setCellStyle(
              createOrRetrieveCellStyle(HorizontalAlignment.CENTER, currentCell, column, type));
        }
        break;
      case END:
        if (!currentCell.getCellStyle().getAlignment().equals(HorizontalAlignment.RIGHT)) {
          currentCell.setCellStyle(
              createOrRetrieveCellStyle(HorizontalAlignment.RIGHT, currentCell, column, type));
        }
        break;
      default:
        currentCell.setCellStyle(currentCell.getCellStyle());
    }
  }

  @SuppressWarnings("unchecked")
  private CellStyle createOrRetrieveCellStyle(HorizontalAlignment alignment, Cell currentCell,
      Column<T> column, ExcelCellType type) {
    Map<String, CellStyle> cellStyles =
        (Map<String, CellStyle>) ComponentUtil.getData(column, COLUMN_CELLSTYLE_MAP);
    if (cellStyles == null) {
      cellStyles = new HashMap<>();
      ComponentUtil.setData(column, COLUMN_CELLSTYLE_MAP, cellStyles);
    }
    CellStyle cellStyle;
    if (cellStyles.get(type.name()) == null) {
      cellStyle = currentCell.getSheet().getWorkbook().createCellStyle();
      cellStyle.cloneStyleFrom(currentCell.getCellStyle());
      cellStyle.setAlignment(alignment);
      cellStyles.put(type.name(), cellStyle);
    } else {
      cellStyle = cellStyles.get(type.name());
    }
    return cellStyle;
  }

  @SuppressWarnings("unchecked")
  private void buildCell(Object value, Cell cell, Column<T> column, T item) {
    ValueProvider<T,String> provider = (ValueProvider<T, String>) ComponentUtil.getData(column, GridExporter.COLUMN_EXCEL_FORMAT_DATA_PROVIDER);
    Map<String, CellStyle> cellStyles = getCellStyles(column);
    String excelFormat = getExcelFormat(column, item, provider);
    if (value == null) {
      PoiHelper.setBlank(cell);
      if(excelFormat != null) {
        applyExcelFormat(cell, excelFormat, cellStyles);
      }
    } else if (value instanceof Number) {
      excelFormat = (excelFormat!=null)?excelFormat:"0";
      cell.setCellValue(((Number) value).doubleValue());
      applyExcelFormat(cell, excelFormat, cellStyles);
    } else if (value instanceof Date) {
      excelFormat = (excelFormat!=null)?excelFormat:"dd/MM/yyyy";
      applyExcelFormat(cell, excelFormat, cellStyles);
      cell.setCellValue((Date) value);
    } else if (value instanceof LocalDate) {
      excelFormat = (excelFormat!=null)?excelFormat:"dd/MM/yyyy";
      applyExcelFormat(cell, excelFormat, cellStyles);
      cell.setCellValue(
          Date.from(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    } else {
      cell.setCellValue(value.toString());
    }
  }

  private String getExcelFormat(Column<T> column, T item, ValueProvider<T, String> provider) {
    String excelFormat;
    if (provider!=null) {
      excelFormat = provider.apply(item);
    } else {
      excelFormat =
          (String) ComponentUtil.getData(column, GridExporter.COLUMN_EXCEL_FORMAT_DATA);
    }
    return excelFormat;
  }

  @SuppressWarnings("unchecked")
  private Map<String, CellStyle> getCellStyles(Column<T> column) {
    Map<String,CellStyle> cellStyles = (Map<String, CellStyle>) ComponentUtil.getData(column, COLUMN_CELLSTYLE_MAP);
    if (cellStyles==null) {
      cellStyles = new HashMap<>();
      ComponentUtil.setData(column, COLUMN_CELLSTYLE_MAP, cellStyles);
    }
    return cellStyles;
  }

  private void applyExcelFormat(Cell cell, String excelFormat, Map<String, CellStyle> cellStyles) {
    DataFormat format = cell.getSheet().getWorkbook().createDataFormat();
    if (excelFormat!=null && cellStyles.get(excelFormat)==null) {
      CellStyle cs = cell.getSheet().getWorkbook().createCellStyle();
      cs.cloneStyleFrom(cell.getCellStyle());
      cellStyles.put(excelFormat, cs);
      cell.setCellStyle(cs);
    } else if (excelFormat!=null) {
      cell.setCellStyle(cellStyles.get(excelFormat));
    }
    cell.getCellStyle().setDataFormat(format.getFormat(excelFormat));
  }

  private Workbook getBaseTemplateWorkbook() {
    try {
      InputStream inp = this.getClass().getResourceAsStream(getTemplate());
      Workbook result = WorkbookFactory.create(inp);
      return result;
    } catch (Exception e) {
      throw new IllegalStateException("Problem creating workbook", e);
    }
  }

  private Cell findCellWithPlaceHolder(Sheet sheet, String placeholder) {
    for (Row row : sheet) {
      for (Cell cell : row) {
        if (PoiHelper.cellTypeEquals(cell, CellType.STRING)) {
          if (cell.getRichStringCellValue().getString().trim().equals(placeholder)) {
            return cell;
          }
        }
      }
    }
    return null;
  }

  private boolean hasCustomHeaders(Grid<T> grid) {
    return grid.getColumns().stream()
        .anyMatch(column -> ComponentUtil.getData(column, GridExporter.COLUMN_HEADER) != null);
  }

  private void fillHeaderRows(Sheet sheet, Cell headersPlaceholderCell,
      List<GridHeader<T>> allHeaders, int headerRowsCount, boolean allRows) {
    CellStyle style = headersPlaceholderCell.getCellStyle();
    int startRow = headersPlaceholderCell.getRowIndex();
    int startColumn = headersPlaceholderCell.getColumnIndex();
    Grid<T> grid = exporter.getGrid();
    List<HeaderRow> gridHeaderRows = grid.getHeaderRows();

    // Clear existing merged regions in the header area to prevent conflicts
    for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
        CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
        if (mergedRegion.getFirstRow() >= startRow && mergedRegion.getLastRow() < startRow + headerRowsCount) {
            sheet.removeMergedRegion(i);
        }
    }

    // Shift rows down to make space for all header rows, if necessary
    if (headerRowsCount > 1) {
        sheet.shiftRows(startRow, sheet.getLastRowNum(), headerRowsCount - 1);
    }

    for (int i = 0; i < headerRowsCount; i++) {
        int gridRowIndex = allRows ? i : exporter.getHeaderRowIndex();
        if (gridRowIndex < 0) gridRowIndex = 0;
        HeaderRow hr = gridHeaderRows.get(gridRowIndex);
        Row row = sheet.getRow(startRow + i);
        if (row == null) {
            row = sheet.createRow(startRow + i);
        }

        for (int j = 0; j < allHeaders.size(); j++) {
            GridHeader<T> gh = allHeaders.get(j);
            Column<T> column = gh.getColumn();
            HeaderCell hc = hr.getCell(column);

            // Check if this is the first column of a merged cell
            boolean isFirst = true;
            int colSpan = 1;
            for (int k = j - 1; k >= 0; k--) {
                if (hr.getCell(allHeaders.get(k).getColumn()) == hc) {
                    isFirst = false;
                    break;
                }
            }

            if (isFirst) {
                // Calculate span
                for (int k = j + 1; k < allHeaders.size(); k++) {
                    if (hr.getCell(allHeaders.get(k).getColumn()) == hc) {
                        colSpan++;
                    } else {
                        break;
                    }
                }

                // Create all cells in the span and apply style
                for (int k = j; k < j + colSpan; k++) {
                    Cell c = row.getCell(startColumn + k);
                    if (c == null) c = row.createCell(startColumn + k);
                    c.setCellStyle(style);
                }

                Cell cell = row.getCell(startColumn + j);
                String headerText = renderHeaderCellTextContent(grid, hr, column);
                cell.setCellValue(headerText);
                
                if (colSpan > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow + i, startRow + i, startColumn + j, startColumn + j + colSpan - 1));
                }
                configureAlignment(column, cell, ExcelCellType.HEADER);
            }
        }
    }
  }

  private void fillFooter(Sheet sheet, Cell headersOrFootersCell,
      List<GridFooter<T>> footers, boolean isHeader) {
    fillFooterRows(sheet, headersOrFootersCell, footers);
  }

  private void fillFooterRows(Sheet sheet, Cell footersPlaceholderCell,
      List<GridFooter<T>> allFooters) {
    CellStyle style = footersPlaceholderCell.getCellStyle();
    int startRow = footersPlaceholderCell.getRowIndex();
    int startColumn = footersPlaceholderCell.getColumnIndex();

    for (GridFooter<T> footer : allFooters) {
      Row row = sheet.getRow(startRow);
      if (row == null) {
        row = sheet.createRow(startRow);
      }
      Cell cell = row.getCell(startColumn);
      if (cell == null) {
        cell = row.createCell(startColumn);
      }
      cell.setCellStyle(style);
      Object value = transformToType(footer.getText(), footer.getColumn());
      buildCell(value, cell, footer.getColumn(), null);
      configureAlignment(footer.getColumn(), cell, ExcelCellType.FOOTER);
      startColumn++;
    }
  }

}
