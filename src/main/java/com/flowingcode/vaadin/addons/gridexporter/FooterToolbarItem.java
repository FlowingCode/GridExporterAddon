package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.component.Component;
import java.io.Serializable;

/**
 * Item that can be added to {@link FooterToolbar}.
 * <p>
 * Default position is {@link FooterToolbarItemPosition.AFTER_EXPORT_BUTTONS}
 */
public class FooterToolbarItem implements Serializable {

  private final Component component;

  private final FooterToolbarItemPosition position;

  public FooterToolbarItem(Component component) {
    this.component = component;
    this.position = FooterToolbarItemPosition.AFTER_EXPORT_BUTTONS;
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
