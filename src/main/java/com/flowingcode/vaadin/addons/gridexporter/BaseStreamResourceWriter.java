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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.server.StreamResourceWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
abstract class BaseStreamResourceWriter<T> implements StreamResourceWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseStreamResourceWriter.class);

  protected final GridExporter<T> exporter;
  private String template;

  public BaseStreamResourceWriter(GridExporter<T> exporter) {
    super();
    this.exporter = exporter;
  }

  public BaseStreamResourceWriter(
      GridExporter<T> exporter, String customTemplate, String defaultTemplate) {
    super();
    this.exporter = exporter;
    template = customTemplate == null ? defaultTemplate : customTemplate;
  }

  protected String getTemplate() {
    return template;
  }

  /**
   * If a column was configured to be exported or not, that will be honored. If not, it will
   * exported based on the visibility
   *
   * @param column
   * @return
   */
  protected boolean isExportable(Grid.Column<T> column) {
    Boolean exported =
        (Boolean) ComponentUtil.getData(column, GridExporter.COLUMN_EXPORTED_PROVIDER_DATA);
    return exported != null ? exported : column.isVisible();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected Stream<T> getDataStream(Query newQuery) {
    Stream<T> stream = exporter.getGrid().getDataProvider().fetch(newQuery);
    if (stream.isParallel()) {
      LoggerFactory.getLogger(DataCommunicator.class)
          .debug(
              "Data provider {} has returned " + "parallel stream on 'fetch' call",
              exporter.getGrid().getDataProvider().getClass());
      stream = stream.collect(Collectors.toList()).stream();
      assert !stream.isParallel();
    }
    return stream;
  }

  protected List<Pair<String, Column<T>>> getGridHeaders(Grid<T> grid) {
    return exporter.getColumnsOrdered().stream()
        .map(
            column ->
                ImmutablePair.of(
                    renderCellTextContent(grid, column, GridExporter.COLUMN_HEADER), column))
        .collect(Collectors.toList());
  }

  protected List<Pair<String, Column<T>>> getGridFooters(Grid<T> grid) {
    return exporter.getColumnsOrdered().stream()
        .map(
            column ->
                ImmutablePair.of(
                    renderCellTextContent(grid, column, GridExporter.COLUMN_FOOTER), column))
        .collect(Collectors.toList());
  }

  private String renderCellTextContent(Grid<T> grid, Column<T> column, String columnType) {
    String headerOrFooter = (String) ComponentUtil.getData(column, columnType);
    if (Strings.isBlank(headerOrFooter)) {
      Function<Column<?>, Component> getHeaderOrFooterComponent;
      if (GridExporter.COLUMN_HEADER.equals(columnType)) {
        getHeaderOrFooterComponent = Column::getHeaderComponent;
        headerOrFooter = column.getHeaderText();
      } else if (GridExporter.COLUMN_FOOTER.equals(columnType)) {
        getHeaderOrFooterComponent = Column::getFooterComponent;
        headerOrFooter = column.getFooterText();
      } else {
        throw new IllegalArgumentException();
      }
      if (Strings.isBlank(headerOrFooter)) {
        try {
          Component component = getHeaderOrFooterComponent.apply(column);
          if (component != null) {
            headerOrFooter = component.getElement().getTextRecursively();
          }
        } catch (RuntimeException e) {
          throw new IllegalStateException(
              "Problem when trying to render header or footer cell text content", e);
        }
      }
    }

    return headerOrFooter;
  }

  protected Stream<T> obtainDataStream(DataProvider<T, ?> dataProvider) {
    Grid<T> grid = exporter.getGrid();

    Object filter = null;
    try {
      Method method = DataCommunicator.class.getDeclaredMethod("getFilter");
      method.setAccessible(true);
      filter = method.invoke(grid.getDataCommunicator());
    } catch (Exception e) {
      LOGGER.error("Unable to get filter from DataCommunicator", e);
    }

    Stream<T> dataStream;

    // special handling for hierarchical data provider
    if (grid.getDataProvider() instanceof HierarchicalDataProvider) {
      return obtainFlattenedHierarchicalDataStream(grid);
    } else if (dataProvider instanceof AbstractBackEndDataProvider) {
      GridLazyDataView<T> gridLazyDataView = grid.getLazyDataView();
      dataStream = gridLazyDataView.getItems();
    } else {
      @SuppressWarnings({"rawtypes", "unchecked"})
      Query<T, ?> streamQuery =
          new Query<>(
              0,
              grid.getDataProvider().size(new Query(filter)),
              grid.getDataCommunicator().getBackEndSorting(),
              grid.getDataCommunicator().getInMemorySorting(),
              filter);
      dataStream = getDataStream(streamQuery);
    }
    return dataStream;
  }

  private Stream<T> obtainFlattenedHierarchicalDataStream(final Grid<T> grid) {
    ArrayList<T> flattenedData = fetchDataRecursive(grid, null);
    return flattenedData.stream();
  }

  private ArrayList<T> fetchDataRecursive(final Grid<T> grid, T parent) {
    ArrayList<T> result = new ArrayList<>();

    if (parent != null) {
      result.add(parent);
    }

    HierarchicalDataProvider<T, ?> hDataProvider =
        (HierarchicalDataProvider<T, ?>) grid.getDataProvider();

    int childCount =
        hDataProvider.getChildCount(
            new HierarchicalQuery<>(
                0,
                Integer.MAX_VALUE,
                grid.getSortOrder().stream()
                    .flatMap(so -> so.getSorted().getSortOrder(so.getDirection()))
                    .collect(Collectors.toList()),
                grid.getDataCommunicator().getInMemorySorting(),
                null,
                parent));

    if (childCount > 0) {
      hDataProvider
          .fetchChildren(
              new HierarchicalQuery<>(
                  0,
                  Integer.MAX_VALUE,
                  grid.getSortOrder().stream()
                      .flatMap(so -> so.getSorted().getSortOrder(so.getDirection()))
                      .collect(Collectors.toList()),
                  grid.getDataCommunicator().getInMemorySorting(),
                  null,
                  parent))
          .forEach(
              child -> {
                ArrayList<T> subTree = fetchDataRecursive(grid, child);
                result.addAll(subTree);
              });
    }

    return result;
  }
}
