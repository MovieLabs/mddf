/**
 * Created Jan 27, 2016 
 * Copyright Motion Picture Laboratories, Inc. 2016
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.movielabs.mddf.tools.util.logging;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import com.movielabs.mddf.tools.util.FileChooserDialog;
import com.movielabs.mddflib.logging.LogEntryComparator;
import com.movielabs.mddflib.logging.LogEntryNode;
import com.movielabs.mddflib.logging.LogEntryNode.Field;
import com.movielabs.mddflib.logging.LogMgmt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.awt.BorderLayout;

/**
 * @author L. Levin, Critical Architectures LLC
 *
 */
public class LogPanel extends JPanel {

	/**
	 * Extends <tt>TableRowSorter</tt> to provide the ability to sort table rows
	 * in which all columns contain <tt>LogEntryNode</tt> instances. This
	 * requires the usage of custom comparators (i.e.,
	 * <tt>LogEntryComparator</tt> instances).
	 * 
	 * @author L. Levin, Critical Architectures LLC
	 *
	 */
	public class LogSorter<T> extends TableRowSorter<DefaultTableModel> {

		/**
		 * @param model
		 */
		public LogSorter(DefaultTableModel model) {
			super(model);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.table.TableRowSorter#getComparator(int)
		 */
		@Override
		public Comparator<?> getComparator(int column) {
			if ((column >= 0) && (column < colList.size())) {
				return new LogEntryComparator(colList.get(column));
			}
			return super.getComparator(column);
		}

		/**
		 * Override parent implementation to return <tt>false</tt> in all cases.
		 * This is necessary to ensure that the column-specific custom
		 * comparators are correctly applied.
		 * <p>
		 * Note that <tt>TableRowSorter#useToString(int)</tt> has what appears
		 * to be a bug in that it explicitly invokes
		 * {@link DefaultRowSorter#getComparator(int)}. This will prevent
		 * {@link LogSorter#getComparator(int)} from correctly being invoked.
		 * That, in turn, results in a <tt>useToString()</tt> always returning
		 * <tt>true</tt>. Overriding <tt>useToString()</tt> in
		 * <tt>LogSorter</tt> bypasses this bug.
		 * </p>
		 * 
		 * @see javax.swing.table.TableRowSorter#useToString(int)
		 */
		protected boolean useToString(int column) {
			return false;
		}
	}

	private static List<Field> defaultColList = new ArrayList<Field>();
	private static float[] defaultColWidthBASE = { 0.06f, 0.07f, 0.07f, 0.50f, 0.07f, 0.19f, 0.12f };

	private JTable logTable;
	private DefaultTableModel model;
	private JScrollPane scPane;

	private String columnNames[] = null;
	private float[] colWidthSaved = null;
	private boolean firstResize = true;
	private TableRowSorter<DefaultTableModel> sorter;
	private List<Field> colList;

	static {
		// Default col list
		defaultColList.add(Field.Num);
		defaultColList.add(Field.Level);
		defaultColList.add(Field.Tag);
		defaultColList.add(Field.Details);
		defaultColList.add(Field.Line);
		defaultColList.add(Field.File);
		defaultColList.add(Field.Reference);
	}

	/**
	 * Create the panel using the default column list.
	 * 
	 * @param advLogPanel
	 */
	public LogPanel() {
		colList = defaultColList;
		colWidthSaved = defaultColWidthBASE;
		initialize();
	}

	public LogPanel(List<Field> colList, float[] colWidths) {
		this.colList = colList;
		colWidthSaved = colWidths;
		initialize();
	}

