package com.flowingcode.vaadin.addons.gridexporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.data.binder.BeanPropertySet;
import com.vaadin.flow.data.binder.PropertyDefinition;
import com.vaadin.flow.data.binder.PropertySet;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.server.StreamResource;

public class GridExporter<T> {

	private final static Logger LOGGER = LoggerFactory.getLogger(GridExporter.class);

	private Grid<T> grid;
	
	private String titlePlaceHolder = "${title}";
	private String headersPlaceHolder = "${headers}";
	private String dataPlaceHolder = "${data}";
	private String footersPlaceHolder = "${footers}";
	
    private Collection<Grid.Column<T>> columns;
    private PropertySet<T> propertySet;
	
	private Map<String,String> additionalPlaceHolders = new HashMap<>();
	
	private String title = "Grid Export";
	
	private String fileName = "export";
	
	private int sheetNumber = 0;
	
	private boolean autoAttachExportButtons = true;
	
	private boolean autoMergeTitle = true;
	
	private GridExporter(Grid<T> grid) {
		this.grid = grid;
	}

	public static <T> GridExporter<T> createFor(Grid<T> grid) {
		GridExporter<T> exporter = new GridExporter<>(grid);
		grid.getElement().addAttachListener(ev->{
			Element gridElement = ev.getSource();
			Element parent = gridElement.getParent();
			int gridPosition = parent.indexOfChild(gridElement);
		    Anchor link = new Anchor("",FontAwesome.Solid.FILE_EXCEL.create());
		    link.setHref(exporter.getExcelStreamResource());
			parent.insertChild(gridPosition+1, link.getElement());
		    link.getElement().setAttribute("download", true);
		});
		return exporter;
	}
	
	public StreamResource getExcelStreamResource() {
		return new StreamResource(fileName + ".xlsx", () -> {
			PipedInputStream in = new PipedInputStream();
			try {
				this.columns = grid.getColumns().stream().filter(this::isExportable).collect(Collectors.toList());
				Workbook wb = getWorkbook();
				Sheet sheet = wb.getSheetAt(sheetNumber);

				Cell titleCell = findCellWithPlaceHolder(sheet,titlePlaceHolder);
				titleCell.setCellValue(title);
				
				Cell cell = findCellWithPlaceHolder(sheet,headersPlaceHolder);
				List<String> headers = getGridHeaders(grid);
				fillHeaderOrFooter(sheet,cell,headers);
				if (this.isAutoMergeTitle()) {
					sheet.addMergedRegion(new CellRangeAddress(titleCell.getRowIndex(),titleCell.getRowIndex(), titleCell.getColumnIndex(), titleCell.getColumnIndex() + headers.size() - 1));
				}
				
				cell = findCellWithPlaceHolder(sheet,dataPlaceHolder);
				fillData(sheet, cell, grid.getDataProvider());

				cell = findCellWithPlaceHolder(sheet,footersPlaceHolder);
				List<String> footers = getGridFooters(grid);
				fillHeaderOrFooter(sheet, cell, footers);
				
				getAdditionalPlaceHolders().entrySet().forEach(entry->{
					Cell cellwp = findCellWithPlaceHolder(sheet,entry.getKey());
					cellwp.setCellValue(entry.getValue());
				});

				final PipedOutputStream out = new PipedOutputStream(in);
				new Thread(new Runnable() {
					public void run() {
						try {
							wb.write(out);
						} catch (IOException e) {
							LOGGER.error("Problem generating export", e);
						} finally {
							if (out != null) {
								try {
									out.close();
								} catch (IOException e) {
									LOGGER.error("Problem generating export", e);
								}
							}
						}
					}
				}).start();
			} catch (IOException e) {
				LOGGER.error("Problem generating export", e);
			}
			return in;
		});
	}

	@SuppressWarnings("unchecked")
	private void fillData(Sheet sheet, Cell dataCell, DataProvider<T, ?> dataProvider) {
        Object filter = null;
        try {
            Method method = DataCommunicator.class.getDeclaredMethod("getFilter");
            method.setAccessible(true);
            filter = method.invoke(grid.getDataCommunicator());
        } catch (Exception e) {
            LOGGER.error("Unable to get filter from DataCommunicator");
        }

        @SuppressWarnings("rawtypes")
		Query<T,?> streamQuery = new Query<>(0, grid.getDataProvider().size(new Query(filter)), grid.getDataCommunicator().getBackEndSorting(),
                grid.getDataCommunicator().getInMemorySorting(), null);
        Stream<T> dataStream = getDataStream(streamQuery);

        boolean[] notFirstRow = new boolean[1];
    	Cell[] startingCell = new Cell[1];
    	startingCell[0] = dataCell;
        dataStream.forEach(t -> {
        	if(notFirstRow[0]) {
        		CellStyle cellStyle = startingCell[0].getCellStyle();
        		int lastRow = sheet.getLastRowNum(); 
        		sheet.shiftRows(startingCell[0].getRowIndex()+1, lastRow, 1);
        		Row newRow = sheet.createRow(startingCell[0].getRowIndex()+1);
        		startingCell[0] = newRow.createCell(startingCell[0].getColumnIndex());
        		startingCell[0].setCellStyle(cellStyle);
        	}
            buildRow(t, sheet, startingCell[0]);
            notFirstRow[0]=true;
        });		
		
	}
    
