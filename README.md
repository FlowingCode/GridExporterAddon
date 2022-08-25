[![Published on Vaadin Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://vaadin.com/directory/component/grid-exporter-add-on)
[![Stars on vaadin.com/directory](https://img.shields.io/vaadin-directory/star/grid-exporter-add-on.svg)](https://vaadin.com/directory/component/grid-exporter-add-on)
[![Build Status](https://jenkins.flowingcode.com/job/GridExporter-addon/badge/icon)](https://jenkins.flowingcode.com/job/GridExporter-addon)

# Grid Exporter Add-on

This is an addon that allows to export Vaadin's grid data to some formats like Excel, Docx, PDF and CSV. It works with Apache POI 5.2.2, but it works partially (docx and pdf won't work) with older versions (for using an older versions you need to exclude the apache poi transitive dependencies from this projects and add them with the explicit version that you want to use).

## Features

* Can configure which columns to export
* Other templates can be used besides the basic templates
* Optionally can create the export buttons automatically (default)

## Online demo

[Online demo here](http://addonsv23.flowingcode.com/grid-exporter-addon)

## Download release

[Available in Vaadin Directory](https://vaadin.com/directory/component/grid-exporter-addon)

## Building and running demo

- git clone repository
- mvn clean install jetty:run

To see the demo, navigate to http://localhost:8080/

## Release notes

See [here](https://github.com/FlowingCode/GridExporterAddon/releases)

## Issue tracking

The issues for this add-on are tracked on its github.com page. All bug reports and feature requests are appreciated. 

## Contributions

Contributions are welcome, but there are no guarantees that they are accepted as such. Process for contributing is the following:

- Fork this project
- Create an issue to this project about the contribution (bug or feature) if there is no such issue about it already. Try to keep the scope minimal.
- Develop and test the fix or functionality carefully. Only include minimum amount of code needed to fix the issue.
- Refer to the fixed issue in commit
- Send a pull request for the original project
- Comment on the original issue that you have implemented a fix for it

## License & Author

Add-on is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

Grid Exporter Addon is written by Flowing Code S.A.

# Developer Guide

After creating a grid, the method createFor can be used to create the exporter that then can be configured. It is important that the grid should always be completely configured (headers, footers, etc.), before creating the exporter, because if not, it will not work properly.

    GridExporter<Person> exporter = GridExporter.createFor(grid, "/custom-template.xlsx", "/custom-template.docx");
    HashMap<String,String> placeholders = new HashMap<>();
    placeholders.put("${date}", new SimpleDateFormat().format(Calendar.getInstance().getTime()));
    exporter.setExportColumn(nameColumn, false);
    exporter.setExportColumn(lastNameColumn, true);
    exporter.setAdditionalPlaceHolders(placeholders);
    exporter.setSheetNumber(1);
    exporter.setCsvExportEnabled(false);
    exporter.setTitle("People information");
    exporter.setFileName("GridExport" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));

Custom templates can be added anywhere in the classpath (ie: src/main/resources)
