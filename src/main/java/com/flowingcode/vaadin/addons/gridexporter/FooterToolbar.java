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
    getElement().getClassList().add("fc-ge-footer-toolbar");
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
