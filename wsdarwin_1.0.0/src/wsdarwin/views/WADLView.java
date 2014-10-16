package wsdarwin.views;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wadlto.WADLToJava;
import org.apache.cxf.tools.wadlto.WadlToolConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.*;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.runner.Result;

import wsdarwin.clientadaptation.ClientAdaptationRefactoring;
import wsdarwin.clientadaptation.RESTClientAdaptationRefactoring;
import wsdarwin.comparison.delta.AddDelta;
import wsdarwin.comparison.delta.ChangeDelta;
import wsdarwin.comparison.delta.DeleteDelta;
import wsdarwin.comparison.delta.Delta;
import wsdarwin.comparison.delta.MoveDelta;
import wsdarwin.model.Operation;
import wsdarwin.parsers.WADLParser;
import wsdarwin.parsers.WSDLParser;
import wsdarwin.util.DeltaUtil;
import wsdarwin.util.XMLGenerator;
import wsdarwin.wadlgenerator.RequestAnalyzer;
import wsdarwin.wadlgenerator.Response2XSD;
import wsdarwin.wadlgenerator.model.WADLFile;
import wsdarwin.wadlgenerator.model.xsd.XSDFile;
import wsdarwin.wizards.DiffInputPage;
import wsdarwin.wizards.MyRefactoringWizard;
import wsdarwin.wizards.NewClientAdapterWizard;
import wsdarwin.wizards.WADLGenerationInputPage;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class WADLView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "wsdarwin.views.WSDLView";

	private TreeViewer viewer;
	private Action generateWADL;
	private Action generateClientProxy;
	private Action compareInterfaces;
	private Action adaptClient;
	private Action runTests;
	private Action doubleClickAction;
	private Delta[] diffTable;
	private String oldWSDL;
	private String newWSDL;
	private ICompilationUnit oldStub;
	private ICompilationUnit newStub;
	private String wadlFilepath;
	private String wadlFilename;
	private String requestFilename;
	private String destinationFolderName;
	private IFolder destinationFolder;

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
			switch (i) {
			case 0:
				return o.getClass().getSimpleName();
			case 1:
				if (o instanceof AddDelta) {
					return ((AddDelta) o).getTarget().getClass()
							.getSimpleName();
				} else {
					return ((Delta) o).getSource().getClass().getSimpleName();
				}
			case 2:
				if (o instanceof AddDelta) {
					return "";
				} else {
					return ((Delta) o).getSource().toString();
				}
			case 3:
				if (o instanceof DeleteDelta) {
					return "";
				} else {
					return ((Delta) o).getTarget().toString();
				}
			case 4:
				if (o instanceof ChangeDelta) {
					return ((ChangeDelta) o).getChangedAttribute().toString();
				} else {
					return "";
				}
			case 5:
				if (o instanceof ChangeDelta) {
					return ((ChangeDelta) o).getOldValue().toString();
				} 
				else if(o instanceof MoveDelta) {
					return ((MoveDelta) o).getOldParent().toString();
				}
				else {
					return "";
				}
			case 6:
				if (o instanceof ChangeDelta) {
					return ((ChangeDelta) o).getNewValue().toString();
				} 
				else if(o instanceof MoveDelta) {
					return ((MoveDelta) o).getNewParent().toString();
				}
				else {
					return "";
				}
			}

			return null;
		}
	}

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public WADLView() {
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
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		viewer.getTree().setLayout(layout);
		viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Delta Type");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Element Type");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Source Element");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Target Element");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Changed Attribute");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("Old Value");
		new TreeColumn(viewer.getTree(), SWT.LEFT).setText("New Value");
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
				WADLView.this.fillContextMenu(manager);
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
		manager.add(generateWADL);
		manager.add(generateClientProxy);
		manager.add(compareInterfaces);
		manager.add(adaptClient);
		manager.add(runTests);
		manager.add(new Separator());
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(generateWADL);
		manager.add(generateClientProxy);
		manager.add(compareInterfaces);
		manager.add(adaptClient);
		manager.add(runTests);
		manager.add(new Separator());
	}

	private void makeActions() {
		generateWADL = new Action() {
			public void run() {
				IWizardDescriptor descriptor = PlatformUI.getWorkbench()
						.getNewWizardRegistry()
						.findWizard("wsdarwin.wizards.NewClientAdapterWizard");
				try {
					IWizard wizard = descriptor.createWizard();
					NewClientAdapterWizard ncaw = (NewClientAdapterWizard) wizard;
					ncaw.setAction(this);
					
					if (wadlFilename != null) {
						ncaw.setWADLFilename(wadlFilename);
					}
					if (requestFilename != null) {
						ncaw.setRequestFilename(requestFilename);
					}
					if (destinationFolderName != null) {
						ncaw.setDestinationFolderName(destinationFolderName);
					}
					if (destinationFolder != null) {
						ncaw.setDestinationFolder(destinationFolder);
					}
					
					WizardDialog wd = new WizardDialog(PlatformUI
							.getWorkbench().getActiveWorkbenchWindow()
							.getShell(), wizard);
					wd.setTitle(wizard.getWindowTitle());
					wd.open();
					
					wadlFilename = ncaw.getWADLFilename();
					wadlFilepath = ncaw.getDestinationFolderName()+"\\"+ncaw.getWADLFilename();
					requestFilename = ncaw.getRequestFilename();
					destinationFolderName = ncaw.getDestinationFolderName();
					destinationFolder = ncaw.getDestinationFolder();
					final String wadl = ncaw.getWADLFilename();
					
					
					IRunnableWithProgress op = new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException {
							try {
								monitor.beginTask("Generating WADL Interface", 1);
								try {
									ArrayList<String> requests = new ArrayList<String>();
									ArrayList<String> uris = new ArrayList<String>();
									HashMap<String, XSDFile> responses = new HashMap<String, XSDFile>();
									
									BufferedReader testIn = new BufferedReader(new FileReader(new File(requestFilename)));
									String line = testIn.readLine();
									
									while(line != null) {
										requests.add(line);
										String[] tokens = line.split(" ");
										uris.add(tokens[2]);
										line = testIn.readLine();
									}
									
									RequestAnalyzer analyzer = new RequestAnalyzer();
									String resourceBase = analyzer.batchRequestAnalysis(uris);
									for(String methodName : analyzer.getMethodNamesFromBatch(uris)) {
										responses.put(methodName, new XSDFile());
									}

									XMLGenerator generator = new XMLGenerator();
									
									WADLFile mergedWADL = new WADLFile(wadlFilepath, null, new XSDFile());
									HashSet<XSDFile> grammarSet = new HashSet<XSDFile>();
									for(String requestLine : requests) {
										String[] tokens = requestLine.split(" ");
										String id = "";
										String methodName = "";
										String urlLine = "";
										if (tokens.length>1) {
											id = tokens[0];
											methodName = tokens[1];
											urlLine = tokens[2];
										}
										analyzer.resetUriString(urlLine);
										final String FILENAME_JSON  = id+".json";
										
								        
								        // URLConnection
										URL yahoo = new URL(urlLine);
								        URLConnection yc = yahoo.openConnection();
								        BufferedReader in = new BufferedReader(
						                    new InputStreamReader(yc.getInputStream()));
								        String inputLine;
								        File jsonFile = new File(destinationFolderName+"\\"+FILENAME_JSON);
										BufferedWriter out = new BufferedWriter(new FileWriter(jsonFile));

								        while ((inputLine = in.readLine()) != null) {
								        	int listIndex = inputLine.indexOf("[");
											int mapIndex = inputLine.indexOf("{");
											if(listIndex<mapIndex) {
												inputLine = inputLine.substring(listIndex);
											}
											else {
												inputLine = inputLine.substring(mapIndex);
											}
								            out.write(inputLine);
								            out.newLine();
								        }
								        in.close();
								        out.close();
								        Response2XSD xsdBuilder = new Response2XSD();
							        
								        String methodID = "";
								        if(analyzer.getMethodID().equals("")) {
								        	methodID = analyzer.getContainingResource();
								        }
								        else {
								        	methodID = analyzer.getMethodID();
								        }
										xsdBuilder.buildXSDFromJSON(jsonFile, methodID);
										XSDFile xsdFile = xsdBuilder.getXSDFile();
										
								        WADLFile newWADL = new WADLFile(wadlFilepath, urlLine, xsdFile);
								        
								        grammarSet.add(xsdFile);
								        newWADL.buildWADL(grammarSet, analyzer, resourceBase, methodName, 200);
								        mergedWADL.compareToMerge(newWADL);
								        
										requestLine = testIn.readLine();
										jsonFile.delete();
										
									}
									generator.createWADL(mergedWADL, resourceBase);
									testIn.close();
									destinationFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
									
									} catch (MalformedURLException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (ParserConfigurationException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (CoreException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								monitor.worked(1);
							} finally {
								monitor.done();
							}
						}
					};
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.run(true, false, op);
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						IFile fileToOpen = destinationFolder.getFile(wadl);
					    try {
					        IDE.openEditor(page, fileToOpen);
					    } catch ( PartInitException e ) {
					        //Put your exception handler here if you wish to
					    }
					} catch (InterruptedException e) {
						MessageDialog.openError(PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								"Error", e.getMessage());
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						Throwable realException = e.getTargetException();
						MessageDialog.openError(PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								"Error", realException.getMessage());
						e.printStackTrace();
					}
					viewer.refresh();
					// viewer.setContentProvider(new ViewContentProvider());
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		};
		generateWADL.setText("generate");
		generateWADL.setToolTipText("Generate WADL interface");
		generateWADL.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
		generateClientProxy = new Action() {
			public void run() {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException {
						try {
							monitor.beginTask("Generating client proxy", 1);
							ToolContext context = new ToolContext();
							HashMap<String, Object> parameters = new HashMap<String, Object>();
							//parameters.put(WadlToolConstants.CFG_OUTPUTDIR, destinationFolderName);
							parameters.put(WadlToolConstants.CFG_WADLURL, wadlFilepath);
							context.setParameters(parameters);
							WADLToJava gen = new WADLToJava();
							gen.run(context);
							WADLParser parser1 = new WADLParser(new File(oldWSDL));
							WADLParser parser2 = new WADLParser(new File(newWSDL));
							Delta delta = parser1.getService().diff(parser2.getService());
							//DeltaUtil.findMoveDeltas(delta);
							diffTable = new Delta[] { delta };
							monitor.worked(1);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {
							monitor.done();
						}
					}
				};
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.run(true, false, op);
				} catch (InterruptedException e) {
					MessageDialog.openError(PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell(),
							"Error", e.getMessage());
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					Throwable realException = e.getTargetException();
					MessageDialog.openError(PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell(),
							"Error", realException.getMessage());
					e.printStackTrace();
				}
			}
		};
		generateClientProxy.setText("generateProxy");
		generateClientProxy.setToolTipText("Generate client proxy");
		generateClientProxy.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_PRINT_EDIT));
		compareInterfaces = new Action() {
			public void run() {
				IWizardDescriptor descriptor = PlatformUI.getWorkbench()
						.getNewWizardRegistry()
						.findWizard("wsdarwin.wizards.NewClientAdapterWizard");
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
					
					oldWSDL = ncaw.getOldWSDL();
					newWSDL = ncaw.getNewWSDL();
					oldStub = ncaw.getOldStub();
					newStub = ncaw.getNewStub();
					IRunnableWithProgress op = new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException {
							try {
								monitor.beginTask("Producing diff", 1);
								WADLParser parser1 = new WADLParser(new File(oldWSDL));
								WADLParser parser2 = new WADLParser(new File(newWSDL));
								Delta delta = parser1.getService().diff(parser2.getService());
								//DeltaUtil.findMoveDeltas(delta);
								diffTable = new Delta[] { delta };
								monitor.worked(1);
							} finally {
								monitor.done();
							}
						}
					};
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow()
								.run(true, false, op);
					} catch (InterruptedException e) {
						MessageDialog.openError(PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								"Error", e.getMessage());
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						Throwable realException = e.getTargetException();
						MessageDialog.openError(PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								"Error", realException.getMessage());
						e.printStackTrace();
					}
					viewer.refresh();
					// viewer.setContentProvider(new ViewContentProvider());
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		};
		compareInterfaces.setText("diff");
		compareInterfaces.setToolTipText("WADL Diff");
		compareInterfaces.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		adaptClient = new Action() {
			public void run() {
				diffTable[0].printDelta(0);
				CompilationUnit oldCompilationUnit = parseAST(oldStub);
				Map<String, Delta> changedOperationNames = new HashMap<String, Delta>();
				changedOperationNames = getChangedOperations(changedOperationNames, diffTable[0]);
				Map<MethodDeclaration, Delta> changedMethods = getChangedMethods(
						changedOperationNames, oldCompilationUnit);
				
				HashMap<String, CompilationUnit> oldCompilationUnits = new HashMap<String, CompilationUnit>();
				IPackageFragment oldPackageFragment = (IPackageFragment)oldStub.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				HashMap<String, CompilationUnit> newCompilationUnits = new HashMap<String, CompilationUnit>();
				IPackageFragment newPackageFragment = (IPackageFragment)newStub.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
				try {
					for(ICompilationUnit unit : oldPackageFragment.getCompilationUnits()) {
						oldCompilationUnits.put(unit.getElementName(), parseAST(unit));
					}
					for(ICompilationUnit unit : newPackageFragment.getCompilationUnits()) {
						newCompilationUnits.put(unit.getElementName(), parseAST(unit));
					}
				} catch (JavaModelException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				RESTClientAdaptationRefactoring refactoring = new RESTClientAdaptationRefactoring(parseAST(oldStub), parseAST(newStub),
						oldCompilationUnits, newCompilationUnits,
						(TypeDeclaration) oldCompilationUnits.get(oldStub.getElementName()).types().get(0),
						(TypeDeclaration) newCompilationUnits.get(newStub.getElementName()).types().get(0),
						changedMethods, diffTable[0]);
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
				.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		adaptClient.setEnabled(true);
		
		runTests = new Action() {
			public void run() {
				try {
					IType[] tests = JUnitCore.findTestTypes(oldStub.getJavaProject(), null);
					ICompilationUnit[] compilationUnits = new ICompilationUnit[tests.length];
					for(int i=0;i<compilationUnits.length;i++) {
						compilationUnits[i] = tests[i].getCompilationUnit();
					}
					StructuredSelection selection = new StructuredSelection(compilationUnits);
					JUnitLaunchShortcut shortcut = new JUnitLaunchShortcut();
					shortcut.launch(selection, "run");
					//Class<?> testClass = this.getClass().getClassLoader().loadClass(tests[0].getFullyQualifiedName()+".class");
					//org.junit.runner.JUnitCore.main(paths);;
					//System.out.println(res.toString());
					
				} catch (OperationCanceledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		runTests.setText("Run Tests");
		runTests.setToolTipText("Run Tests");
		runTests.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_WARN_TSK));
		runTests.setEnabled(false);
		
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
		List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
		types.addAll(compilationUnit.types());
		List<TypeDeclaration> firstLevelTypes = compilationUnit.types();
		for(TypeDeclaration type : firstLevelTypes) {
			getTypeDeclarations(type, types);
		}
		for (TypeDeclaration typeDeclaration : types) {
			MethodDeclaration[] methods = typeDeclaration.getMethods();
			for (int i = 0; i < methods.length; i++) {
				String name = containsKeyIgnoreCase(changedOperationNames,
						methods[i].getName().getIdentifier());
				if (name != null) {
					changedMethods.put(methods[i],
							changedOperationNames.get(name));
				}
			}
		}
		System.out.println();
		return changedMethods;
	}

	private void getTypeDeclarations(
			TypeDeclaration type, List<TypeDeclaration> types) {
		types.addAll(Arrays.asList(type.getTypes()));
		for(TypeDeclaration innerType : type.getTypes()) {
			getTypeDeclarations(innerType, types);
		}
	}

	private String containsKeyIgnoreCase(
			Map<String, Delta> changedOperationNames, String identifier) {
		for (String key : changedOperationNames.keySet()) {
			Operation operation = (Operation)changedOperationNames.get(key).getSource();
			String methodName = "get"+operation.getResponseMediaType()+"as"+operation.getResponse().getName();
			if (methodName.equalsIgnoreCase(identifier)) {
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

	private Map<String, Delta> getChangedOperations(
			Map<String, Delta> changedOperationNames, Delta delta) {
		if (delta.getSource() instanceof Operation) {
			if (delta instanceof ChangeDelta) {
				Operation operation = (Operation)delta.getSource();
				String responseTypeName = operation.getResponse().getName().substring(0,1).toUpperCase()+operation.getResponse().getName().substring(1);
				changedOperationNames.put("get"+operation.getRequestMediaType()+"As"+responseTypeName, delta);
			}
		} else if (!delta.getDeltas().isEmpty()) {
			for (Delta deltaChild : delta.getDeltas()) {
				changedOperationNames.putAll(getChangedOperations(
						changedOperationNames, deltaChild));
			}
		}
		return changedOperationNames;
	}
}