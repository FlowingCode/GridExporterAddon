package com.flowingcode.vaadin.addons.gridexporter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.flowingcode.vaadin.addons.gridhelpers.GridHelper;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.server.InputStreamFactory;

@SuppressWarnings("serial")
abstract class BaseInputStreamFactory<T> implements InputStreamFactory {

  private final static Logger LOGGER = LoggerFactory.getLogger(BaseInputStreamFactory.class);
  protected GridExporter<T> exporter;
  protected String template;

  public BaseInputStreamFactory(GridExporter<T> exporter) {
    super();
    this.exporter = exporter;
  }
  
  public BaseInputStreamFactory(GridExporter<T> exporter, String customTemplate, String defaultTemplate) {
    super();
    this.exporter = exporter;
    this.template = customTemplate==null?defaultTemplate:customTemplate;
  }

  /**
   * If a column was configured to be exported or not, that will be honored.
   * If not, it will exported based on the visibility
   * @param column
   * @return
   */
  protected boolean isExportable(Grid.Column<T> column) {
    Boolean exported = (Boolean) ComponentUtil.getData(column, GridExporter.COLUMN_EXPORTED_PROVIDER_DATA);
    return exported!=null?exported:column.isVisible();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected Stream<T> getDataStream(Query newQuery) {
    Stream<T> stream = exporter.grid.getDataProvider().fetch(newQuery);
    if (stream.isParallel()) {
      LoggerFactory.getLogger(DataCommunicator.class).debug(
          "Data provider {} has returned " + "parallel stream on 'fetch' call",
          exporter.grid.getDataProvider().getClass());
      stream = stream.collect(Collectors.toList()).stream();
      assert !stream.isParallel();
    }
    return stream;
  }
  
  protected List<Pair<String,Column<T>>> getGridHeaders(Grid<T> grid) {
    return exporter.columns.stream().map(column -> ImmutablePair.of(GridHelper.getHeader(grid,column),column))
        .collect(Collectors.toList());
  }

  protected List<Pair<String,Column<T>>> getGridFooters(Grid<T> grid) {
    return exporter.columns.stream().map(column -> ImmutablePair.of(GridHelper.getFooter(grid,column),column))
        .collect(Collectors.toList());
  }

  protected Stream<T> obtainDataStream(DataProvider<T, ?> dataProvider) {
    Object filter = null;
    try {
      Method method = DataCommunicator.class.getDeclaredMethod("getFilter");
      method.setAccessible(true);
      filter = method.invoke(exporter.grid.getDataCommunicator());
    } catch (Exception e) {
      LOGGER.error("Unable to get filter from DataCommunicator", e);
    }

    Stream<T> dataStream;
    if (dataProvider instanceof AbstractBackEndDataProvider) {
      GridLazyDataView<T> gridLazyDataView = exporter.grid.getLazyDataView();
      dataStream = gridLazyDataView.getItems();
    } else {
      @SuppressWarnings({"rawtypes", "unchecked"})
      Query<T, ?> streamQuery =
          new Query<>(0, exporter.grid.getDataProvider().size(new Query(filter)),
              exporter.grid.getDataCommunicator().getBackEndSorting(),
              exporter.grid.getDataCommunicator().getInMemorySorting(), null);
      dataStream = getDataStream(streamQuery);
    }
    return dataStream;
  }
}
