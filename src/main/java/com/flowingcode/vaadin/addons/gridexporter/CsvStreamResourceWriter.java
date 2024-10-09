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

import com.opencsv.CSVWriter;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.server.VaadinSession;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mlope
 */
@SuppressWarnings("serial")
class CsvStreamResourceWriter<T> extends BaseStreamResourceWriter<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvStreamResourceWriter.class);

  public CsvStreamResourceWriter(GridExporter<T> exporter) {
    super(exporter, null, null);
  }

  @Override
  public void accept(OutputStream out, VaadinSession session) throws IOException {

    String[] headers;
    List<String[]> data;
    String[] footers;

    session.lock();
    try {
      Grid<T> grid = exporter.getGrid();
      exporter.setColumns(
          grid.getColumns().stream()
          .filter(this::isExportable)
          .collect(Collectors.toList()));

      headers = getGridHeaders(grid).stream().map(Pair::getLeft).toArray(String[]::new);
      data = obtainDataStream(grid.getDataProvider())
          .map(this::buildRow)
          .collect(Collectors.toList());
      footers = getGridFooters(grid).stream()
          .filter(pair -> StringUtils.isNotBlank(pair.getKey()))
          .map(Pair::getLeft)
          .toArray(String[]::new);
    } finally {
      session.unlock();
    }

    try (
        OutputStreamWriter os = new OutputStreamWriter(out, exporter.getCsvCharset());
        CSVWriter writer = new CSVWriter(os)) {
      if (StandardCharsets.UTF_8.equals(exporter.getCsvCharset())) {
        // write BOM
        os.write(0xfeff);
      }

      writer.writeNext(headers);
      writer.writeAll(data);
      if (footers.length > 0) {
        writer.writeNext(footers);
      }
    }
  }

  private String[] buildRow(T item) {

    if (exporter.getColumns().isEmpty()) throw new IllegalStateException("Grid has no columns");

    String[] result = new String[exporter.getColumns().size()];
    int[] currentColumn = new int[1];
    exporter.getColumnsOrdered().stream()
        .forEach(
            column -> {
              Object value = exporter.extractValueFromColumn(item, column);

              result[currentColumn[0]] = "" + value;
              currentColumn[0] = currentColumn[0] + 1;
            });
    return result;
  }
}
