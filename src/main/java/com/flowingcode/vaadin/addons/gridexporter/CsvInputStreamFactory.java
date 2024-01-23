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
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.PropertySet;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
class CsvInputStreamFactory<T> extends BaseInputStreamFactory<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvInputStreamFactory.class);

  public CsvInputStreamFactory(GridExporter<T> exporter) {
    super(exporter, null, null);
  }

  @Override
  public InputStream createInputStream() {
    PipedInputStream in = new PipedInputStream();
    try {
      exporter.setColumns(
          exporter.grid.getColumns().stream()
              .filter(this::isExportable)
              .collect(Collectors.toList()));

      String[] headers =
          getGridHeaders(exporter.grid).stream().map(Pair::getLeft).toArray(String[]::new);
      List<String[]> data =
          obtainDataStream(exporter.grid.getDataProvider())
              .map(this::buildRow)
              .collect(Collectors.toList());
      String[] footers =
          getGridFooters(exporter.grid).stream()
              .filter(pair -> StringUtils.isNotBlank(pair.getKey()))
              .map(Pair::getLeft)
              .toArray(String[]::new);

      PipedOutputStream out = new PipedOutputStream(in);
      new Thread(
              () -> {
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
                } catch (IOException e) {
                  LOGGER.error("Problem generating export", e);
                } finally {
                  IOUtils.closeQuietly(out);
                }
              })
          .start();
    } catch (IOException e) {
      LOGGER.error("Problem generating export", e);
    }
    return in;
  }

  @SuppressWarnings("unchecked")
  private String[] buildRow(T item) {
    if (exporter.propertySet == null) {
      exporter.propertySet = (PropertySet<T>) BeanPropertySet.get(item.getClass());
    }
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
