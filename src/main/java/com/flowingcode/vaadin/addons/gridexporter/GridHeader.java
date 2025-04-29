package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.component.grid.Grid.Column;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
final class GridHeader<T> implements GridHeaderOrFooter<T> {

  private final List<String> texts;
  private final Column<T> column;

  @Override
  public String getText() {
    return texts.get(0);
  }

}
