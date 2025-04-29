package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.component.grid.Grid.Column;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
final class GridFooter<T> implements GridHeaderOrFooter<T> {

  private final String text;
  private final Column<T> column;

  @Override
  public List<String> getTexts() {
    return List.of(text);
  }

}
