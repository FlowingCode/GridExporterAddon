/**
 * 
 */
package com.flowingcode.vaadin.addons.gridexporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.opencsv.CSVWriter;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.PropertySet;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.Query;

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
      exporter.columns =
          exporter.grid.getColumns().stream().filter(this::isExportable).collect(Collectors.toList());
      final PipedOutputStream out = new PipedOutputStream(in);
      new Thread(new Runnable() {
        public void run() {
          try {
            CSVWriter writer = new CSVWriter(new OutputStreamWriter(out));
            List<Pair<String, Column<T>>> headers = getGridHeaders(exporter.grid);
            writer.writeNext(headers.stream().map(pair->pair.getLeft()).collect(Collectors.toList()).toArray(new String[0]));
            
            Object filter = null;
            try {
              Method method = DataCommunicator.class.getDeclaredMethod("getFilter");
              method.setAccessible(true);
              filter = method.invoke(exporter.grid.getDataCommunicator());
            } catch (Exception e) {
              LOGGER.error("Unable to get filter from DataCommunicator", e);
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            Query<T, ?> streamQuery = new Query<>(0, exporter.grid.getDataProvider().size(new Query(filter)),
                exporter.grid.getDataCommunicator().getBackEndSorting(),
                exporter.grid.getDataCommunicator().getInMemorySorting(), null);
            Stream<T> dataStream = getDataStream(streamQuery);
            dataStream.forEach(t -> {
              writer.writeNext(buildRow(t,writer));
            });
            List<Pair<String,Column<T>>> footers = getGridFooters(exporter.grid);
            writer.writeNext(footers.stream().map(pair->pair.getLeft()).collect(Collectors.toList()).toArray(new String[0]));
            
            writer.close();
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
      }).start();
    } catch (IOException e) {
      LOGGER.error("Problem generating export", e);
    }
    return in;
  }
  
  
  @SuppressWarnings("unchecked")
  private String[] buildRow(T item, CSVWriter writer) {
    if (exporter.propertySet == null) {
      exporter.propertySet = (PropertySet<T>) BeanPropertySet.get(item.getClass());
    }
    if (exporter.columns.isEmpty())
      throw new IllegalStateException("Grid has no columns");

    String[] result = new String[exporter.columns.size()];
    int[] currentColumn = new int[1];
    exporter.columns.forEach(column -> {
      Object value = exporter.extractValueFromColumn(item, column);

      result[currentColumn[0]]=""+value;
      currentColumn[0] = currentColumn[0] + 1;
    });
    return result;
  }

 
}