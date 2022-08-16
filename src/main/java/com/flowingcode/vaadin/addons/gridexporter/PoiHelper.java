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

  /**
   * Tries to set the cell to be blank
   * @param cell
   */
  @SuppressWarnings("deprecation")
  public static void setBlank(Cell cell) {
    try {
      Method setBlank = Cell.class.getMethod("setBlank", Void.class);
      setBlank.invoke(cell);
    } catch (NoSuchMethodException e) {
      cell.setCellType(CellType.BLANK);
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException("Problem when calling setBlank() method",e);
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
      Method getCellType = Cell.class.getMethod("getCellType");
      if (getCellType.getReturnType().isPrimitive()) {
        result = getCellType.invoke(cell).equals(cellType.getCode());
      } else {
        result = getCellType.invoke(cell).equals(cellType);
      }
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException("Problem when calling setBlank() method",e);
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
      Method setW = CTTblWidth.class.getMethod("setW", Object.class);
      setW.invoke(tblW, string);      
    } catch (NoSuchMethodException e) {
      try {
        Method setWBigInteger = CTTblWidth.class.getMethod("setW", BigInteger.class);
        setWBigInteger.invoke(tblW, new BigInteger(string));
      } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
        throw new IllegalStateException("Problem when calling setW() method on CTTblWidth",e1);
      }
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException("Problem when calling setW() method on CTTblWidth",e);
    }
  }

  /**
   * Sets the width of the given CTTblGridCol
   * @param cctblgridcol
   * @param string
   */
  public static void setWonCTTblGridCol(CTTblGridCol cctblgridcol, String string) {
    try {
      Method setW = CTTblGridCol.class.getMethod("setW", Object.class);
      setW.invoke(cctblgridcol, string);      
    } catch (NoSuchMethodException e) {
      try {
        Method setWBigInteger = CTTblGridCol.class.getMethod("setW", BigInteger.class);
        setWBigInteger.invoke(cctblgridcol, new BigInteger(string));
      } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
        throw new IllegalStateException("Problem when calling setW() method on CTTblGridCol",e1);
      }
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException("Problem when calling setW() method on CTTblGridCol",e);
    }
  }

  /**
   * Sets the width of the given XWPFTableCell
   * @param currentCell
   * @param string
   */
  public static void setWidth(XWPFTableCell currentCell, String string) {
    try {
      Method setW = XWPFTableCell.class.getMethod("setWidth", String.class);
      setW.invoke(currentCell, string);      
    } catch (NoSuchMethodException e) {
      // por ahora no hacer nada y vemos
    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException("Problem when calling setW() method on CTTblGridCol",e);
    }
  }

}
