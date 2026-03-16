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

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.HeaderRow.HeaderCell;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.server.VaadinSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mlope
 */
@SuppressWarnings("serial")
class DocxStreamResourceWriter<T> extends BaseStreamResourceWriter<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DocxStreamResourceWriter.class);

  private static final String DEFAULT_TEMPLATE = "/template.docx";

  public DocxStreamResourceWriter(GridExporter<T> exporter, String template) {
    super(exporter, template, DEFAULT_TEMPLATE);
  }

  @Override
  public void accept(OutputStream out, VaadinSession session) throws IOException {
    createDoc(session).write(out);
  }

  protected XWPFDocument createDoc(VaadinSession session) throws IOException {
    session.lock();
    try {
      return createDoc();
    } finally {
      session.unlock();
    }
  }

  private XWPFDocument createDoc() throws IOException {
    Grid<T> grid = exporter.getGrid();
    exporter.setColumns(
        grid.getColumns().stream()
            .filter(this::isExportable)
            .collect(Collectors.toList()));

    XWPFDocument doc = getBaseTemplateDoc();

    doc.getParagraphs()
        .forEach(
            paragraph -> {
              paragraph
                  .getRuns()
                  .forEach(
                      run -> {
                        String text = run.getText(0);
                        if (text != null && text.contains(exporter.titlePlaceHolder)) {
                          text = text.replace(exporter.titlePlaceHolder, exporter.title);
                          run.setText(text, 0);
                        }
                        for (Map.Entry<String, String> entry :
                            exporter.additionalPlaceHolders.entrySet()) {
                          if (text != null && text.contains(entry.getKey())) {
                            text = text.replace(entry.getKey(), entry.getValue());
                            run.setText(text, 0);
                          }
                        }
                      });
            });

    XWPFTable table = findTable(doc);
    PoiHelper.setWonCTTblWidth(table.getCTTbl().getTblPr().getTblW(), "9638");
    table
        .getCTTbl()
        .getTblPr()
        .getTblW()
        .setType(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.Enum.forString(
                "dxa"));

    table.getCTTbl().getTblGrid().getGridColList().clear();
    exporter
        .getColumnsOrdered()
        .forEach(
            col -> {
              CTTblGridCol cctblgridcol = table.getCTTbl().getTblGrid().addNewGridCol();
              PoiHelper.setWonCTTblGridCol(
                  cctblgridcol, "" + Math.round(9638 / exporter.getColumns().size()));
            });

    boolean allRows = exporter.getHeaderRowIndex() < 0;
    List<GridHeader<T>> headers = getGridHeaders(grid, allRows);
    XWPFTableCell cell = findCellWithPlaceHolder(table, exporter.headersPlaceHolder);
    if (cell != null) {
      fillHeaderRows(table, cell, headers, exporter.headersPlaceHolder, allRows);
    }

    cell = findCellWithPlaceHolder(table, exporter.dataPlaceHolder);
    fillData(table, cell, grid.getDataProvider());

    cell = findCellWithPlaceHolder(table, exporter.footersPlaceHolder);
    List<GridFooter<T>> footers = getGridFooters(grid);
    if (cell != null) {
      fillFooterRows(table, cell, footers, exporter.footersPlaceHolder);
    }
    return doc;
  }

  private void fillHeaderRows(XWPFTable table, XWPFTableCell headersPlaceholderCell,
      List<GridHeader<T>> allHeaders, String placeHolder, boolean allRows) {
    CTTcPr tcPr = headersPlaceholderCell.getCTTc().getTcPr();
    CTPPr ctpPr = headersPlaceholderCell.getParagraphs().iterator().next().getCTP().getPPr();
    CTRPr ctrPr = headersPlaceholderCell.getParagraphs().iterator().next().getRuns().isEmpty() ? null : headersPlaceholderCell.getParagraphs().iterator().next().getRuns().iterator().next().getCTR().getRPr();

    XWPFTableRow placeholderRow = headersPlaceholderCell.getTableRow();
    int startRow = table.getRows().indexOf(placeholderRow);
    int startColumn = placeholderRow.getTableCells().indexOf(headersPlaceholderCell);

    Grid<T> grid = exporter.getGrid();
    List<HeaderRow> gridHeaderRows = grid.getHeaderRows();
    int headerRowsCount = allRows ? gridHeaderRows.size() : 1;

    // Shift rows down to make space for all header rows, if necessary
    for (int r = 0; r < headerRowsCount - 1; r++) {
        table.createRow();
    }
    for (int r = table.getNumberOfRows() - 1; r > startRow + (headerRowsCount - 1); r--) {
        table.getCTTbl().setTrArray(r, table.getCTTbl().getTrArray(r - (headerRowsCount - 1)));
    }

    for (int i = 0; i < headerRowsCount; i++) {
        int gridRowIndex = allRows ? i : exporter.getHeaderRowIndex();
        if (gridRowIndex < 0) gridRowIndex = 0;
        HeaderRow hr = gridHeaderRows.get(gridRowIndex);
        XWPFTableRow row = table.getRow(startRow + i);
        if (row == null) {
            row = table.insertNewTableRow(startRow + i);
        }
        
        // Remove existing cells from the template row if it's the first header row we are writing
        while (row.getTableCells().size() > startColumn) {
            row.removeCell(row.getTableCells().size() - 1);
        }

        for (int j = 0; j < allHeaders.size(); j++) {
            GridHeader<T> gh = allHeaders.get(j);
            Column<T> column = gh.getColumn();
            HeaderCell hc = hr.getCell(column);

            boolean isFirst = true;
            int colSpan = 1;
            for (int k = j - 1; k >= 0; k--) {
                if (hr.getCell(allHeaders.get(k).getColumn()) == hc) {
                    isFirst = false;
                    break;
                }
            }

            if (isFirst) {
                for (int k = j + 1; k < allHeaders.size(); k++) {
                    if (hr.getCell(allHeaders.get(k).getColumn()) == hc) {
                        colSpan++;
                    } else {
                        break;
                    }
                }

                XWPFTableCell currentCell = row.createCell();
                currentCell.getCTTc().setTcPr(tcPr);
                PoiHelper.setWidth(currentCell, "" + Math.round(9638 / exporter.getColumns().size() * colSpan));
                
                String headerText = gh.getTexts().get(allRows ? i : 0);
                setCellValue(headerText, currentCell, placeHolder, ctpPr, ctrPr);
                setCellAlignment(currentCell, column.getTextAlign());
                
                if (colSpan > 1) {
                    // XWPF merging is done via CTHMerge
                    for (int k = 1; k < colSpan; k++) {
                        row.createCell().getCTTc().setTcPr(tcPr);
                    }
                    PoiHelper.mergeCellsHorizontal(row, j, j + colSpan - 1);
                }
            }
        }
    }
  }

  private void fillFooterRows(XWPFTable table, XWPFTableCell footersPlaceholderCell,
      List<GridFooter<T>> footers, String placeHolder) {
    CTTcPr tcPr = footersPlaceholderCell.getCTTc().getTcPr();
    CTPPr ctpPr = footersPlaceholderCell.getParagraphs().iterator().next().getCTP().getPPr();
    CTRPr ctrPr = footersPlaceholderCell.getParagraphs().iterator().next().getRuns().isEmpty() ? null : footersPlaceholderCell.getParagraphs().iterator().next().getRuns().iterator().next().getCTR().getRPr();

    XWPFTableRow tableRow = footersPlaceholderCell.getTableRow();
    int startColumn = tableRow.getTableCells().indexOf(footersPlaceholderCell);
    
    // Clear existing cells from placeholder onwards
    while (tableRow.getTableCells().size() > startColumn) {
        tableRow.removeCell(tableRow.getTableCells().size() - 1);
    }

    footers.forEach(
        footer -> {
          XWPFTableCell currentCell = tableRow.createCell();
          currentCell.getCTTc().setTcPr(tcPr);
          PoiHelper.setWidth(currentCell, "" + Math.round(9638 / exporter.getColumns().size()));
          setCellValue(footer.getText(), currentCell, placeHolder, ctpPr, ctrPr);
          setCellAlignment(currentCell, footer.getColumn().getTextAlign());
        });
  }

  private void fillData(XWPFTable table, XWPFTableCell dataCell, DataProvider<T, ?> dataProvider) {
    Stream<T> dataStream = obtainDataStream(dataProvider);

    boolean[] firstRow = new boolean[] {true};
    XWPFTableCell[] startingCell = new XWPFTableCell[1];
    startingCell[0] = dataCell;
    dataStream.forEach(
        t -> {
          XWPFTableRow currentRow = startingCell[0].getTableRow();
          if (!firstRow[0]) {
            currentRow =
                table.insertNewTableRow(
                    dataCell
                            .getTableRow()
                            .getTable()
                            .getRows()
                            .indexOf(startingCell[0].getTableRow())
                        + 1);
            Iterator<Column<T>> iterator = exporter.getColumns().iterator();
            while (iterator.hasNext()) {
              iterator.next();
              // preserve increment for deprecated attribute
              exporter.totalcells = exporter.totalcells + 1;
              currentRow.createCell();
            }
            startingCell[0] = currentRow.getCell(0);
          }
          buildRow(t, currentRow, startingCell[0], dataCell.getCTTc().getTcPr(), dataCell);
          firstRow[0] = false;
        });
  }

  private void buildRow(
      T item,
      XWPFTableRow row,
      XWPFTableCell startingCell,
      CTTcPr tcpr,
      XWPFTableCell templateCell) {

    if (exporter.getColumns().isEmpty()) {
      throw new IllegalStateException("Grid has no columns");
    }

    int[] currentColumn = new int[1];
    currentColumn[0] = row.getTableCells().indexOf(startingCell);
    exporter.getColumnsOrdered().stream()
        .forEach(
            column -> {
              Object value = exporter.extractValueFromColumn(item, column);

              XWPFTableCell currentCell = startingCell;
              if (row.getTableCells().indexOf(startingCell) < currentColumn[0]) {
                currentCell = startingCell.getTableRow().getCell(currentColumn[0]);
                if (currentCell == null) {
                  currentCell = startingCell.getTableRow().createCell();
                }
              }
              PoiHelper.setWidth(currentCell, "" + Math.round(9638 / exporter.getColumns().size()));
              currentCell.getCTTc().setTcPr(tcpr);
              currentColumn[0] = currentColumn[0] + 1;
              buildCell(
                  value,
                  currentCell,
                  templateCell.getParagraphs().iterator().next().getCTP().getPPr(),
                  templateCell
                      .getParagraphs()
                      .iterator()
                      .next()
                      .getRuns()
                      .isEmpty() ? null :
                  templateCell
                      .getParagraphs()
                      .iterator()
                      .next()
                      .getRuns()
                      .iterator()
                      .next()
                      .getCTR()
                      .getRPr());
              setCellAlignment(currentCell, column.getTextAlign());
            });
  }

  private void buildCell(Object value, XWPFTableCell cell, CTPPr ctpPr, CTRPr ctrPr) {
    if (value == null) {
      setCellValue("", cell, exporter.dataPlaceHolder, ctpPr, ctrPr);
    } else if (value instanceof Boolean) {
      setCellValue("" + value, cell, exporter.dataPlaceHolder, ctpPr, ctrPr);
    } else if (value instanceof Calendar) {
      Calendar calendar = (Calendar) value;
      setCellValue("" + calendar.getTime(), cell, exporter.dataPlaceHolder, ctpPr, ctrPr);
    } else if (value instanceof Double) {
      setCellValue("" + value, cell, exporter.dataPlaceHolder, ctpPr, ctrPr);
    } else {
      setCellValue("" + value.toString(), cell, exporter.dataPlaceHolder, ctpPr, ctrPr);
    }
  }

  private void setCellValue(String value, XWPFTableCell cell, String placeHolderToReplace) {
    setCellValue(value, cell, placeHolderToReplace, null, null);
  }

  private void setCellValue(
      String value, XWPFTableCell cell, String placeHolderToReplace, CTPPr ctpPr, CTRPr ctrPr) {
    XWPFParagraph p = cell.getParagraphs().get(0);
    if (ctpPr != null) {
      p.getCTP().setPPr(ctpPr);
    }
    if (p.getRuns().size() == 0) {
      XWPFRun run = p.createRun();
      if (ctrPr != null) {
        run.getCTR().setRPr(ctrPr);
      }
      run.setText(value);
    } else {
      p.getRuns()
          .forEach(
              run -> {
                String text = run.getText(0);
                if (text != null && text.contains(placeHolderToReplace)) {
                  text = text.replace(placeHolderToReplace, value);
                } else {
                  text = value;
                }
                run.setText(text, 0);
              });
    }
    p.setStyle("TextBody");
  }

  private void setCellAlignment(XWPFTableCell currentCell, ColumnTextAlign right) {
    currentCell
        .getParagraphs()
        .forEach(
            paragraph -> {
              switch (right) {
                case START:
                  paragraph.setAlignment(ParagraphAlignment.START);
                  break;
                case CENTER:
                  paragraph.setAlignment(ParagraphAlignment.CENTER);
                  break;
                case END:
                  paragraph.setAlignment(ParagraphAlignment.END);
                  break;
                default:
                  paragraph.setAlignment(ParagraphAlignment.START);
              }
            });
  }

  private XWPFTableCell findCellWithPlaceHolder(XWPFTable table, String placeHolder) {
    for (XWPFTableRow row : table.getRows()) {
      for (XWPFTableCell cell : row.getTableCells()) {
        if (cell.getText().equals(placeHolder)) {
          return cell;
        }
      }
    }
    return null;
  }

  private XWPFTable findTable(XWPFDocument doc) {
    XWPFTable[] result = new XWPFTable[1];
    doc.getTables()
        .forEach(
            table -> {
              boolean[] foundHeaders = new boolean[1];
              boolean[] foundData = new boolean[1];
              table
                  .getRows()
                  .forEach(
                      row -> {
                        XWPFTableCell cell = row.getCell(0);
                        if (cell != null && cell.getText().equals(exporter.headersPlaceHolder)) {
                          foundHeaders[0] = true;
                        }
                        if (cell != null && cell.getText().equals(exporter.dataPlaceHolder)) {
                          foundData[0] = true;
                        }
                      });
              if (foundHeaders[0] && foundData[0]) {
                result[0] = table;
              }
            });
    return result[0];
  }

  private XWPFDocument getBaseTemplateDoc() throws EncryptedDocumentException, IOException {
    InputStream inp = this.getClass().getResourceAsStream(getTemplate());
    return new XWPFDocument(inp);
  }
}
