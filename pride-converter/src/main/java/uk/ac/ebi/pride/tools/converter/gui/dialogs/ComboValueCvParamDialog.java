/*
 * Created by JFormDesigner on Mon Jan 16 14:23:03 GMT 2012
 */

package uk.ac.ebi.pride.tools.converter.gui.dialogs;

import uk.ac.ebi.pride.toolsuite.ols.dialog.OLSDialog;
import uk.ac.ebi.pride.toolsuite.ols.dialog.OLSInputable;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.error.ErrorLevel;
import uk.ac.ebi.pride.tools.converter.gui.NavigationPanel;
import uk.ac.ebi.pride.tools.converter.gui.component.table.ParamTable;
import uk.ac.ebi.pride.tools.converter.gui.interfaces.CvUpdatable;
import uk.ac.ebi.pride.tools.converter.gui.util.error.ErrorDialogHandler;
import uk.ac.ebi.pride.tools.converter.report.model.CvParam;
import uk.ac.ebi.pride.tools.converter.report.model.ReportObject;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfigProd;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Identifier;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * @author User #3
 */
public class ComboValueCvParamDialog extends AbstractDialog implements OLSInputable, KeyListener, Comparator<String> {

    private static final Logger logger = Logger.getLogger(CvParamDialog.class);
    private boolean working = false;
    private Map<String, String> olsResults = new TreeMap<String, String>(this);
    private Set<String> suggestedCvLabels = new HashSet<String>();
    private String queryString;
    private Set<String> sortedValues = new TreeSet<String>();

    public ComboValueCvParamDialog(Frame owner, CvUpdatable paramTable, Set<String> suggestedCVs, Collection<String> comboBoxValues) {
        super(owner);
        callback = paramTable;
        initComponents();
        //so that the key events get captured properly
        nameField.getEditor().getEditorComponent().addKeyListener(this);
        initSuggestedCVs(suggestedCVs);
        if (comboBoxValues != null) {
            sortedValues = new TreeSet<String>(comboBoxValues);
            valueComboBox.setModel(new DefaultComboBoxModel(sortedValues.toArray()));
        }
    }

    public ComboValueCvParamDialog(Dialog owner, CvUpdatable paramTable, Set<String> suggestedCVs, Collection<String> comboBoxValues) {
        super(owner);
        callback = paramTable;
        initComponents();
        //so that the key events get captured properly
        nameField.getEditor().getEditorComponent().addKeyListener(this);
        initSuggestedCVs(suggestedCVs);
        if (comboBoxValues != null) {
            sortedValues = new TreeSet<String>(comboBoxValues);
            valueComboBox.setModel(new DefaultComboBoxModel(sortedValues.toArray()));
        }
    }

    //this constructor is required for AbstractDialog getInstance()
    public ComboValueCvParamDialog(Frame owner, ParamTable paramTable) {
        super(owner);
        callback = paramTable;
        initComponents();
        //so that the key events get captured properly
        nameField.getEditor().getEditorComponent().addKeyListener(this);
    }

    private void initSuggestedCVs(Set<String> suggestedCVs) {
        if (suggestedCVs != null) {
            suggestedCvLabels.addAll(suggestedCVs);
            //update title bar for user information
            setTitle("Cv Param - Suggested CVs: " + suggestedCvLabels);
            //if there is only one suggested cv, set it directly
            if (suggestedCVs.size() == 1) {
                cvField.setText(suggestedCVs.iterator().next());
            }
        }
    }

    private void olsButtonActionPerformed() {
        new OLSDialog(this, this, true, "", "", accessionField.getText());
    }

    private void cancelButtonActionPerformed(ActionEvent e) {
        setVisible(false);
        dispose();
    }

    private void okButtonActionPerformed(ActionEvent e) {

        if (!isEditing) {
            if (applyToAllSubsamplesCheckBox.isSelected()) {
                for (String subsample : sortedValues) {
                    callback.add(new CvParam(cvField.getText(), accessionField.getText(), (nameField.getSelectedItem() != null) ? nameField.getSelectedItem().toString() : null, subsample));
                }
            } else {
                callback.add(new CvParam(cvField.getText(), accessionField.getText(), (nameField.getSelectedItem() != null) ? nameField.getSelectedItem().toString() : null, valueComboBox.getSelectedItem().toString()));
            }
        } else {
            callback.update(new CvParam(cvField.getText(), accessionField.getText(), (nameField.getSelectedItem() != null) ? nameField.getSelectedItem().toString() : null, valueComboBox.getSelectedItem().toString()), modelRowIndex);
        }
        setVisible(false);
        dispose();
    }

