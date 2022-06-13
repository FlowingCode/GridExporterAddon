package com.flowingcode.vaadin.addons.gridexporter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;
import com.flowingcode.vaadin.addons.gridhelpers.GridHelper;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.server.InputStreamFactory;

@SuppressWarnings("serial")
abstract class BaseInputStreamFactory<T> implements InputStreamFactory {

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
  
  protected List<String> getGridHeaders(Grid<T> grid) {
    return exporter.columns.stream().map(column -> GridHelper.getHeader(grid,column))
        .collect(Collectors.toList());
  }

  protected List<String> getGridFooters(Grid<T> grid) {
    return exporter.columns.stream().map(column -> GridHelper.getFooter(grid,column))
        .collect(Collectors.toList());
  }


}
