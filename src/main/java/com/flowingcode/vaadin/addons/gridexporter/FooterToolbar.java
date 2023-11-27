package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 
 * Footer toolbar for export grid.
 *
 */
class FooterToolbar extends Composite<HorizontalLayout> {

  private final List<FooterToolbarItem> items = new ArrayList<>();

  public FooterToolbar() {
    addClassName("fc-ge-footer-toolbar");
    getContent().setSizeFull();
    getContent().setAlignItems(Alignment.CENTER);
  }

  public void add(FooterToolbarItem... items) {
    this.add(List.of(items));
  }

  public void add(List<FooterToolbarItem> items) {
    this.items.addAll(items);
  }

  /**
   * Checks if any {@link FooterToolbarItem} has been added.
   * 
   * @return true if has {@link FooterToolbarItem} added.
   */
  public boolean hasItems() {
    return !items.isEmpty();
  }


  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);

    items.stream().sorted(Comparator.comparing(FooterToolbarItem::getPosition))
        .map(FooterToolbarItem::getComponent).forEach(this.getContent()::add);
  }

}
