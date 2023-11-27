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
/** */
package com.flowingcode.vaadin.addons.gridexporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.PropertySet;
import com.vaadin.flow.data.provider.DataProvider;

/**
 * @author mlope
 */
@SuppressWarnings("serial")
class ExcelInputStreamFactory<T> extends BaseInputStreamFactory<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExcelInputStreamFactory.class);
  private static final String DEFAULT_TEMPLATE = "/template.xlsx";

  public ExcelInputStreamFactory(GridExporter<T> exporter, String template) {
    super(exporter, template, DEFAULT_TEMPLATE);
  }

  @Override
  public InputStream createInputStream() {
    PipedInputStream in = new PipedInputStream();
    try {
      exporter.setColumns(
          exporter.grid.getColumns().stream()
              .filter(this::isExportable)
              .collect(Collectors.toList()));
      Workbook wb = getBaseTemplateWorkbook();
      Sheet sheet = wb.getSheetAt(exporter.sheetNumber);

      Cell titleCell = findCellWithPlaceHolder(sheet, exporter.titlePlaceHolder);
      if (titleCell != null) {
        titleCell.setCellValue(exporter.title);
      }

      Cell cell = findCellWithPlaceHolder(sheet, exporter.headersPlaceHolder);
      List<Pair<String, Column<T>>> headers = getGridHeaders(exporter.grid);

      fillHeaderOrFooter(sheet, cell, headers, true);
      if (exporter.autoMergeTitle && titleCell != null) {
        sheet.addMergedRegion(
            new CellRangeAddress(
                titleCell.getRowIndex(),
                titleCell.getRowIndex(),
                titleCell.getColumnIndex(),
                titleCell.getColumnIndex() + headers.size() - 1));
      }

      cell = findCellWithPlaceHolder(sheet, exporter.dataPlaceHolder);
      int[] dataStartingColumn = new int[1];
      dataStartingColumn[0] = cell.getColumnIndex();

      // initialize the data range with tne coordinates of tha data placeholder cell
      CellRangeAddress dataRange = new CellRangeAddress(cell.getRowIndex(), cell.getRowIndex(), cell.getColumnIndex(), cell.getColumnIndex());
      
      Sheet tempSheet = wb.cloneSheet(exporter.sheetNumber);
      
      int lastRow = fillData(sheet, cell, exporter.grid.getDataProvider(), dataRange, titleCell != null);

      applyConditionalFormattings(sheet, dataRange);

      copyBottomOfSheetStartingOnRow(wb, tempSheet, sheet, cell.getRowIndex()+1, lastRow);
      
      wb.removeSheetAt(exporter.sheetNumber + 1);
      
      cell = findCellWithPlaceHolder(sheet, exporter.footersPlaceHolder);
      List<Pair<String, Column<T>>> footers = getGridFooters(exporter.grid);
      if (cell != null) {
        fillHeaderOrFooter(sheet, cell, footers, false);
      }

      if (exporter.isAutoSizeColumns()) {
        exporter
            .getColumns()
            .forEach(
                column -> {
                  sheet.autoSizeColumn(dataStartingColumn[0]);
                  dataStartingColumn[0]++;
                });
      }

      exporter
          .additionalPlaceHolders
          .entrySet()
          .forEach(
              entry -> {
                Cell cellwp;
                cellwp = findCellWithPlaceHolder(sheet, entry.getKey());
                while (cellwp != null) {
                  cellwp.setCellValue(entry.getValue());
                  cellwp = findCellWithPlaceHolder(sheet, entry.getKey());
                }
              });

      final PipedOutputStream out = new PipedOutputStream(in);
      new Thread(
              new Runnable() {
                public void run() {
                  try {
                    wb.write(out);
                  } catch (IOException e) {
                    LOGGER.error("Problem generating export", e);
                  } finally {
                    if (out != null) {
                      try {
                        out.close();
                      } catch (IOException e) {
                        LOGGER.error("Problem generating export", e);
                      }
                    }
                  }
                }
              })
          .start();
    } catch (IOException e) {
      LOGGER.error("Problem generating export", e);
    }
    return in;
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

    boolean[] notFirstRow = new boolean[1];
    Cell[] startingCell = new Cell[1];
    startingCell[0] = dataCell;
    dataStream.forEach(
        t -> {
          if (notFirstRow[0]) {
            CellStyle cellStyle = startingCell[0].getCellStyle();
            Row newRow = sheet.createRow(startingCell[0].getRowIndex() + 1);
            startingCell[0] = newRow.createCell(startingCell[0].getColumnIndex());
            startingCell[0].setCellStyle(cellStyle);
            // update the data range by updating last row
            dataRange.setLastRow(dataRange.getLastRow() + 1);
          }
          buildRow(t, sheet, startingCell[0]);
          notFirstRow[0] = true;
        });
    // since we initialized the cell range with the data placeholder cell, we use
    // the existing 'getLastColumn' to keep the offset of the data range
    dataRange.setLastColumn(dataRange.getLastColumn() + exporter.getColumns().size() - 1);
    return startingCell[0].getRowIndex();
  }

  @SuppressWarnings("unchecked")
  private void buildRow(T item, Sheet sheet, Cell startingCell) {
    if (exporter.propertySet == null) {
      exporter.propertySet = (PropertySet<T>) BeanPropertySet.get(item.getClass());
    }
    if (exporter.getColumns().isEmpty()) throw new IllegalStateException("Grid has no columns");

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
                CellStyle newCellStyle = currentCell.getSheet().getWorkbook().createCellStyle();
                newCellStyle.cloneStyleFrom(startingCell.getCellStyle());
                currentCell.setCellStyle(newCellStyle);

                configureAlignment(column.getTextAlign(), currentCell);
              }
              currentColumn[0] = currentColumn[0] + 1;
              buildCell(value, currentCell, column);
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

  protected void configureAlignment(ColumnTextAlign columnTextAlign, Cell currentCell) {
    switch (columnTextAlign) {
      case START:
        currentCell.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
        break;
      case CENTER:
        currentCell.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
        break;
      case END:
        currentCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
        break;
      default:
        currentCell.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
    }
  }

  private void buildCell(Object value, Cell cell, Column<T> column) {
    String excelFormat =
        (String) ComponentUtil.getData(column, GridExporter.COLUMN_EXCEL_FORMAT_DATA);
    if (value == null) {
      PoiHelper.setBlank(cell);
    } else if (value instanceof Number) {
      excelFormat = (excelFormat!=null)?excelFormat:"0";
      applyExcelFormat(cell, excelFormat);
      cell.setCellValue(((Number) value).doubleValue());
    } else if (value instanceof Date) {
      excelFormat = (excelFormat!=null)?excelFormat:"dd/MM/yyyy";
      applyExcelFormat(cell, excelFormat);
      cell.setCellValue((Date) value);
    } else if (value instanceof LocalDate) {
      excelFormat = (excelFormat!=null)?excelFormat:"dd/MM/yyyy";
      applyExcelFormat(cell, excelFormat);
      cell.setCellValue(
          Date.from(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    } else {
      cell.setCellValue(value.toString());
    }
  }

  private void applyExcelFormat(Cell cell, String excelFormat) {
    DataFormat format = cell.getSheet().getWorkbook().createDataFormat();
    cell.getCellStyle().setDataFormat(format.getFormat(excelFormat));
  }

  private Workbook getBaseTemplateWorkbook() {
    try {
      InputStream inp = this.getClass().getResourceAsStream(template);
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

  private void fillHeaderOrFooter(
      Sheet sheet,
      Cell headersOrFootersCell,
      List<Pair<String, Column<T>>> headersOrFooters,
      boolean isHeader) {
    CellStyle style = headersOrFootersCell.getCellStyle();
    sheet.setActiveCell(headersOrFootersCell.getAddress());
    headersOrFooters.forEach(
        headerOrFooter -> {
          Cell cell =
              sheet
                  .getRow(sheet.getActiveCell().getRow())
                  .getCell(sheet.getActiveCell().getColumn());
          if (cell == null) {
            cell =
                sheet
                    .getRow(sheet.getActiveCell().getRow())
                    .createCell(sheet.getActiveCell().getColumn());
            CellStyle newCellStyle = cell.getSheet().getWorkbook().createCellStyle();
            newCellStyle.cloneStyleFrom(style);
            cell.setCellStyle(newCellStyle);
          }
          Object value =
              (isHeader
                  ? headerOrFooter.getLeft()
                  : transformToType(headerOrFooter.getLeft(), headerOrFooter.getRight()));
          buildCell(value, cell, headerOrFooter.getRight());
          configureAlignment(headerOrFooter.getRight().getTextAlign(), cell);
          sheet.setActiveCell(
              new CellAddress(
                  sheet.getActiveCell().getRow(), sheet.getActiveCell().getColumn() + 1));
        });
  }
}