	protected void initialize() {
		setBackground(UIManager.getColor("OptionPane.warningDialog.titlePane.background"));
		final Object testData[][] = {};

		columnNames = new String[colList.size()];
		for (int i = 0; i < colList.size(); i++) {
			columnNames[i] = colList.get(i).toString();
		}

		model = new DefaultTableModel(testData, columnNames);
		logTable = new JTable(model);
		logTable.setRowSelectionAllowed(false);
		logTable.setShowHorizontalLines(false);
		logTable.setBorder(new LineBorder(new Color(0, 0, 0)));
		logTable.setDefaultRenderer(Object.class, new LogTableRenderer());

		sorter = new LogSorter<DefaultTableModel>(model);
		setLayout(new BorderLayout(0, 0));
		logTable.setRowSorter(sorter);

		scPane = new JScrollPane(logTable);
		scPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		add(scPane, BorderLayout.CENTER);

		/*
		 * Following is broken code (hence commented out for now). Intent is to
		 * detect user resize of column widths and save for next time setSize()
		 * is invoked.
		 */
		TableColumnModelListener tableColumnModelListener = new TableColumnModelListener() {
			public void columnAdded(TableColumnModelEvent e) {
			}

			public void columnMarginChanged(ChangeEvent e) {
				if (colWidthSaved[0] >= 1.0f) {
					/*
					 * NONE OF THESE WORK!!
					 */
					// System.out.println("Margin: colWidthSaved[2]=" +
					// colWidthSaved[2]);
					// saveColSize();
					// Dimension newPS = scPane.getPreferredSize();
					// int curW = (int) newPS.getWidth();
					// setSize(curW, (int) (newPS.getHeight()+2));
					// advLogPanel.setSize();
				}
			}

			public void columnMoved(TableColumnModelEvent e) {
			}

			public void columnRemoved(TableColumnModelEvent e) {
			}

			public void columnSelectionChanged(ListSelectionEvent e) {
			}
		};
		TableColumnModel columnModel = logTable.getColumnModel();
		columnModel.addColumnModelListener(tableColumnModelListener);

	}

	public void addMouseListener(MouseListener listener) {
		super.addMouseListener(listener);
		logTable.addMouseListener(listener);
	}

	/**
	 * Respond to a <tt>setSize()</tt> request by re-adjusting the column
	 * widths.
	 * 
	 * @see com.movielabs.mddf.util.UiLogger#setWidth(int)
	 */
	public void setSize(int width, int ht) {
		if (width < 1) {
			width = 1000;
		}
		Dimension newPS = new Dimension(width, ht);
		scPane.setPreferredSize(newPS);
		// now apportion the col widths..
		double total = 0;
		float[] colWidthPercentage = new float[colWidthSaved.length];
		for (int i = 0; i < logTable.getColumnModel().getColumnCount(); i++) {
			if (!firstResize) {
				TableColumn column = logTable.getColumnModel().getColumn(i);
				colWidthPercentage[i] = column.getPreferredWidth();
			} else {
				colWidthPercentage[i] = colWidthSaved[i];
			}
			total += colWidthPercentage[i];
		}
		colWidthSaved = colWidthPercentage;
		firstResize = false;
		int totalW = 0;
		for (int i = 0; i < logTable.getColumnModel().getColumnCount(); i++) {
			TableColumn column = logTable.getColumnModel().getColumn(i);
			int cW = (int) (width * (colWidthPercentage[i] / total));
			column.setPreferredWidth(cW);
			totalW = totalW + cW;
		}
		this.revalidate();
		this.repaint();
	}

	/**
	 * @param entry
	 */
	private void append(LogEntryNode entry) {
		/*
		 * The design is to have the LogTableRenderer figure out what attribute
		 * of the entry to display based on the column number passed to it. That
		 * means regardless of which column, the same LogEntryNode instance is
		 * going to be used as the column data for a given row.
		 */
		Object[] rowData = new LogEntryNode[columnNames.length];
		for (int i = 0; i < rowData.length; i++) {
			rowData[i] = entry;
		}
		model.addRow(rowData);
	}

