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
import java.io.Serializable;

/**
 * Item that can be added to {@link FooterToolbar}.
 * <p>
 * Default position is {@link FooterToolbarItemPosition#AFTER_EXPORT_BUTTONS}
 */
public class FooterToolbarItem implements Serializable {

  private final Component component;

  private final FooterToolbarItemPosition position;

  public FooterToolbarItem(Component component) {
    this.component = component;
    position = FooterToolbarItemPosition.AFTER_EXPORT_BUTTONS;
  }

  public FooterToolbarItem(Component component, FooterToolbarItemPosition position) {
    this.component = component;
    this.position = position;
  }

  public Component getComponent() {
    return component;
  }

  public FooterToolbarItemPosition getPosition() {
    return position;
  }

}
