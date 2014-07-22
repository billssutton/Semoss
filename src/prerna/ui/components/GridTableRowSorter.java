/*******************************************************************************
 * Copyright 2013 SEMOSS.ORG
 * 
 * This file is part of SEMOSS.
 * 
 * SEMOSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SEMOSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SEMOSS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package prerna.ui.components;

import java.util.Comparator;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.itextpdf.text.pdf.PdfStructTreeController.returnType;


/**
 * This class is used to create a table model for grids.
 */
public class GridTableRowSorter <M extends TableModel> extends TableRowSorter<TableModel> {
	MyComparator comparator = new MyComparator();
	
	public GridTableRowSorter(TableModel tm){
		super(tm);
	}
	
    @Override
    public boolean useToString(int column){
    	return false;
    }
	
    @Override
    public Comparator getComparator(int column){
    	return comparator;
    }
    
    private class MyComparator implements Comparator<Object>{

		@Override
		public int compare(Object o1, Object o2) {
			if(o1 == null){
				if(o2 == null){
					return 0;
				}
				else{
					return 1;
				}
			}
			else if(o2 == null){
				return -1;
			}
			else{
				if(o1 instanceof Integer) {
					o1 = new Double((Integer)o1);
				}
				if(o2 instanceof Integer) {
					o2 = new Double((Integer)o2);
				}
				
				if(o1 instanceof String){
					if(o2 instanceof String){
						return ((String)o1).compareTo((String) o2);
					}
					else if(o2 instanceof Double){
						return -1; //string comes before double
					}
				}
				else if(o2 instanceof String){
					if(o1 instanceof Double){
						return 1;//string comes before double
					}
				}
				else if(o1 instanceof Double){
					if(o2 instanceof Double){
						if(((Double)o1) > ((Double)o2)) {
							return 1;
						}
						else if(((Double)o1) < ((Double)o2)) {
							return -1;
						}
						else if(Double.doubleToLongBits(((Double)o1)) ==
				                  Double.doubleToLongBits(((Double)o2))) {
							return 0;
						}
						else if(Double.isNaN((Double)o1)){
							if(Double.isNaN((Double)o2)){
								return 0;
							}
							else{
								return 1;
							}
						}
						else if(Double.isNaN((Double)o2)){
							return -1;
						}
					}
				}
			}

			System.out.println("UH OH " + o1.getClass() + "     " +o1 + "     " + o2.getClass()+"     " +o2);
			return (o1.toString()).compareTo(o2.toString());
		}
    }
}
