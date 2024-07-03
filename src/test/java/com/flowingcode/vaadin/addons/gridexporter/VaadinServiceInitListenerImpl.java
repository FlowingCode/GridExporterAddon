package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import java.util.concurrent.TimeUnit;

public class VaadinServiceInitListenerImpl implements VaadinServiceInitListener {

  @Override
  public void serviceInit(ServiceInitEvent event) {
    GridExporter.setConcurrentDownloadLimit(10);

    // begin-block setConcurrentDownloadTimeout
    GridExporter.setConcurrentDownloadTimeout(5, TimeUnit.SECONDS);
    GridExporter.addGlobalConcurrentDownloadTimeoutEvent(ev -> {
      Notification.show("System is busy. Please try again later.")
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    });
    // end-block

  }

}
