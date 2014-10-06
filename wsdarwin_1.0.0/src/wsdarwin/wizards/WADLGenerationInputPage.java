package wsdarwin.wizards;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class WADLGenerationInputPage extends WizardPage {
	
	private IFolder destinationFolder;
	private String destinationFolderName;
	private String wadlFilename;
	private String requestFilename;

	public WADLGenerationInputPage(String pageName) {
		super(pageName);
	}

	public WADLGenerationInputPage(String pageName, String title,
			ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	
	public WADLGenerationInputPage() {
		super("wizardPage");
		setTitle("WADL Generation input");
		setDescription("Please provide a set of URL requests for the service you want its interface generated. Click the button on the right to add more requests.");
	}
	

	public IFolder getDestinationFolder() {
		return destinationFolder;
	}

	public String getDestinationFolderName() {
		return destinationFolderName;
	}

	public String getWadlFilename() {
		return wadlFilename;
	}

	public String getRequestFilename() {
		return requestFilename;
	}

	public void setDestinationFolder(IFolder destinationFolder) {
		this.destinationFolder = destinationFolder;
	}

	public void setDestinationFolderName(String destinationFolderName) {
		this.destinationFolderName = destinationFolderName;
	}

	public void setWadlFilename(String wadlFilename) {
		this.wadlFilename = wadlFilename;
	}

	public void setRequestFilename(String requestFilename) {
		this.requestFilename = requestFilename;
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
		label.setText("&Destination Folder:");
		
		final Text folderText = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		if(destinationFolderName != null) {
			folderText.setText(destinationFolderName);
		}
		/*else {
			oldWSDL.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\wsdls\\HiltonServicesSimple.wsdl");
		}*/
		
		folderText.setLayoutData(gd);
		folderText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		Button browseButton = new Button(clientContainer, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse("Destination Folder", folderText);
			}
		});
		
		label = new Label(clientContainer, SWT.NULL);
		label.setText("&WADL filename:");
		
		final Text fileText = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		if(wadlFilename != null) {
			fileText.setText(wadlFilename);
		}
		/*else {
			oldWSDL.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\wsdls\\HiltonServicesSimple.wsdl");
		}*/
		
		fileText.setLayoutData(gd);
		fileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				wadlFilename = fileText.getText();
			}
		});
		
		label = new Label(clientContainer, SWT.NULL);

		label = new Label(clientContainer, SWT.NULL);
		label.setText("&Request file path:");
		
		final Text requestText = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		if(requestFilename != null) {
			requestText.setText(requestFilename);
		}
		/*else {
			oldWSDL.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\wsdls\\HiltonServicesSimple.wsdl");
		}*/
		
		requestText.setLayoutData(gd);
		requestText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		browseButton = new Button(clientContainer, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse("Request File", requestText);
			}
		});
		
		initialize();
		dialogChanged();
		setControl(clientContainer);
	}

	/*private Composite createRequestsControl(Composite parent) {
		Composite labelContainer = new Composite(parent, SWT.NULL);
		FillLayout fillLayout = new FillLayout();
		labelContainer.setLayout(fillLayout);
		
		Label label = new Label(labelContainer, SWT.NULL);
		label.setText("&Input URL requests. Click the button to add more requests.");
		
		final Composite requestsContainer = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		requestsContainer.setLayout(layout);
		layout.numColumns = 4;
		layout.verticalSpacing = 9;
		
		for(int i=0; i<requests.size(); i++) {
			label = new Label(requestsContainer, SWT.NULL);
			label.setText("&"+(i+1)+".");
			
			Combo combo = new Combo (requestsContainer, SWT.READ_ONLY);
			String[] httpMethods = new String[]{"GET", "PUT", "POST", "DELETE"};
			combo.setItems (httpMethods);
			for(int j=0; j<httpMethods.length; j++) {
				if(requests.get(i).getMethod().equals(httpMethods[j])) {
					combo.select(j);
				}
			}
			
			final Text requestText = new Text(requestsContainer, SWT.BORDER | SWT.SINGLE);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			
			requestText.setLayoutData(gd);
			requestText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					dialogChanged();
				}
			});
			
			requestText.setText(requests.get(i).getUrl());
			
			if (i==requests.size()-1) {
				Button oldWSDLBrowse = new Button(requestsContainer,
						SWT.PUSH);
				oldWSDLBrowse.setText("Add URL");
				oldWSDLBrowse.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						requests.add(new URLRequest("","",""));
						Label label = new Label(requestsContainer, SWT.NULL);
						label.setText("&"+requests.size()+".");
						
						Combo combo = new Combo (requestsContainer, SWT.READ_ONLY);
						String[] httpMethods = new String[]{"GET", "PUT", "POST", "DELETE"};
						combo.setItems (httpMethods);
						for(int j=0; j<httpMethods.length; j++) {
							if(requests.get(requests.size()-1).getMethod().equals(httpMethods[j])) {
								combo.select(j);
							}
						}
						
						final Text requestText = new Text(requestsContainer, SWT.BORDER | SWT.SINGLE);
						GridData gd = new GridData(GridData.FILL_HORIZONTAL);
						
						requestText.setLayoutData(gd);
						requestText.addModifyListener(new ModifyListener() {
							public void modifyText(ModifyEvent e) {
								dialogChanged();
							}
						});
						
						requestText.setText(requests.get(requests.size()-1).getUrl());
					}
				});
			}
			else {
				Label emptyLabel = new Label(requestsContainer, SWT.NULL);
			}
		}
		return requestsContainer;
		
	}*/

	/*private void createFileDestinationControl(Composite parent) {
		Composite clientContainer = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		clientContainer.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		
		Label label = new Label(clientContainer, SWT.NULL);
		label.setText("&Destination Folder:");
		
		final Text folderText = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		if(destinationFolderName != null) {
			folderText.setText(destinationFolderName);
		}
		else {
			oldWSDL.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\wsdls\\HiltonServicesSimple.wsdl");
		}
		
		folderText.setLayoutData(gd);
		folderText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		Button browseButton = new Button(clientContainer, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse("folder", folderText);
			}
		});
		
		label = new Label(clientContainer, SWT.NULL);
		label.setText("&WADL filename:");
		
		Text fileText = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		if(wadlFilename != null) {
			fileText.setText(wadlFilename);
		}
		else {
			oldWSDL.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\wsdls\\HiltonServicesSimple.wsdl");
		}
		
		fileText.setLayoutData(gd);
		fileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		label = new Label(clientContainer, SWT.NULL);

		label = new Label(clientContainer, SWT.NULL);
		label.setText("&Request file path:");
		
		final Text requestText = new Text(clientContainer, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		if(requestFilename != null) {
			requestText.setText(requestFilename);
		}
		else {
			oldWSDL.setText("C:\\Users\\Marios\\Desktop\\eclipse\\runtime-EclipseApplication\\HiltonServicesClient\\wsdls\\HiltonServicesSimple.wsdl");
		}
		
		requestText.setLayoutData(gd);
		requestText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		
		browseButton = new Button(clientContainer, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse("request file", requestText);
			}
		});
	}*/

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

	private void handleBrowse(String buttonID, Text textField) {
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
		dialog.setTitle(buttonID+" selection");
		dialog.setMessage("Select "+buttonID+":");
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				IResource resource = (IResource)result[0];
				textField.setText(resource.getLocation().toOSString());
				if(buttonID.equals("Request File")) {
					requestFilename = textField.getText();
				}
				else if(buttonID.equals("Destination Folder")) {
					destinationFolderName = textField.getText();
					destinationFolder = (IFolder)resource;
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


}