	/**
	 * @param msgList
	 */
	public void append(List<LogEntryNode> msgList) {
		int firstRow = model.getRowCount() - 1;
		for (int i = 0; i < msgList.size(); i++) {
			LogEntryNode entry = msgList.get(i);
			append(entry);
		}
		firstResize = true;
		int lastRow = model.getRowCount() - 1;
		TableModelEvent evt = new TableModelEvent(model, firstRow, lastRow, TableModelEvent.ALL_COLUMNS,
				TableModelEvent.INSERT);
		model.fireTableChanged(evt);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.movielabs.mddf.util.Logger#clearLog()
	 */
	public void clearLog() {
		model.setRowCount(0);
	}

	/**
	 * Prompts user to select where to save the log file contents, then writes
	 * the log messages in the desired format.
	 * 
	 * @param format
	 * @throws IOException
	 */
	public void saveAs(String format) throws IOException {
		FileFilter filter = new FileNameExtensionFilter(format.toUpperCase() + " File", format);
		File outFile = FileChooserDialog.getFilePath("Save to file", null, filter, "logFile", logTable);
		saveAs(outFile, format);
	}

	/**
	 * Save currently displayed log messages in the desired location and format.
	 * 
	 * @param outFile
	 * @param format
	 * @throws IOException
	 */
	public void saveAs(File outFile, String format) throws IOException {
		if (outFile == null) {
			return;
		}
		String suffix = "." + format;
		if (!outFile.getName().endsWith(suffix)) {
			String fPath = outFile.getAbsolutePath() + suffix;
			outFile = new File(fPath);
		}
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
		/* first row has column names */
		int cCnt = LogEntryNode.DEFAULT_COL_NAMES.length;
		String colSep = LogEntryNode.colSep;
		String headerRow = LogEntryNode.DEFAULT_COL_NAMES[0];
		for (int i = 1; i < cCnt; i++) {
			if (!LogEntryNode.DEFAULT_COL_NAMES[i].equals("Module")) {
				headerRow = headerRow + colSep + LogEntryNode.DEFAULT_COL_NAMES[i];
			}
		}
		/*
		 * 'Notes' is special case for the tooltip (a.k.a 'added detail' or
		 * 'drill-down')
		 */
		headerRow = headerRow + colSep + "Notes";
		headerRow = headerRow + colSep + "File Path";
		writer.write(headerRow + "\n");
		/* add data rows */
		Vector dVecTable = model.getDataVector();
		for (int i = 0; i < dVecTable.size(); i++) {
			Vector rowVec = (Vector) dVecTable.get(i);
			LogEntryNode entry = (LogEntryNode) rowVec.get(0);
			String trimedRow = entry.toCSV();
			writer.write(trimedRow + "\n");
		}
		writer.flush();
		writer.close();

	}

	public class LogTableRenderer implements TableCellRenderer {
		private final Color EVEN_ROW_COLOR = Color.lightGray;
		private final Color ODD_ROW_COLOR = Color.white;
		public final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * javax.swing.table.TableCellRenderer#getTableCellRendererComponent
		 * (javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = DEFAULT_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);

			// Apply zebra style on table rows
			if (row % 2 == 0) {
				c.setBackground(EVEN_ROW_COLOR);
			} else {
				c.setBackground(ODD_ROW_COLOR);
			}
			LogEntryNode entry = (LogEntryNode) value;
			String celltext = null;
			String tTipText = entry.getTooltip();
			if (tTipText == null) {
				tTipText = "";
			}
			String specRef = entry.getReference();
			if (!specRef.isEmpty()) {
				tTipText = tTipText + " [" + specRef + "]";
			}
			// ..............................................................................
			switch (columnNames[column]) {
			case "Num":
				celltext = Integer.toString(entry.getEntryNumber());
				break;
			case "Level":
				celltext = LogMgmt.logLevels[entry.getLevel()];
				break;
			case "Type":
				celltext = entry.getTagAsText();
				break;
			case "Details":
				celltext = entry.getSummary();
				tTipText = celltext + "; " + tTipText;
				break;
			case "Line":
				if (entry.getLine() <= 0) {
					celltext = "";
					tTipText = entry.getSrcFileName();
				} else {
					celltext = Integer.toString(entry.getLine());
				}
				break;
			case "File":
				celltext = entry.getSrcFileNode().getLabel();
				File targetFile = entry.getFile();
				if (targetFile != null) {
					tTipText = entry.getFile().getAbsolutePath();
				}
				break;
			case "Reference":
				celltext = entry.getReference();
				break;
			case "Module":
				celltext = entry.getModuleID();
				break;
			case "Tag":
				celltext = entry.getTagAsText();
				break;
			default:
				break;
			}
			((JLabel) c).setText(celltext);
			if ((tTipText != null) && (!tTipText.isEmpty())) {
				((JComponent) c).setToolTipText(tTipText);
			} else {

				((JComponent) c).setToolTipText(null);
			}
			return c;
		}
	}

	/**
	 * @param e
	 * @return
	 */
	public LogEntryNode getLogEntryAt(MouseEvent evt) {
		int rowNum = logTable.rowAtPoint(evt.getPoint());
		int entryIndex = logTable.convertRowIndexToModel(rowNum);
		/*
		 * Can use any column to retrieve entry since append() uses the LogEntry
		 * as the data for every column in the row.
		 */
		LogEntryNode entry = (LogEntryNode) model.getValueAt(entryIndex, 1);
		return entry;
	}

}
