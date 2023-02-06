/*-
 * #%L
 * Grid Exporter Add-on
 * %%
 * Copyright (C) 2022 - 2023 Flowing Code
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;

/**
 * Class for adding support for older versions of Apache POI by using reflection
 * @author mlopez
 *
 */
public class PoiHelper {

  private static final String XWPF_TABLE_CELL_CLASS_NAME = "XWPFTableCell";
  private static final String CT_TBL_GRID_COL_CLASS_NAME = "CTTblGridCol";
  private static final String CT_TBL_WIDTH_CLASS_NAME = "CTTblWidth";
  private static final String CELL_CLASS_NAME = "Cell";
  private static final String SET_WIDTH_METHOD_NAME = "setWidth";
  private static final String SET_W_METHOD_NAME = "setW";
  private static final String GET_CELL_TYPE_METHOD_NAME = "getCellType";
  private static final String SET_BLANK_METHOD_NAME = "setBlank";
  private static final String PROBLEM_WHEN_CALLING_METHOD_ON_EXCEPTION_MESSAGE = "Problem when calling method %s() on class %s";

  /**
   * Tries to set the cell to be blank
   * @param cell
   */
  @SuppressWarnings("deprecation")
  public static void setBlank(Cell cell) {
    try {
      Method setBlank = Cell.class.getMethod(SET_BLANK_METHOD_NAME);
      setBlank.invoke(cell);
    } catch (NoSuchMethodException e) {
      cell.setCellType(CellType.BLANK);
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException(String.format(PROBLEM_WHEN_CALLING_METHOD_ON_EXCEPTION_MESSAGE, SET_BLANK_METHOD_NAME, CELL_CLASS_NAME),e);
    }
  }

  /**
   * Verifies if the cell if of the supplied type
   * @param cell
   * @param cellType
   * @return
   */
  @SuppressWarnings("deprecation")
  public static boolean cellTypeEquals(Cell cell, CellType cellType) {
    boolean result = false;
    try {
      Method getCellType = Cell.class.getMethod(GET_CELL_TYPE_METHOD_NAME);
      if (getCellType.getReturnType().isPrimitive()) {
        result = getCellType.invoke(cell).equals(cellType.getCode());
      } else {
        result = getCellType.invoke(cell).equals(cellType);
      }
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException(String.format(PROBLEM_WHEN_CALLING_METHOD_ON_EXCEPTION_MESSAGE, GET_CELL_TYPE_METHOD_NAME, CELL_CLASS_NAME),e);
    }

    return result;
  }

  /**
   * Sets the width of the given CTTblWidth
   * @param tblW
   * @param string
   */
  public static void setWonCTTblWidth(CTTblWidth tblW, String string) {
    try {
      Method setW = CTTblWidth.class.getMethod(SET_W_METHOD_NAME, Object.class);
      setW.invoke(tblW, string);      
    } catch (NoSuchMethodException e) {
      try {
        Method setWBigInteger = CTTblWidth.class.getMethod(SET_W_METHOD_NAME, BigInteger.class);
        setWBigInteger.invoke(tblW, new BigInteger(string));
      } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
        throw new IllegalStateException(String.format(PROBLEM_WHEN_CALLING_METHOD_ON_EXCEPTION_MESSAGE, SET_W_METHOD_NAME, CT_TBL_WIDTH_CLASS_NAME),e1);
      }
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException(String.format(PROBLEM_WHEN_CALLING_METHOD_ON_EXCEPTION_MESSAGE, SET_W_METHOD_NAME, CT_TBL_WIDTH_CLASS_NAME),e);
    }
  }

  /**
   * Sets the width of the given CTTblGridCol
   * @param cctblgridcol
   * @param string
   */
  public static void setWonCTTblGridCol(CTTblGridCol cctblgridcol, String string) {
    try {
      Method setW = CTTblGridCol.class.getMethod(SET_W_METHOD_NAME, Object.class);
      setW.invoke(cctblgridcol, string);      
    } catch (NoSuchMethodException e) {
      try {
        Method setWBigInteger = CTTblGridCol.class.getMethod(SET_W_METHOD_NAME, BigInteger.class);
        setWBigInteger.invoke(cctblgridcol, new BigInteger(string));
      } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
        throw new IllegalStateException(String.format(PROBLEM_WHEN_CALLING_METHOD_ON_EXCEPTION_MESSAGE, SET_W_METHOD_NAME, CT_TBL_GRID_COL_CLASS_NAME),e1);
      }
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException(String.format(PROBLEM_WHEN_CALLING_METHOD_ON_EXCEPTION_MESSAGE, SET_W_METHOD_NAME, CT_TBL_GRID_COL_CLASS_NAME),e);
    }
  }

  /**
   * Sets the width of the given XWPFTableCell
   * @param currentCell
   * @param string
   */
  public static void setWidth(XWPFTableCell currentCell, String string) {
    try {
      Method setW = XWPFTableCell.class.getMethod(SET_WIDTH_METHOD_NAME, String.class);
      setW.invoke(currentCell, string);      
    } catch (NoSuchMethodException e) {
      // ignoring for now
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException(String.format(PROBLEM_WHEN_CALLING_METHOD_ON_EXCEPTION_MESSAGE, SET_WIDTH_METHOD_NAME, XWPF_TABLE_CELL_CLASS_NAME),e);
    }
  }

}
