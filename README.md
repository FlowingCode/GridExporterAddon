[![Published on Vaadin Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://vaadin.com/directory/component/grid-exporter-add-on)
[![Stars on vaadin.com/directory](https://img.shields.io/vaadin-directory/star/grid-exporter-add-on.svg)](https://vaadin.com/directory/component/grid-exporter-add-on)
[![Build Status](https://jenkins.flowingcode.com/job/GridExporter-addon/badge/icon)](https://jenkins.flowingcode.com/job/GridExporter-addon)
[![Javadoc](https://img.shields.io/badge/javadoc-00b4f0)](https://javadoc.flowingcode.com/artifact/org.vaadin.addons.flowingcode/grid-exporter-addon)

# Grid Exporter Add-on

This is an addon that allows to export Vaadin's grid data to some formats like Excel, Docx, PDF and CSV. It works with Apache POI 5.2.2, but it works partially (docx and pdf won't work) with older versions (for using an older versions you need to exclude the apache poi transitive dependencies from this projects and add them with the explicit version that you want to use).

## Features

* Can configure which columns to export
* Other templates can be used besides the basic templates
* Optionally can create the export buttons automatically (default)

## Online demo

[Online demo here](http://addonsv23.flowingcode.com/gridexporter)

## Download release

[Available in Vaadin Directory](https://vaadin.com/directory/component/grid-exporter-add-on)

### Maven install

Add the following dependencies in your pom.xml file:

```xml
<dependency>
   <groupId>org.vaadin.addons.flowingcode</groupId>
   <artifactId>grid-exporter-addon</artifactId>
   <version>X.Y.Z</version>
</dependency>
```
<!-- the above dependency should be updated with latest released version information -->

```xml
<repository>
   <id>vaadin-addons</id>
   <url>https://maven.vaadin.com/vaadin-addons</url>
</repository>
```

For SNAPSHOT versions see [here](https://maven.flowingcode.com/snapshots/).

## Building and running demo

- git clone repository
- mvn clean install jetty:run

To see the demo, navigate to http://localhost:8080/

## Release notes

See [here](https://github.com/FlowingCode/GridExporterAddon/releases)

## Issue tracking

The issues for this add-on are tracked on its github.com page. All bug reports and feature requests are appreciated. 

## Contributions

Contributions are welcome, but there are no guarantees that they are accepted as such. 

As first step, please refer to our [Development Conventions](https://github.com/FlowingCode/DevelopmentConventions) page to find information about Conventional Commits & Code Style requeriments.

Then, follow these steps for creating a contribution:

- Fork this project.
- Create an issue to this project about the contribution (bug or feature) if there is no such issue about it already. Try to keep the scope minimal.
- Develop and test the fix or functionality carefully. Only include minimum amount of code needed to fix the issue.
- For commit message, use [Conventional Commits](https://github.com/FlowingCode/DevelopmentConventions/blob/main/conventional-commits.md) to describe your change.
- Send a pull request for the original project.
- Comment on the original issue that you have implemented a fix for it.

## License & Author

This add-on is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

Grid Exporter Addon is written by Flowing Code S.A.

# Developer Guide

After creating a grid, the method createFor can be used to create the exporter that then can be configured. It is important that the grid should always be completely configured (headers, footers, etc.), before creating the exporter, because if not, it will not work properly. Another important consideration is to create the exporter before attaching the grid, creating the grid exporter after the grid is attached will raise an exception (see [this issue](https://github.com/FlowingCode/GridExporterAddon/issues/8))

    GridExporter<Person> exporter = GridExporter.createFor(grid, "/custom-template.xlsx", "/custom-template.docx");
    HashMap<String,String> placeholders = new HashMap<>();
    placeholders.put("${date}", new SimpleDateFormat().format(Calendar.getInstance().getTime()));
    exporter.setExportColumn(nameColumn, false);
    exporter.setExportColumn(lastNameColumn, true);
    exporter.setAdditionalPlaceHolders(placeholders);
    exporter.setSheetNumber(1);
    exporter.setCsvExportEnabled(false);
    exporter.setNumberColumnFormat(budgetColumn, "$#,###.##", "$#,###.##");
    exporter.setDateColumnFormat(dateColumn1, "dd/MM/yyyy");
    exporter.setTitle("People information");
    exporter.setFileName("GridExport" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));

Custom templates can be added anywhere in the classpath (ie: src/main/resources)

## Note when deploying with docker using Alpine images

Apply [this fix](https://github.com/docker-library/openjdk/issues/73#issuecomment-451102068) as mentioned in #15 to avoid the "Cannot load from short array because "sun.awt.FontConfiguration.head" is null error.

## Customizing Export Icon Tooltips

The GridExporter addon allows you to customize the tooltip text displayed when a user hovers over the export icons (Excel, DOCX, PDF, CSV).

You can set a default tooltip that applies to all export icons, or provide specific tooltips for individual icons.

### Setting Tooltips

1.  **Default Tooltip:**
    Use the `setDefaultExportIconTooltip(String tooltipText)` method to set a common tooltip for all export icons. This tooltip will be used unless a more specific one is set for a particular icon.

    ```java
    GridExporter<Person> exporter = GridExporter.createFor(grid);
    exporter.setDefaultExportIconTooltip("Export grid data");
    ```

2.  **Specific Tooltips:**
    You can set a unique tooltip for each export format using dedicated methods:
    *   `setExcelExportIconTooltip(String tooltipText)`
    *   `setDocxExportIconTooltip(String tooltipText)`
    *   `setPdfExportIconTooltip(String tooltipText)`
    *   `setCsvExportIconTooltip(String tooltipText)`

    A specific tooltip will always override the default tooltip for that particular icon.

    ```java
    // Specific tooltip for Excel, other icons (DOCX, PDF, CSV) will use the default if set.
    exporter.setExcelExportIconTooltip("Export to Excel spreadsheet (.xlsx)");
    ```

### Behavior and Precedence

*   A specific tooltip, when set for an icon (e.g., `setExcelExportIconTooltip("Specific")`), always takes precedence for that icon.
*   When an icon's specific tooltip is not set or is `null`, the default tooltip (set by `setDefaultExportIconTooltip("Default")`) will be applied.
*   Should neither a specific nor a default tooltip be configured for an icon (or if both are `null`), the icon will not display a tooltip (its `title` attribute will be removed).

### Removing or Clearing Tooltips

*   **Passing `null`**: If you pass `null` to a specific tooltip setter (e.g., `setExcelExportIconTooltip(null)`), that specific tooltip is removed. The icon will then attempt to use the default tooltip if one is set. If `null` is passed to `setDefaultExportIconTooltip(String)`, the default tooltip is removed. If an icon has no specific tooltip and the default is removed, it will have no tooltip.
*   **Passing an Empty String `""`**: If you pass an empty string to any tooltip setter (e.g., `setExcelExportIconTooltip("")`), the icon will have an intentionally blank tooltip (the `title` attribute will be present but empty). This overrides any default tooltip for that specific icon.

### Examples

```java
GridExporter<Person> exporter = GridExporter.createFor(grid);

// Example 1: Set a default tooltip for all export icons
exporter.setDefaultExportIconTooltip("Export grid data");
// All icons will show "Export grid data"

// Example 2: Set a specific tooltip for the Excel export icon
// This will override the default for the Excel icon only.
exporter.setExcelExportIconTooltip("Export to Excel spreadsheet (.xlsx)");
// Excel icon: "Export to Excel spreadsheet (.xlsx)"
// Other icons: "Export grid data"

// Example 3: Set a specific tooltip for PDF and a default for others
exporter.setDefaultExportIconTooltip("Download file");
exporter.setPdfExportIconTooltip("Download as PDF document");
// Excel, DOCX, CSV icons: "Download file"
// PDF icon: "Download as PDF document"

// Example 4: Clear specific tooltip for CSV; it falls back to default
exporter.setDefaultExportIconTooltip("Default export tooltip");
exporter.setExcelExportIconTooltip("Excel specific"); // Keep one specific for contrast
exporter.setCsvExportIconTooltip(null);
// CSV icon: "Default export tooltip"
// Excel icon: "Excel specific"
// Other icons (DOCX, PDF): "Default export tooltip"

// Example 5: Intentionally blank tooltip for DOCX
exporter.setDocxExportIconTooltip("");
// DOCX icon: Will have a title attribute, but it will be empty.
// Other icons: (Depends on default or other specific settings from previous examples)
```

## Special configuration when using Spring

By default, Vaadin Flow only includes ```com/vaadin/flow/component``` to be always scanned for UI components and views. For this reason, the addon might need to be whitelisted in order to display correctly. 

To do so, just add ```com.flowingcode``` to the ```vaadin.whitelisted-packages``` property in ```src/main/resources/application.properties```, like:

```vaadin.whitelisted-packages = com.vaadin,org.vaadin,dev.hilla,com.flowingcode```
 
More information on Spring whitelisted configuration [here](https://vaadin.com/docs/latest/integrations/spring/configuration/#configure-the-scanning-of-packages).