    @Override
    public void edit(ReportObject object, int modelRowIndex) {

        this.modelRowIndex = modelRowIndex;

        CvParam c = (CvParam) object;
        cvField.setText(c.getCvLabel());
        accessionField.setText(c.getAccession());
        nameField.setSelectedItem(c.getName());
        valueComboBox.setSelectedItem(c.getValue());
        validateRequiredField(cvField, null);
        validateRequiredField(nameField, null);
        validateRequiredField(accessionField, null);
        validateRequiredField(valueComboBox, null);
        okButton.setEnabled(isNonNullTextField(cvField.getText()) && isNonNullTextField(accessionField.getText()) && nameField.getSelectedItem() != null && valueComboBox.getSelectedItem() != null);

    }

    private void nameFieldKeyTyped(KeyEvent e) {

        boolean editorOk = false;
        Object editObj = nameField.getEditor().getItem();
        if (editObj != null && !"".equals(editObj.toString().trim())) {
            editorOk = true;
        }
        okButton.setEnabled(isNonNullTextField(cvField.getText()) && isNonNullTextField(accessionField.getText()) && editorOk);

        //ignore these events
        switch (e.getKeyChar()) {
            case '\t':
            case '\b':
            case '\n':
                return;
        }

        //keep track of current string
        queryString = nameField.getEditor().getItem().toString() + e.getKeyChar();

        //don't send requests while one is already being processed
        if (!working) {

            //only start sending queries when the length is big enough
            if (queryString.length() > 3) {

                working = true;

                //query OLS in background
                final SwingWorker sw = new SwingWorker() {
                    @Override
                    protected Object doInBackground() throws Exception {

                        //reset old OLS values
                        olsResults.clear();

                        try {
                            //run in background
                            //perform OLS query
                            //if there is a cv already defined, use that, otherwise leave it null
                            //and query everything
                            String cv = cvField.getText();
                            if (cv != null && "".equals(cv.trim())) {
                                cv = null;
                            }
                            OLSClient olsClient = new OLSClient(new OLSWsConfigProd());
                            olsClient.getTermsByName(queryString, cv, false);
                            List<Term> terms =  olsClient.getTermsByName(queryString, cv, false);

                            //filter out terms
                            for (Term term : terms) {
                                //if there are no pre-set filters, keep everything
                                if (suggestedCvLabels.isEmpty()) {
                                    olsResults.put(term.getLabel(), term.getTermOBOId().getIdentifier());
                                } else {
                                    //otherwise only keep the desired terms
                                    for (String suggested : suggestedCvLabels) {
                                        //need to amend this check for NEWT, because the terms aren't prefixed!
                                        if (term.getTermOBOId().getIdentifier().startsWith(suggested) || ("NEWT".equals(suggested) && term.getTermOBOId().getIdentifier().matches("[0-9]*"))) {
                                            olsResults.put(term.getLabel(), term.getTermOBOId().getIdentifier());
                                            break;
                                        }
                                    }
                                }
                            }

                            return null;
                        } catch (Exception e) {
                            logger.error("OLS Error: " + e.getMessage(), e);
                            olsResults.clear();
                            olsResults.put("OLS ERROR", "OLS ERROR");
                            return null;
                        }

                    }
                };
                //add a listener to the swingworker so that the navigation flow can continue
                //once the background thread is done
                sw.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
                            try {

                                //if there is no error, we can proceed
                                Exception value = (Exception) sw.get();
                                if (value == null) {
                                    //add back options
                                    nameField.setModel(new DefaultComboBoxModel(olsResults.keySet().toArray()));
                                    //this is to try and avoid the following exception that has been seen:
                                    //  component must be showing on the screen to determine its location
                                    nameField.setVisible(true);
                                    nameField.getEditor().setItem(queryString);
                                    nameField.setSelectedIndex(-1);
                                    nameField.setPopupVisible(true);
                                    working = false;
                                    setLoadingIcon(false);
                                } else {
                                    logger.error("Execution error: " + value.getMessage(), value);
                                    ErrorDialogHandler.showErrorDialog(NavigationPanel.getInstance(), ErrorLevel.FATAL, "Error executing background job", "An error occurred while processing the conversion", "NAVIGATOR_PANEL", value);
                                }
                            } catch (Exception e) {
                                logger.error("Execution error: " + e.getMessage(), e);
                                ErrorDialogHandler.showErrorDialog(NavigationPanel.getInstance(), ErrorLevel.FATAL, "Error executing background job", "An error occurred while processing the conversion", "NAVIGATOR_PANEL", e);
                            }
                        }

                    }
                });
                sw.execute();
                //upadte UI
                setLoadingIcon(true);
                //clear old results
                olsResults.clear();

            }
        }
    }


    private void setLoadingIcon(boolean active) {
        if (active) {
            URL imgURL = getClass().getResource("/images/loading.gif");
            if (imgURL != null) {
                iconLabel.setIcon(new ImageIcon(imgURL));
            }
        } else {
            URL imgURL = getClass().getResource("/images/loading-off.gif");
            if (imgURL != null) {
                iconLabel.setIcon(new ImageIcon(imgURL));
            }
        }
        iconLabel.revalidate();
        repaint();
    }

    private void accessionFieldActionPerformed() {
        if ("".equals(cvField.getText()) || cvField.getText() == null) {
            JOptionPane.showMessageDialog(this, "Please enter a valid CV Label", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            OLSClient olsClient = new OLSClient(new OLSWsConfigProd());
            Term term = olsClient.getTermById(new Identifier(accessionField.getText(), Identifier.IdentifierType.OBO), cvField.getText());
            nameField.setSelectedItem(term.getLabel());
        } catch (Exception e1) {
            logger.error("OLS Error: " + e1.getMessage(), e1);
        }
    }

    private void nameFieldItemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            String ternName = nameField.getSelectedItem().toString();
            String accession = olsResults.get(ternName);
            if (accession != null) {
                accessionField.setText(accession);
                validateRequiredField(accessionField, null);
                //try and parse CV
                int ndx = accession.indexOf(":");
                if (ndx > -1) {
                    cvField.setText(accession.substring(0, ndx));
                    validateRequiredField(cvField, null);
                } else {
                    //this will happen in the case of NEWT
                    if (accession.matches("[0-9]*")) {
                        cvField.setText("NEWT");
                        validateRequiredField(cvField, null);
                    }
                }
            }
        }
        okButton.setEnabled(isNonNullTextField(cvField.getText()) && isNonNullTextField(accessionField.getText()) && nameField.getSelectedItem() != null && valueComboBox.getSelectedItem() != null);
    }

    private void requiredFieldFocusLost(FocusEvent e) {
        validateRequiredField(e.getComponent(), null);
        okButton.setEnabled(isNonNullTextField(cvField.getText()) && isNonNullTextField(accessionField.getText()) && nameField.getSelectedItem() != null && valueComboBox.getSelectedItem() != null);
    }

    private void requiredFieldKeyTyped(KeyEvent e) {
        validateRequiredField(e.getComponent(), e);
        String s1 = cvField.getText();
        String s2 = accessionField.getText();
        if (e.getSource().equals(cvField)) {
            s1 += e.getKeyChar();
        } else if (e.getSource().equals(accessionField)) {
            s2 += e.getKeyChar();
        }
        okButton.setEnabled(isNonNullTextField(s1) && isNonNullTextField(s2) && nameField.getSelectedItem() != null && valueComboBox.getSelectedItem() != null);
    }

    private void valueComboBoxItemStateChanged() {
        okButton.setEnabled(isNonNullTextField(cvField.getText()) && isNonNullTextField(accessionField.getText()) && nameField.getSelectedItem() != null && valueComboBox.getSelectedItem() != null);
    }

    private void applyToAllSubsamplesCheckBoxItemStateChanged() {
        if (applyToAllSubsamplesCheckBox.isSelected()) {
            valueComboBox.setModel(new DefaultComboBoxModel(new String[]{"Apply to all"}));
            valueComboBox.setSelectedIndex(0);
            validateRequiredField(valueComboBox, null);
            valueComboBox.setEnabled(false);
        } else {
            valueComboBox.setModel(new DefaultComboBoxModel(sortedValues.toArray()));
            valueComboBox.setSelectedItem(null);
            valueComboBox.setEnabled(true);
        }
        okButton.setEnabled(isNonNullTextField(cvField.getText()) && isNonNullTextField(accessionField.getText()) && nameField.getSelectedItem() != null && valueComboBox.getSelectedItem() != null);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        label4 = new JLabel();
        label1 = new JLabel();
        label2 = new JLabel();
        label3 = new JLabel();
        nameField = new JComboBox();
        accessionField = new JTextField();
        cvField = new JTextField();
        label9 = new JLabel();
        olsButton = new JButton();
        label7 = new JLabel();
        iconLabel = new JLabel();
        label8 = new JLabel();
        cancelButton = new JButton();
        okButton = new JButton();
        valueComboBox = new JComboBox();
        label10 = new JLabel();
        applyToAllSubsamplesCheckBox = new JCheckBox();

        //======== this ========
        setResizable(false);
        setTitle("Cv Param");
        Container contentPane = getContentPane();

        //---- label4 ----
        label4.setText("CV");
        label4.setHorizontalAlignment(SwingConstants.RIGHT);

        //---- label1 ----
        label1.setText("Accession");
        label1.setHorizontalAlignment(SwingConstants.RIGHT);
        label1.setLabelFor(accessionField);

        //---- label2 ----
        label2.setText("Name");
        label2.setHorizontalAlignment(SwingConstants.RIGHT);

        //---- label3 ----
        label3.setText("Value");
        label3.setHorizontalAlignment(SwingConstants.RIGHT);

        //---- nameField ----
        nameField.setEditable(true);
        nameField.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                nameFieldItemStateChanged(e);
            }
        });

        //---- accessionField ----
        accessionField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accessionFieldActionPerformed();
            }
        });
        accessionField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                requiredFieldFocusLost(e);
            }
        });
        accessionField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                requiredFieldKeyTyped(e);
            }
        });

        //---- cvField ----
        cvField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                requiredFieldFocusLost(e);
            }
        });
        cvField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                requiredFieldKeyTyped(e);
            }
        });

        //---- label9 ----
        label9.setText("*");
        label9.setForeground(Color.red);

        //---- olsButton ----
        olsButton.setText("OLS");
        olsButton.setFocusTraversalPolicyProvider(true);
        olsButton.setFocusCycleRoot(true);
        olsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                olsButtonActionPerformed();
            }
        });

        //---- label7 ----
        label7.setText("*");
        label7.setForeground(Color.red);

        //---- iconLabel ----
        iconLabel.setIcon(new ImageIcon(getClass().getResource("/images/loading-off.gif")));

        //---- label8 ----
        label8.setText("*");
        label8.setForeground(Color.red);

        //---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelButtonActionPerformed(e);
            }
        });

        //---- okButton ----
        okButton.setText("OK");
        okButton.setEnabled(false);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okButtonActionPerformed(e);
            }
        });

        //---- valueComboBox ----
        valueComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                requiredFieldFocusLost(e);
            }
        });
        valueComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                valueComboBoxItemStateChanged();
            }
        });

        //---- label10 ----
        label10.setText("*");
        label10.setForeground(Color.red);

        //---- applyToAllSubsamplesCheckBox ----
        applyToAllSubsamplesCheckBox.setText("Use for all labels");
        applyToAllSubsamplesCheckBox.setFont(new Font("Dialog", Font.BOLD, 10));
        applyToAllSubsamplesCheckBox.setMargin(new Insets(0, 2, 2, 2));
        applyToAllSubsamplesCheckBox.setToolTipText("Use this annotation for all labelled subsamoples");
        applyToAllSubsamplesCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                applyToAllSubsamplesCheckBoxItemStateChanged();
            }
        });

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(contentPaneLayout.createParallelGroup()
                                        .addGroup(contentPaneLayout.createSequentialGroup()
                                                .addComponent(label4, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(cvField, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(label9, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(olsButton, GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE))
                                        .addGroup(contentPaneLayout.createSequentialGroup()
                                                .addGroup(contentPaneLayout.createParallelGroup()
                                                        .addComponent(label1, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(label2, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(label3, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(contentPaneLayout.createParallelGroup()
                                                        .addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
                                                                .addComponent(applyToAllSubsamplesCheckBox)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 125, Short.MAX_VALUE)
                                                                .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cancelButton))
                                                        .addGroup(contentPaneLayout.createSequentialGroup()
                                                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(valueComboBox, GroupLayout.Alignment.LEADING, 0, 342, Short.MAX_VALUE)
                                                                        .addComponent(nameField, GroupLayout.Alignment.LEADING, 0, 342, Short.MAX_VALUE)
                                                                        .addComponent(accessionField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 342, Short.MAX_VALUE))
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                                        .addGroup(contentPaneLayout.createSequentialGroup()
                                                                                .addComponent(iconLabel)
                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addGroup(contentPaneLayout.createParallelGroup()
                                                                                        .addComponent(label8, GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
                                                                                        .addComponent(label7, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)))
                                                                        .addComponent(label10, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE))))))
                                .addContainerGap())
        );
        contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cvField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(label9)
                                        .addComponent(label4)
                                        .addComponent(olsButton))
                                .addGap(11, 11, 11)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(accessionField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(label1)
                                        .addComponent(label7))
                                .addGap(15, 15, 15)
                                .addGroup(contentPaneLayout.createParallelGroup()
                                        .addComponent(nameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                .addGroup(contentPaneLayout.createSequentialGroup()
                                                        .addGap(5, 5, 5)
                                                        .addComponent(label2))
                                                .addComponent(iconLabel, GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
                                                .addComponent(label8, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGap(16, 16, 16)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(label3)
                                        .addComponent(valueComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(label10, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(contentPaneLayout.createParallelGroup()
                                        .addComponent(applyToAllSubsamplesCheckBox)
                                        .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                .addComponent(okButton)
                                                .addComponent(cancelButton)))
                                .addGap(12, 12, 12))
        );
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel label4;
    private JLabel label1;
    private JLabel label2;
    private JLabel label3;
    private JComboBox nameField;
    private JTextField accessionField;
    private JTextField cvField;
    private JLabel label9;
    private JButton olsButton;
    private JLabel label7;
    private JLabel iconLabel;
    private JLabel label8;
    private JButton cancelButton;
    private JButton okButton;
    private JComboBox valueComboBox;
    private JLabel label10;
    private JCheckBox applyToAllSubsamplesCheckBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

/*    @Override
    public void insertOLSResult(String field, String selectedValue, String accession, String ontologyShort, String ontologyLong, int modifiedRow, String mappedTerm, Map<String, String> metadata) {
        cvField.setText(ontologyShort);
        nameField.setSelectedItem(selectedValue);
        accessionField.setText(accession);
        validateRequiredField(cvField, null);
        validateRequiredField(nameField, null);
        validateRequiredField(accessionField, null);
        okButton.setEnabled(isNonNullTextField(cvField.getText()) && isNonNullTextField(accessionField.getText()) && nameField.getSelectedItem() != null && valueComboBox.getSelectedItem() != null);
    }*/

    /**
     * Inserts the selected cv term into the parent frame or dialog. If the
     * frame (or dialog) contains more than one OLS term, the field label can be
     * used to separate between the two. Modified row is used if the cv terms
     * are in a table and one of them are altered.
     *
     * @param field the name of the field where the CV term will be inserted
     * @param selectedValue the value to search for
     * @param accession the accession number to search for
     * @param ontologyShort short name of the ontology to search in, e.g., GO or
     * MOD
     * @param ontologyLong long ontology name, e.g., Gene Ontology [GO]
     * @param modifiedRow if the CV terms is going to be inserted into a table,
     * the row number can be provided here, use -1 if inserting a new row
     * @param mappedTerm the name of the previously mapped term, can be null
     * @param metadata the metadata associated with the current term (can be
     * null or empty)
     */
    @Override
    public void insertOLSResult(String field, Term selectedValue, Term accession,
                                String ontologyShort, String ontologyLong, int modifiedRow, String mappedTerm, List<String> metadata) {
        String ontoShort = (accession.getOntologyName().equalsIgnoreCase("ncbitaxon") ? "NEWT" : accession.getOntologyName().toUpperCase());
        cvField.setText(ontoShort);
        nameField.setSelectedItem(accession.getLabel());
        accessionField.setText(accession.getOntologyName().equalsIgnoreCase("ncbitaxon")
                ? accession.getTermOBOId().getIdentifier().substring(accession.getTermOBOId().getIdentifier().indexOf(':')+1)
                : accession.getTermOBOId().getIdentifier());
        validateRequiredField(cvField, null);
        validateRequiredField(nameField, null);
        validateRequiredField(accessionField, null);
        okButton.setEnabled(isNonNullTextField(cvField.getText()) && isNonNullTextField(accessionField.getText())
                && nameField.getSelectedItem() != null && valueComboBox.getSelectedItem() != null);
    }

    @Override
    public Window getWindow() {
        return NavigationPanel.getInstance();
    }

    /**
     * Invoked when a key has been typed.
     * See the class description for {@link java.awt.event.KeyEvent} for a definition of
     * a key typed event.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        nameFieldKeyTyped(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        /* no op */
    }

    @Override
    public void keyReleased(KeyEvent e) {
        /* no op */
    }

    @Override
    public int compare(String s1, String s2) {
        if (s1.length() == s2.length()) {
            return s1.compareTo(s2);
        } else {
            if (s1.length() > s2.length()) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}
