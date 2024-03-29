package wsdarwin.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;

import wsdarwin.comparison.delta.Delta;
import wsdarwin.parsers.WSDL2Parser;
import wsdarwin.parsers.WSDLParser;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "mpe". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class NewClientAdapterWizard extends Wizard implements IWorkbenchWizard{
	private WizardPage page;
	private Delta diff;
	private Action action;
	private ISelection selection;
	private String oldWSDL;
	private String newWSDL;
	private ICompilationUnit oldStub;
	private ICompilationUnit newStub;
	private IFolder destinationFolder;
	private String destinationFolderName;
	private String wadlFilename;
	private String requestFilename;
	//private WSDLComparator comp;

	/**
	 * Constructor for SampleNewWizard.
	 */
	public NewClientAdapterWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	public NewClientAdapterWizard(Action action) {
		super();
		setNeedsProgressMonitor(true);
		this.action = action;
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		if (action.getText().equals("diff")) {
			page = new DiffInputPage();
			((DiffInputPage)page).setOldWSDLPath(oldWSDL);
			((DiffInputPage)page).setNewWSDLPath(newWSDL);
			((DiffInputPage)page).setOldStubFile(oldStub);
			((DiffInputPage)page).setNewStubFile(newStub);
		}
		else if(action.getText().equals("generate")) {
			page = new WADLGenerationInputPage();
			((WADLGenerationInputPage)page).setDestinationFolder(destinationFolder);
			((WADLGenerationInputPage)page).setDestinationFolderName(destinationFolderName);
			((WADLGenerationInputPage)page).setWadlFilename(wadlFilename);
			((WADLGenerationInputPage)page).setRequestFilename(requestFilename);
		}
		addPage(page);
		//NewClientAdapterResourcePage resourcePage = new NewClientAdapterResourcePage("Client test cases", selection);
		//WizardFileSystemResourceExportPage1 resourcePage = new WizardFileSystemResourceExportPage1(selection);
		//resourcePage.setSelectedResource(selectedResource);
		//addPage(resourcePage);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		if (page instanceof DiffInputPage) {
			oldWSDL = ((DiffInputPage) page).getOldWSDL();
			newWSDL = ((DiffInputPage) page).getNewWSDL();
			oldStub = ((DiffInputPage) page).getOldStubFile();
			newStub = ((DiffInputPage) page).getNewStubFile();
		}
		else if(page instanceof WADLGenerationInputPage) {
			destinationFolder = ((WADLGenerationInputPage)page).getDestinationFolder();
			destinationFolderName = ((WADLGenerationInputPage)page).getDestinationFolderName();
			wadlFilename = ((WADLGenerationInputPage)page).getWadlFilename();
			requestFilename = ((WADLGenerationInputPage)page).getRequestFilename();
			
		}
		/*IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(oldWSDL, newWSDL, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};*/
		/*try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}*/
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 * @param newWSDL 
	 * @param oldWSDL 
	 */

	private void doFinish(
		String oldWSDL, String newWSDL, IProgressMonitor monitor)
		throws CoreException {
		// create a sample file
		monitor.beginTask("Producing diff", 1);
		WSDLParser parser1 = new WSDLParser(oldWSDL);
		WSDLParser parser2 = new WSDLParser(newWSDL);
		setDiff(parser1.getService().diff(parser2.getService()));
		monitor.worked(1);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	public Delta getDiff() {
		return diff;
	}

	public void setDiff(Delta diff) {
		this.diff = diff;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getOldWSDL() {
		return oldWSDL;
	}

	public void setOldWSDL(String oldWSDL) {
		this.oldWSDL = oldWSDL;
	}

	public String getNewWSDL() {
		return newWSDL;
	}

	public void setNewWSDL(String newWSDL) {
		this.newWSDL = newWSDL;
	}

	public void setOldStub(ICompilationUnit oldStub) {
		this.oldStub = oldStub;
	}

	public void setNewStub(ICompilationUnit newStub) {
		this.newStub = newStub;
	}

	public ICompilationUnit getOldStub() {
		return oldStub;
	}

	public ICompilationUnit getNewStub() {
		return newStub;
	}

	public String getDestinationFolderName() {
		return destinationFolderName;
	}
	
	public void setDestinationFolderName(String foldername) {
		destinationFolderName = foldername;
	}
	
	public IFolder getDestinationFolder() {
		return destinationFolder;
	}
	
	public void setDestinationFolder(IFolder folder) {
		destinationFolder = folder;
	}

	public String getWADLFilename() {
		return wadlFilename;
	}
	
	public void setWADLFilename(String filename) {
		wadlFilename = filename;
	}

	public String getRequestFilename() {
		return requestFilename;
	}
	
	public void setRequestFilename(String filename) {
		requestFilename = filename;
	}
}