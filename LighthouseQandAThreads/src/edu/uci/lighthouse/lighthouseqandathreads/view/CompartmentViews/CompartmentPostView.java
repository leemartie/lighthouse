package edu.uci.lighthouse.lighthouseqandathreads.view.CompartmentViews;

import java.util.ArrayList;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.Panel;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import edu.uci.lighthouse.ui.utils.GraphUtils;

public class CompartmentPostView extends Panel {

	private int displayLength = 100;
	private int NUM_COLUMNS = 2;
	private Label messageLabel;
	private String prefix = "? ";
	private ArrayList<Shell> openShells = new ArrayList<Shell>();

	public CompartmentPostView() {

		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.numColumns = NUM_COLUMNS;
		layout.marginHeight = 0;
		layout.marginWidth = 0;

		setLayoutManager(layout);
	}

	public CompartmentPostView(String message) {

		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.numColumns = NUM_COLUMNS;
		layout.marginHeight = 0;
		layout.marginWidth = 0;

		setLayoutManager(layout);

		messageLabel = new Label(message.length() >= displayLength ? prefix
				+ message.substring(0, displayLength) : prefix + message);
		this.add(messageLabel);

		PostMouseMotionListener pl = new PostMouseMotionListener();

		this.addMouseMotionListener(pl);
		messageLabel.addMouseMotionListener(pl);
	}

	private class PostMouseMotionListener extends MouseMotionListener.Stub {
		public void mouseHover(MouseEvent me) {
			System.out.println("HOVER");
			Shell treadShell = new Shell(GraphUtils.getGraphViewer()
					.getGraphControl().getDisplay().getActiveShell(),
					SWT.NO_TRIM);
			treadShell.setSize(100, 200);
			Point location = me.getLocation();
			org.eclipse.swt.graphics.Point point = GraphUtils.getGraphViewer()
					.getGraphControl().toDisplay(location.x, location.y);

			treadShell.setLocation(point);

			treadShell.open();

			openShells.add(treadShell);
		}

		public void mouseExited(MouseEvent me) {
			System.out.println("EXITED");
			CompartmentPostView.this.setBackgroundColor(null);
			//this will get called when your mouse enters the tread shell!!

		}

		public void mouseEntered(MouseEvent me) {
			System.out.println("ENTERED");
			CompartmentPostView.this.setBackgroundColor(ColorConstants.yellow);


		}
	}
}