    @SuppressWarnings("unchecked")
	private void buildRow(T item,Sheet sheet, Cell startingCell) {
        if (propertySet == null) {
            propertySet = (PropertySet<T>) BeanPropertySet.get(item.getClass());
            columns = columns.stream().filter(this::isExportable).collect(Collectors.toList());
        }
        if (columns.isEmpty())
        	throw new IllegalStateException("Grid has no columns");

        int[] currentColumn = new int[1];
        currentColumn[0] = startingCell.getColumnIndex();
        columns.forEach(column -> {
        	Object value = null;
        	if (column.getKey()!=null) {
                Optional<PropertyDefinition<T, ?>> propertyDefinition = propertySet.getProperty(column.getKey());
                if (propertyDefinition.isPresent()) {
                	value = propertyDefinition.get().getGetter().apply(item);
                } else {
                    throw new IllegalStateException("Column key: " + column.getKey() + " is a property which cannot be found");
                }
        	} else {
        		Renderer<T> r = column.getRenderer();
        		try {
					Field ivps = Renderer.class.getDeclaredField("valueProviders");
					ivps.setAccessible(true);
					Map<String, ValueProvider<T, ?>> vps = (Map<String, ValueProvider<T, ?>>) ivps.get(r);
					
					Field ciid = Column.class.getDeclaredField("columnInternalId");
					ciid.setAccessible(true);
					String cid = (String) ciid.get(column);
					
					value = vps.get(cid).apply(item);
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
        	}
        	Cell currentCell = startingCell;
        	if (startingCell.getColumnIndex()<currentColumn[0]) {
        		currentCell = startingCell.getRow().createCell(currentColumn[0]);
        		currentCell.setCellStyle(startingCell.getCellStyle());
        	}
    		currentColumn[0] = currentColumn[0] + 1;
            buildCell(value, currentCell);

        });
    }
    
    private void buildCell(Object value, Cell cell) {
        if (value == null) {
        	cell.setBlank();
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Calendar) {
            Calendar calendar = (Calendar) value;
            cell.setCellValue(calendar.getTime());
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else {
            cell.setCellValue(value.toString());
        }
	}

	private boolean isExportable(Grid.Column<T> column) {
        return column.isVisible() /*&& column.getKey() != null && !column.getKey().isEmpty()
                && (propertySet == null || propertySet.getProperty(column.getKey()).isPresent())*/;
    }
	
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Stream<T> getDataStream(Query newQuery) {
        Stream<T> stream = grid.getDataProvider().fetch(newQuery);
        if (stream.isParallel()) {
            LoggerFactory.getLogger(DataCommunicator.class)
                    .debug("Data provider {} has returned "
                                    + "parallel stream on 'fetch' call",
                            grid.getDataProvider().getClass());
            stream = stream.collect(Collectors.toList()).stream();
            assert !stream.isParallel();
        }
        return stream;
    }

	private void fillHeaderOrFooter(Sheet sheet, Cell headersCell, List<String> headers) {
		CellStyle style = headersCell.getCellStyle();
		sheet.setActiveCell(headersCell.getAddress());
		headers.forEach(header->{
			Cell cell = sheet.getRow(sheet.getActiveCell().getRow()).getCell(sheet.getActiveCell().getColumn());
			if (cell==null) {
				cell = sheet.getRow(sheet.getActiveCell().getRow()).createCell(sheet.getActiveCell().getColumn());
				cell.setCellStyle(style);
			}
			cell.setCellValue(header);
			sheet.setActiveCell(new CellAddress(sheet.getActiveCell().getRow(),sheet.getActiveCell().getColumn() + 1));
		});
	}

	private List<String> getGridHeaders(Grid<T> grid) {
		return grid.getColumns().stream().map(column->GridUtils.getHeader(column)).collect(Collectors.toList());
	}

	private List<String> getGridFooters(Grid<T> grid) {
		return grid.getColumns().stream().map(column->GridUtils.getFooter(column)).collect(Collectors.toList());
	}

	private Cell findCellWithPlaceHolder(Sheet sheet, String placeholder) {
	    for (Row row : sheet) {
	        for (Cell cell : row) {
	            if (cell.getCellType() == CellType.STRING) {
	                if (cell.getRichStringCellValue().getString().trim().equals(placeholder)) {
	                    return cell;
	                }
	            }
	        }
	    }               
	    return null;
	}

	private Workbook getWorkbook() throws EncryptedDocumentException, IOException {
		InputStream inp = this.getClass().getResourceAsStream("/template.xlsx");
		return WorkbookFactory.create(inp);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean isAutoAttachExportButtons() {
		return autoAttachExportButtons;
	}

	public void setAutoAttachExportButtons(boolean autoAttachExportButtons) {
		this.autoAttachExportButtons = autoAttachExportButtons;
	}

	public Map<String,String> getAdditionalPlaceHolders() {
		return additionalPlaceHolders;
	}

	public void setAdditionalPlaceHolders(Map<String,String> additionalPlaceHolders) {
		this.additionalPlaceHolders = additionalPlaceHolders;
	}

	public int getSheetNumber() {
		return sheetNumber;
	}

	public void setSheetNumber(int sheetNumber) {
		this.sheetNumber = sheetNumber;
	}

	public boolean isAutoMergeTitle() {
		return autoMergeTitle;
	}

	public void setAutoMergeTitle(boolean autoMergeTitle) {
		this.autoMergeTitle = autoMergeTitle;
	}

}
