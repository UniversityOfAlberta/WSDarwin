package wsdarwin.wizards;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;


/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

public class DiffInputPage extends WizardPage {

	private Text oldWSDL;
	private String oldWSDLPath;
	private Text newWSDL;
	private String newWSDLPath;
	private Text oldStub;
	private ICompilationUnit oldStubFile;
	private Text newStub;
	private ICompilationUnit newStubFile;
	

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public DiffInputPage() {
		super("wizardPage");
		setTitle("WSDarwin Comparison input");
		setDescription("Please provide two versions of the service interface for comparison and the corresponding client proxies for adaptation.");
		
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite clientContainer = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		clientContainer.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(clientContainer, SWT.NULL);
		label.setText("&Old Service Interface:");
		
		oldWSDL = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		if(oldWSDLPath != null) {
			oldWSDL.setText(oldWSDLPath);
		}
		/*else {
			oldWSDL.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\wsdls\\HiltonServicesSimple.wsdl");
		}*/
		
		oldWSDL.setLayoutData(gd);
		oldWSDL.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		Button oldWSDLBrowse = new Button(clientContainer, SWT.PUSH);
		oldWSDLBrowse.setText("Browse...");
		oldWSDLBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse("old WSDL");
			}
		});
		label = new Label(clientContainer, SWT.NULL);
		label.setText("&New Service Interface:");
		
		newWSDL = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		if(newWSDLPath != null) {
			newWSDL.setText(newWSDLPath);
		}
		/*else {
			newWSDL.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\wsdls\\HiltonServicesRenameVariable.wsdl");
		}*/
		newWSDL.setLayoutData(gd);
		newWSDL.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		Button newWSDLBrowse = new Button(clientContainer, SWT.PUSH);
		newWSDLBrowse.setText("Browse...");
		newWSDLBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse("new WSDL");
			}
		});
		label = new Label(clientContainer, SWT.NULL);
		label.setText("&Old Client Proxy:");
		
		oldStub = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		if(oldStubFile != null) {
			oldStub.setText(oldStubFile.getPath().toOSString());
		}
		/*else {
			oldStub.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\src\\pkgSimple\\HiltonServicesStub.java");
		}*/
		oldStub.setLayoutData(gd);
		oldStub.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		Button oldStubBrowse = new Button(clientContainer, SWT.PUSH);
		oldStubBrowse.setText("Browse...");
		oldStubBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse("old stub");
			}
		});
		label = new Label(clientContainer, SWT.NULL);
		label.setText("&New Client Proxy:");
		
		newStub = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		if(newStubFile != null) {
			newStub.setText(newStubFile.getPath().toOSString());
		}
		/*else {
			newStub.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\src\\pkgRenameVariable\\HiltonServicesStub.java");
		}*/
		newStub.setLayoutData(gd);
		newStub.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		Button newStubBrowse = new Button(clientContainer, SWT.PUSH);
		newStubBrowse.setText("Browse...");
		newStubBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse("new stub");
			}
		});
		
		initialize();
		dialogChanged();
		setControl(clientContainer);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

	private void initialize() {
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 * @param buttonID 
	 */

	private void handleBrowse(String buttonID) {
		/*ResourceSelectionDialog dialog = new ResourceSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(),
				"Select "+buttonID+":");
		if (dialog.open() == ResourceSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				if(buttonID.equals("old WSDL")) {
					File file = (File)result[0];
					oldWSDL.setText(file.getLocation().toOSString());
				}
				else if(buttonID.equals("new WSDL")) {
					File file = (File)result[0];
					newWSDL.setText(file.getLocation().toOSString());
				}
			}
		}*/
		
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
		dialog.setTitle("File Selection");
		dialog.setMessage("Select "+buttonID+":");
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				if(buttonID.equals("old WSDL")) {
					IFile file = (IFile)result[0];
					oldWSDL.setText(file.getLocation().toOSString());
				}
				else if(buttonID.equals("new WSDL")) {
					IFile file = (IFile)result[0];
					newWSDL.setText(file.getLocation().toOSString());
				}
				else if(buttonID.equals("old stub")) {
					IFile file = (IFile)result[0];
					oldStubFile = JavaCore.createCompilationUnitFrom(file);
					oldStub.setText(file.getLocation().toOSString());
				}
				else if(buttonID.equals("new stub")) {
					IFile file = (IFile)result[0];
					newStubFile = JavaCore.createCompilationUnitFrom(file);
					newStub.setText(file.getLocation().toOSString());
				}
			}
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {

		/*if (getClientName().length() == 0) {
			updateStatus("File container must be specified");
			return;
		}
		if (container == null
				|| (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("File container must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		if (fileName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name must be valid");
			return;
		}
		int dotLoc = fileName.lastIndexOf('.');
		if (dotLoc != -1) {
			String ext = fileName.substring(dotLoc + 1);
			if (ext.equalsIgnoreCase("mpe") == false) {
				updateStatus("File extension must be \"mpe\"");
				return;
			}
		}*/
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getOldWSDL() {
		return oldWSDL.getText();
	}

	public String getNewWSDL() {
		return newWSDL.getText();
	}

	public void setOldWSDLPath(String oldWSDLPath) {
		this.oldWSDLPath = oldWSDLPath;
	}

	public void setNewWSDLPath(String newWSDLPath) {
		this.newWSDLPath = newWSDLPath;
	}

	public ICompilationUnit getOldStubFile() {
		return oldStubFile;
	}

	public ICompilationUnit getNewStubFile() {
		return newStubFile;
	}

	public void setOldStubFile(ICompilationUnit oldStubFile) {
		this.oldStubFile = oldStubFile;
	}

	public void setNewStubFile(ICompilationUnit newStubFile) {
		this.newStubFile = newStubFile;
	}
	
	
	
}