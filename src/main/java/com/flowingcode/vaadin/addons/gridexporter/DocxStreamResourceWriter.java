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

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
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
import org.apache.commons.lang3.tuple.Pair;
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

    List<Pair<String, Column<T>>> headers = getGridHeaders(grid);
    XWPFTableCell cell = findCellWithPlaceHolder(table, exporter.headersPlaceHolder);
    if (cell != null) {
      fillHeaderOrFooter(table, cell, headers, true, exporter.headersPlaceHolder);
    }

    cell = findCellWithPlaceHolder(table, exporter.dataPlaceHolder);
    fillData(table, cell, grid.getDataProvider());

    cell = findCellWithPlaceHolder(table, exporter.footersPlaceHolder);
    List<Pair<String, Column<T>>> footers = getGridFooters(grid);
    if (cell != null) {
      fillHeaderOrFooter(table, cell, footers, false, exporter.footersPlaceHolder);
    }
    return doc;
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
                if (text.indexOf(placeHolderToReplace) > 0) {
                  text = text.replace(placeHolderToReplace, value);
                } else {
                  text = value;
                }
                run.setText(text, 0);
              });
    }
    p.setStyle("TextBody");
  }

  private void fillHeaderOrFooter(
      XWPFTable table,
      XWPFTableCell cell,
      List<Pair<String, Column<T>>> headers,
      boolean createColumns,
      String placeHolder) {
    boolean[] firstHeader = new boolean[] {true};
    XWPFTableRow tableRow = cell.getTableRow();
    headers.forEach(
        header -> {
          if (!firstHeader[0]) {
            XWPFTableCell currentCell = tableRow.addNewTableCell();
            currentCell.getCTTc().setTcPr(cell.getCTTc().getTcPr());
            PoiHelper.setWidth(currentCell, "" + Math.round(9638 / exporter.getColumns().size()));
            setCellValue(
                header.getLeft(),
                currentCell,
                placeHolder,
                cell.getParagraphs().iterator().next().getCTP().getPPr(),
                cell.getParagraphs()
                    .iterator()
                    .next()
                    .getRuns()
                    .iterator()
                    .next()
                    .getCTR()
                    .getRPr());
            setCellAlignment(currentCell, header.getRight().getTextAlign());
          } else {
            setCellValue(header.getLeft(), cell, placeHolder);
            setCellAlignment(cell, header.getRight().getTextAlign());
            PoiHelper.setWidth(cell, "" + Math.round(9638 / exporter.getColumns().size()));
            firstHeader[0] = false;
          }
        });
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
                        if (cell.getText().equals(exporter.headersPlaceHolder)) {
                          foundHeaders[0] = true;
                        }
                        if (cell.getText().equals(exporter.dataPlaceHolder)) {
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
