package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.component.grid.Grid.Column;
import java.util.List;

interface GridHeaderOrFooter<T> {

  String getText();

  List<String> getTexts();

  Column<T> getColumn();

}
