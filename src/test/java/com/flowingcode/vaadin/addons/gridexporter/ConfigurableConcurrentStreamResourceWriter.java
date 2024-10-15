package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinSession;

@SuppressWarnings("serial")
public abstract class ConfigurableConcurrentStreamResourceWriter
    extends ConcurrentStreamResourceWriter {

  public ConfigurableConcurrentStreamResourceWriter(StreamResourceWriter delegate) {
    super(delegate);
  }

  private float cost = GridExporter.DEFAULT_COST;
  private long timeout = 0L;
  private UI ui;

  @Override
  public float getCost(VaadinSession session) {
    return cost;
  }

  public void setCost(float cost) {
    this.cost = cost;
  }

  @Override
  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  @Override
  public UI getUI() {
    return ui;
  }

  public void setUi(UI ui) {
    this.ui = ui;
  }

}
