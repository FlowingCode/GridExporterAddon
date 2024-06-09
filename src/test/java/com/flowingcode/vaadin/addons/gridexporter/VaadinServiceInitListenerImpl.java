package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import java.util.concurrent.TimeUnit;

public class VaadinServiceInitListenerImpl implements VaadinServiceInitListener {

  @Override
  public void serviceInit(ServiceInitEvent event) {
    GridExporter.setConcurrentDownloadLimit(10);
    GridExporter.setConcurrentDownloadTimeout(5, TimeUnit.SECONDS);
  }

}
