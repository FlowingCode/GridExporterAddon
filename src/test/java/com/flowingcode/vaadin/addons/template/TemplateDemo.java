package com.flowingcode.vaadin.addons.template;

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Template Addon Demo")
@SuppressWarnings("serial")
@Route(value="demo", layout=TemplateDemoView.class)
public class TemplateDemo extends Div {

  public TemplateDemo() {
    add(new TemplateAddon());
  }
}
