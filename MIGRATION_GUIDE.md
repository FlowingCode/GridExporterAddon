# Migration Guide: StreamResource to DownloadHandler

## Overview

Starting with version 2.6.0, Grid Exporter Add-on introduces new `DownloadHandler`-based methods to replace the deprecated `StreamResource` methods. This migration is necessary for compatibility with Vaadin 25, where `StreamResource` will be removed.

## What's Changing?

The Grid Exporter Add-on now provides two sets of methods:

- **Old API (Deprecated)**: Methods returning `StreamResource` or `GridExporterStreamResource`
- **New API (Recommended)**: Methods returning `DownloadHandler`

## Backward Compatibility

✅ **Your existing code will continue to work!** The old methods are deprecated but still functional. You can migrate at your own pace.

## Migration Steps

### 1. Update Vaadin Version

The new `DownloadHandler` API requires **Vaadin 24.8.0 or later**.

**pom.xml:**
```xml
<properties>
    <vaadin.version>24.8.0</vaadin.version>
</properties>
```

### 2. Update Method Calls

Replace the deprecated `get*StreamResource()` methods with the new `get*DownloadHandler()` methods.

#### Excel Export

**Before:**
```java
Anchor excelLink = new Anchor("", FontAwesome.Regular.FILE_EXCEL.create());
excelLink.setHref(exporter.getExcelStreamResource());
excelLink.getElement().setAttribute("download", true);
```

**After:**
```java
Anchor excelLink = new Anchor("", FontAwesome.Regular.FILE_EXCEL.create());
excelLink.setHref(exporter.getExcelDownloadHandler());
excelLink.getElement().setAttribute("download", true);
```

#### DOCX Export

**Before:**
```java
exporter.getDocxStreamResource()
exporter.getDocxStreamResource(customTemplate)
```

**After:**
```java
exporter.getDocxDownloadHandler()
exporter.getDocxDownloadHandler(customTemplate)
```

#### PDF Export

**Before:**
```java
exporter.getPdfStreamResource()
exporter.getPdfStreamResource(customTemplate)
```

**After:**
```java
exporter.getPdfDownloadHandler()
exporter.getPdfDownloadHandler(customTemplate)
```

#### CSV Export

**Before:**
```java
exporter.getCsvStreamResource()
```

**After:**
```java
exporter.getCsvDownloadHandler()
```

### 3. Update Custom Export Links (if applicable)

If you're creating custom export links instead of using auto-attached buttons:

**Before:**
```java
GridExporter<Person> exporter = GridExporter.createFor(grid);
exporter.setAutoAttachExportButtons(false);

Anchor customLink = new Anchor("", "Download Excel");
customLink.setHref(exporter.getExcelStreamResource().forComponent(customLink));
```

**After:**
```java
GridExporter<Person> exporter = GridExporter.createFor(grid);
exporter.setAutoAttachExportButtons(false);

Anchor customLink = new Anchor("", "Download Excel");
customLink.setHref(exporter.getExcelDownloadHandler());
// Note: forComponent() is handled internally for DownloadHandler
```

## API Comparison

| Old Method (Deprecated) | New Method | Notes |
|------------------------|------------|-------|
| `getExcelStreamResource()` | `getExcelDownloadHandler()` | Excel export |
| `getExcelStreamResource(String)` | `getExcelDownloadHandler(String)` | Excel with template |
| `getDocxStreamResource()` | `getDocxDownloadHandler()` | DOCX export |
| `getDocxStreamResource(String)` | `getDocxDownloadHandler(String)` | DOCX with template |
| `getPdfStreamResource()` | `getPdfDownloadHandler()` | PDF export |
| `getPdfStreamResource(String)` | `getPdfDownloadHandler(String)` | PDF with template |
| `getCsvStreamResource()` | `getCsvDownloadHandler()` | CSV export |

## Features Preserved

All existing features continue to work with the new API:

- ✅ Concurrent download control
- ✅ Download timeouts
- ✅ Button disable/enable during download
- ✅ Custom templates
- ✅ All export formats (Excel, DOCX, PDF, CSV)
- ✅ Custom columns and headers
- ✅ Hierarchical data support

## Timeline

- **Version 2.6.0**: New `DownloadHandler` methods introduced, old methods deprecated
- **Version 3.0.0**: Old `StreamResource` methods will be removed

## Need Help?

If you encounter any issues during migration, please:
1. Check that you're using Vaadin 24.8.0 or later
2. Review the deprecation warnings in your IDE
3. Open an issue on [GitHub](https://github.com/FlowingCode/GridExporterAddon/issues)

## Example: Complete Migration

**Before (Version 2.5.x):**
```java
Grid<Person> grid = new Grid<>(Person.class);
grid.setItems(people);

GridExporter<Person> exporter = GridExporter.createFor(grid);
// Auto-attached buttons use StreamResource internally
```

**After (Version 2.6.0+):**
```java
Grid<Person> grid = new Grid<>(Person.class);
grid.setItems(people);

GridExporter<Person> exporter = GridExporter.createFor(grid);
// Auto-attached buttons now use DownloadHandler internally
// No code changes needed if using auto-attached buttons!
```

**Custom Implementation:**
```java
Grid<Person> grid = new Grid<>(Person.class);
grid.setItems(people);

GridExporter<Person> exporter = GridExporter.createFor(grid);
exporter.setAutoAttachExportButtons(false);

// Create custom export buttons
Anchor excelBtn = new Anchor("", "Excel");
excelBtn.setHref(exporter.getExcelDownloadHandler());
excelBtn.getElement().setAttribute("download", true);

Anchor pdfBtn = new Anchor("", "PDF");
pdfBtn.setHref(exporter.getPdfDownloadHandler());
pdfBtn.getElement().setAttribute("download", true);

add(excelBtn, pdfBtn);
```
