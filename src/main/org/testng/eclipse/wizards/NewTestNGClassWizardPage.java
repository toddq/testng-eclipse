package org.testng.eclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.Utils;
import org.testng.eclipse.util.Utils.JavaElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate a new TestNG class and optionally, the corresponding XML suite file.
 */
public class NewTestNGClassWizardPage extends WizardPage {
  private ISelection m_selection;
  private Text m_sourceFolderText;
  private Text m_packageNameText;
  private Text m_classNameText;
  private Text m_xmlFilePath;

  private Map<String, Button> m_annotations = new HashMap<String, Button>();
  public static final String[] ANNOTATIONS = new String[] {
    "BeforeMethod", "AfterMethod", "DataProvider",
    "BeforeClass", "AfterClass", "",
    "BeforeTest",  "AfterTest", "",
    "BeforeSuite", "AfterSuite", ""
  };

  public NewTestNGClassWizardPage(ISelection selection) {
    super("TestNG class");
    setTitle("TestNG class");
    setDescription("This wizard creates a new TestNG class.");
    m_selection = selection;
  }

  /**
   * @see IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    container.setLayout(layout);

    createTop(container);
    createBottom(container);

    initialize();
    dialogChanged();
    setControl(container);
  }

  private void createTop(Composite parent) {
    Composite container = SWTUtil.createGridContainer(parent, 3);

    //
    // Source folder
    //
    {
      m_sourceFolderText = SWTUtil.createPathBrowserText(container, "&Source folder:",
          new ModifyListener() {
            public void modifyText(ModifyEvent e) {
              dialogChanged();
            }
          });
    }

    //
    // Package name
    //
    {
      Label label = new Label(container, SWT.NULL);
      label.setText("&Package name:");
      m_packageNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
      m_packageNameText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          dialogChanged();
        }
      });
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      m_packageNameText.setLayoutData(gd);
      Button button = new Button(container, SWT.PUSH);
      button.setText("Browse...");
      button.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          handleBrowsePackages();
        }
      });
    }

    //
    // Class name
    //
    {
      Label label = new Label(container, SWT.NULL);
      label.setText("&Class name:");
      m_classNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      m_classNameText.setLayoutData(gd);
      m_classNameText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          dialogChanged();
        }
      });
    }
  }

  private void createBottom(Composite parent) {
    //
    // Annotations
    //
    {
      Group g = new Group(parent, SWT.SHADOW_ETCHED_OUT);
      g.setText("Annotations");
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      g.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      g.setLayout(layout);
      layout.numColumns = 3;

      for (String label : ANNOTATIONS) {
        if ("".equals(label)) {
          new Label(g, SWT.NONE);
        } else {
          Button b = new Button(g, "".equals(label) ? SWT.None : SWT.CHECK);
          m_annotations.put(label, b);
          b.setText("@" + label);
        }
      }
    }

    //
    // XML suite file
    //
    {
      Composite container = SWTUtil.createGridContainer(parent, 2);

      //
      // Label
      //
      Label label = new Label(container, SWT.NULL);
      label.setText(ResourceUtil.getString("TestNG.newClass.suitePath"));

      //
      // Text widget
      //
      m_xmlFilePath = new Text(container, SWT.SINGLE | SWT.BORDER);
      GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
      gd.grabExcessHorizontalSpace = true;
      m_xmlFilePath.setLayoutData(gd);
    }
  }

  /**
   * Tests if the current workbench selection is a suitable container to use.
   */
  private void initialize() {
    List<JavaElement> elements = Utils.getSelectedJavaElements();
    if (elements.size() > 0) {
      JavaElement sel = elements.get(0);
      if (sel.sourceFolder != null) {
        m_sourceFolderText.setText(sel.sourceFolder);
      }
      if (sel.getPackageName() != null) {
        m_packageNameText.setText(sel.getPackageName());
      }
    }
    m_classNameText.setText("NewTest");
  }

  private void handleBrowsePackages() {
  }

  /**
   * Ensures that both text fields are set.
   */
  private void dialogChanged() {
    IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(
        new Path(getSourceFolder()));
    String className = getClassName();

    if (container == null || (container.getType() &
        (IResource.ROOT | IResource.PROJECT | IResource.FOLDER)) == 0) {
      updateStatus("The source directory must exist");
      return;
    }
    if (getPackageName().length() == 0) {
      updateStatus("The package must be specified");
      return;
    }
    if (!container.isAccessible()) {
      updateStatus("Project must be writable");
      return;
    }
    if (className.length() == 0) {
      updateStatus("Class name must be specified");
      return;
    }
    if (className.replace('\\', '/').indexOf('/', 1) > 0) {
      updateStatus("Class name must be valid");
      return;
    }

    int dotLoc = className.lastIndexOf('.');
    if (dotLoc != -1) {
      String ext = className.substring(dotLoc + 1);
      if (ext.equalsIgnoreCase("java") == false) {
        updateStatus("File extension must be \"java\"");
        return;
      }
    }
    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getSourceFolder() {
    return m_sourceFolderText.getText();
  }

  public String getXmlFile() {
    return m_xmlFilePath.getText();
  }

  public String getPackageName() {
    return m_packageNameText.getText();
  }

  public String getClassName() {
    return m_classNameText.getText();
  }

  public boolean containsAnnotation(String annotation) {
    Button b = m_annotations.get(annotation);
    return b.getSelection();
  }

  public String getPackage() {
    return m_packageNameText.getText();
  }
}