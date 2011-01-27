package edu.uci.lighthouse.ui.views;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.GridLayoutAlgorithm;

import edu.uci.lighthouse.model.LighthouseModel;
import edu.uci.lighthouse.ui.views.actions.DiagramModeDropDownAction;
import edu.uci.lighthouse.ui.views.actions.FilterActiveClassAction;
import edu.uci.lighthouse.ui.views.actions.FilterAuthorAction;
import edu.uci.lighthouse.ui.views.actions.FilterModifiedAction;
import edu.uci.lighthouse.ui.views.actions.FilterPackageAction;
import edu.uci.lighthouse.ui.views.actions.HighlightDropDownAction;
import edu.uci.lighthouse.ui.views.actions.LayoutDropDownAction;
import edu.uci.lighthouse.ui.views.actions.LinkWithEditorAction;
import edu.uci.lighthouse.ui.views.actions.OpenInEditorAction;
import edu.uci.lighthouse.ui.views.actions.SoftLockAction;
import edu.uci.lighthouse.ui.views.actions.ZoomDropDownAction;

public class EmergingDesignView extends ThumbnailView implements IZoomableWorkbenchPart{
	
	private EditorListener editorListener = new EditorListener();
	private FilterManager filterManager;
	private static GraphViewer viewer = null;
	private static Logger logger = Logger.getLogger(EmergingDesignView.class);
	
	private Action softLockAction;
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		viewer = new GraphViewer(parent, SWT.NONE);
		viewer.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
		//Create the FilterManager instance
		filterManager = new FilterManager(viewer);
		
//		viewer.setContentProvider(new LighthouseRelationshipContentProvider());
		viewer.setContentProvider(new LighthouseEntityContentProvider());
		viewer.setLabelProvider(new LighthouseLabelProvider());
		viewer.setLayoutAlgorithm(new GridLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING));		
		viewer.setInput(LighthouseModel.getInstance());
		
		createActions();
		hookContextMenu();
		
		// Set the parameters for the thumbnail view
		setViewport(viewer.getGraphControl().getViewport());
		setSource(viewer.getGraphControl().getContents());
		setMainComposite(viewer.getGraphControl());
		
		getSite().getPage().addPartListener(editorListener);
	}
	
	private void createActions() {
		//FIXME: Erase EditorListener and put everything in LinkWithEditorAction
		LinkWithEditorAction linkAction = new LinkWithEditorAction(viewer);
		FilterActiveClassAction activeClassAction = new FilterActiveClassAction(viewer);
//		FilterOpenEditorAction openEditorAction = new FilterOpenEditorAction(viewer);
		editorListener.addEditorSelectionListener(activeClassAction);
//		editorListener.addEditorSelectionListener(openEditorAction);
		editorListener.addEditorSelectionListener(linkAction);
		
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		
		//toolbarManager.add(linkAction);
		toolbarManager.add(new HighlightDropDownAction(viewer.getGraphControl()));
		toolbarManager.add(new LayoutDropDownAction(viewer.getGraphControl()));
		toolbarManager.add(new DiagramModeDropDownAction(viewer.getGraphControl()));
		toolbarManager.add(new FilterAuthorAction(viewer.getGraphControl()));
		toolbarManager.add(new FilterPackageAction(viewer));
		toolbarManager.add(new FilterModifiedAction(viewer.getGraphControl()));
		toolbarManager.add(activeClassAction);
		//toolbarManager.add(openEditorAction);
		toolbarManager.add(new ZoomDropDownAction(this));
		//toolbarManager.add(new SoftLockAction2(viewer.getGraphControl()));
		
		//TODO (danielle): Create the action here
		softLockAction = new SoftLockAction(viewer.getGraphControl());
		
		//FIXME: Change this to a decorator
		new OpenInEditorAction(viewer.getGraphControl());

	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);		
	}
	
	private void fillContextMenu(IMenuManager manager) {
		manager.add(softLockAction);
	}

	@Override
	public AbstractZoomableViewer getZoomableViewer() {
		return viewer;
	}
	
	@Override
	public void dispose() {
		logger.info("dispose()");
		getSite().getPage().removePartListener(editorListener);
		super.dispose();
	}
}
