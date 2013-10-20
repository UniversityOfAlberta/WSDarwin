package wsdarwin.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.*;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import wsdarwin.clientadaptation.ClientAdaptationRefactoring;
import wsdarwin.comparison.delta.ChangeDelta;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.model.Operation;
import wsdarwin.wizards.MyRefactoringWizard;
import wsdarwin.wizards.NewClientAdapterWizard;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class WSDarwinView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "wsdarwin.views.WSDarwinView";

	private TreeViewer viewer;
	private Action compareInterfaces;
	private Action adaptClient;
	private Action doubleClickAction;
	private Delta[] diffTable;
	private String oldWSDL;
	private String newWSDL;
	private ICompilationUnit oldStub;
	private ICompilationUnit newStub;

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 * 
	 * 
	 * public class TreeObject implements IAdaptable { private String name;
	 * private TreeParent parent;
	 * 
	 * public TreeObject(String name) { this.name = name; } public String
	 * getName() { return name; } public void setParent(TreeParent parent) {
	 * this.parent = parent; } public TreeParent getParent() { return parent; }
	 * public String toString() { return getName(); } public Object
	 * getAdapter(Class key) { return null; } }
	 * 
	 * public class TreeParent extends TreeObject { private ArrayList children;
	 * public TreeParent(String name) { super(name); children = new ArrayList();
	 * } public void addChild(TreeObject child) { children.add(child);
	 * child.setParent(this); } public void removeChild(TreeObject child) {
	 * children.remove(child); child.setParent(null); } public TreeObject []
	 * getChildren() { return (TreeObject [])children.toArray(new
	 * TreeObject[children.size()]); } public boolean hasChildren() { return
	 * children.size()>0; } }
	 */

	class ViewContentProvider implements IStructuredContentProvider,
			ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			if (diffTable != null) {
				return diffTable;
			} else {
				return new Delta[] {};
			}
		}

		public Object getParent(Object child) {
			return ((Delta) child).getParent();
		}

		public Object[] getChildren(Object parent) {
			return ((Delta) parent).getDeltas().toArray(
					new Delta[((Delta) parent).getDeltas().size()]);
		}

		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}
		/*
		 * We will set up a dummy model to initialize tree heararchy. In a real
		 * code, you will connect to a real model and expose its hierarchy.
		 * 
		 * private void initialize() { TreeObject to1 = new
		 * TreeObject("Leaf 1"); TreeObject to2 = new TreeObject("Leaf 2");
		 * TreeObject to3 = new TreeObject("Leaf 3"); TreeParent p1 = new
		 * TreeParent("Parent 1"); p1.addChild(to1); p1.addChild(to2);
		 * p1.addChild(to3);
		 * 
		 * TreeObject to4 = new TreeObject("Leaf 4"); TreeParent p2 = new
		 * TreeParent("Parent 2"); p2.addChild(to4);
		 * 
		 * TreeParent root = new TreeParent("Root"); root.addChild(p1);
		 * root.addChild(p2);
		 * 
		 * invisibleRoot = new TreeParent(""); invisibleRoot.addChild(root); }
		 */
	}

	class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		@Override
		public Image getColumnImage(Object o, int i) {

			return null;
		}

		@Override
		public String getColumnText(Object o, int i) {
			return "";
		}
	}

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public WSDarwinView() {
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		viewer.getTree().setLayout(layout);
		viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("File");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Operation");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Complex Type");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Change Source");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Change Target");
		viewer.expandAll();

		for (int i = 0, n = viewer.getTree().getColumnCount(); i < n; i++) {
			viewer.getTree().getColumn(i).pack();
		}
		viewer.setCellEditors(new CellEditor[] { new TextCellEditor(),
				new TextCellEditor(), new TextCellEditor(),
				new TextCellEditor(), new TextCellEditor() });
		viewer.getTree().setLinesVisible(true);
		viewer.getTree().setHeaderVisible(true);
		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				WSDarwinView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(compareInterfaces);
		manager.add(adaptClient);
		manager.add(new Separator());
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(compareInterfaces);
		manager.add(adaptClient);
		manager.add(new Separator());
	}

	private void makeActions() {
		compareInterfaces = new Action() {
			public void run() {
				IWizardDescriptor descriptor = PlatformUI.getWorkbench()
						.getNewWizardRegistry()
						.findWizard("wsca2.wizards.NewClientAdapterWizard");
				try {
					IWizard wizard = descriptor.createWizard();
					NewClientAdapterWizard ncaw = null;
					if (wizard instanceof NewClientAdapterWizard) {
						ncaw = (NewClientAdapterWizard) wizard;
						ncaw.setAction(this);
						if (oldWSDL != null) {
							ncaw.setOldWSDL(oldWSDL);
						}
						if (newWSDL != null) {
							ncaw.setNewWSDL(newWSDL);
						}
						if (oldStub != null) {
							ncaw.setOldStub(oldStub);
						}
						if (newStub != null) {
							ncaw.setNewStub(newStub);
						}
					}
					WizardDialog wd = new WizardDialog(PlatformUI
							.getWorkbench().getActiveWorkbenchWindow()
							.getShell(), wizard);
					wd.setTitle(wizard.getWindowTitle());
					wd.open();
					diffTable = new Delta[] { ncaw.getDiff() };
					oldWSDL = ncaw.getOldWSDL();
					newWSDL = ncaw.getNewWSDL();
					oldStub = ncaw.getOldStub();
					newStub = ncaw.getNewStub();
					// viewer.refresh();
					// viewer.setContentProvider(new ViewContentProvider());
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		};
		compareInterfaces.setText("diff");
		compareInterfaces.setToolTipText("WSDL Diff");
		compareInterfaces.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_TASK_TSK));

		adaptClient = new Action() {
			public void run() {
				diffTable[0].printDelta(0);
				CompilationUnit oldCompilationUnit = parseAST(oldStub);
				Map<String, Delta> changedOperationNames = getChangedOperations();
				Map<MethodDeclaration, Delta> changedMethods = getChangedMethods(
						changedOperationNames, oldCompilationUnit);
				CompilationUnit newCompilationUnit = parseAST(newStub);

				ClientAdaptationRefactoring refactoring = new ClientAdaptationRefactoring(
						oldCompilationUnit, newCompilationUnit,
						(TypeDeclaration)oldCompilationUnit.types().get(0),
						(TypeDeclaration)newCompilationUnit.types().get(0),
						changedMethods);
				MyRefactoringWizard wizard = new MyRefactoringWizard(
						refactoring);
				RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(
						wizard);
				try {
					String titleForFailedChecks = ""; //$NON-NLS-1$ 
					op.run(getSite().getShell(), titleForFailedChecks);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		adaptClient.setText("Adapt Client");
		adaptClient.setToolTipText("Adapt Client");
		adaptClient.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection)
						.getFirstElement();
				showMessage("Double-click detected on " + obj.toString());
			}
		};
	}

	private Map<MethodDeclaration, Delta> getChangedMethods(
			Map<String, Delta> changedOperationNames,
			CompilationUnit compilationUnit) {
		Map<MethodDeclaration, Delta> changedMethods = new HashMap<MethodDeclaration, Delta>();
		List<TypeDeclaration> types = compilationUnit.types();
		TypeDeclaration typeDeclaration = types.get(0);
		MethodDeclaration[] methods = typeDeclaration.getMethods();
		for (int i = 0; i < methods.length; i++) {
			String name = containsKeyIgnoreCase(changedOperationNames, methods[i].getName()
					.getIdentifier());
			if (name != null) {
				changedMethods.put(methods[i], changedOperationNames
						.get(name));
			}
		}
		System.out.println();
		return changedMethods;
	}

	private String containsKeyIgnoreCase(
			Map<String, Delta> changedOperationNames, String identifier) {
		for(String key : changedOperationNames.keySet()) {
			if(key.equalsIgnoreCase(identifier)) {
				return key;
			}
		}
		return null;
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(),
				"WSDL Diff", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}


	private CompilationUnit parseAST(ICompilationUnit iCompilationUnit) {
		// ASTInformationGenerator.setCurrentITypeRoot(iCompilationUnit);
		IFile iFile = (IFile) iCompilationUnit.getResource();
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(iCompilationUnit);
		parser.setResolveBindings(true); // we need bindings later on
		return (CompilationUnit) parser.createAST(null);
	}

	private Map<String, Delta> getChangedOperations() {
		Map<String, Delta> changedOperationNames = new HashMap<String, Delta>();
		for (Delta delta : diffTable[0].getDeltas()) {
			if (delta instanceof ChangeDelta
					&& delta.getSource() instanceof Operation) {
				changedOperationNames.put(
						((Operation) delta.getSource()).getName(), delta);
			}
		}
		return changedOperationNames;
	}
}