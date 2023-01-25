/**
 * 
 */
package com.flowingcode.vaadin.addons.gridexporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.opencsv.CSVWriter;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.PropertySet;

/**
 * @author mlope
 *
 */
@SuppressWarnings("serial")
class CsvInputStreamFactory<T> extends BaseInputStreamFactory<T> {

  private final static Logger LOGGER = LoggerFactory.getLogger(CsvInputStreamFactory.class);
  
  public CsvInputStreamFactory(GridExporter<T> exporter) {
    super(exporter, null, null);
  }

  @Override
  public InputStream createInputStream() {
    PipedInputStream in = new PipedInputStream();
    try {
      exporter.columns = exporter.grid.getColumns().stream().filter(this::isExportable)
          .collect(Collectors.toList());

      String[] headers =
          getGridHeaders(exporter.grid).stream().map(Pair::getLeft).toArray(String[]::new);
      List<String[]> data = obtainDataStream(exporter.grid.getDataProvider())
          .map(this::buildRow).collect(Collectors.toList());
      String[] footers = getGridFooters(exporter.grid).stream()
          .filter(pair -> StringUtils.isNotBlank(pair.getKey()))
          .map(Pair::getLeft).toArray(String[]::new);

      PipedOutputStream out = new PipedOutputStream(in);
      new Thread(() -> {
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out))) {
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
      }).start();
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
    if (exporter.columns.isEmpty())
      throw new IllegalStateException("Grid has no columns");

    String[] result = new String[exporter.columns.size()];
    int[] currentColumn = new int[1];
    exporter.columns.forEach(column -> {
      Object value = exporter.extractValueFromColumn(item, column);

      result[currentColumn[0]] = "" + value;
      currentColumn[0] = currentColumn[0] + 1;
    });
    return result;
  }

}
