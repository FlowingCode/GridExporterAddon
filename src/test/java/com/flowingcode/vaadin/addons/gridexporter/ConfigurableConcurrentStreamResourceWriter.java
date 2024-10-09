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
