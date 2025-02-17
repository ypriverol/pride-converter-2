/*
 * Created by JFormDesigner on Fri Oct 21 14:43:14 BST 2011
 */

package uk.ac.ebi.pride.tools.converter.gui.forms;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.error.ErrorLevel;
import psidev.psi.tools.validator.ValidatorMessage;
import uk.ac.ebi.pride.tools.converter.conversion.io.PrideXmlWriter;
import uk.ac.ebi.pride.tools.converter.dao.DAO;
import uk.ac.ebi.pride.tools.converter.dao.DAOFactory;
import uk.ac.ebi.pride.tools.converter.gui.NavigationPanel;
import uk.ac.ebi.pride.tools.converter.gui.component.panels.FilterPanel;
import uk.ac.ebi.pride.tools.converter.gui.model.ConverterData;
import uk.ac.ebi.pride.tools.converter.gui.model.FileBean;
import uk.ac.ebi.pride.tools.converter.gui.model.GUIException;
import uk.ac.ebi.pride.tools.converter.gui.util.IOUtilities;
import uk.ac.ebi.pride.tools.converter.gui.util.error.ErrorDialogHandler;
import uk.ac.ebi.pride.tools.converter.report.io.ReportReader;
import uk.ac.ebi.pride.tools.converter.report.io.ReportReaderDAO;
import uk.ac.ebi.pride.tools.converter.utils.FileUtils;
import uk.ac.ebi.pride.tools.converter.utils.xml.validation.ValidatorFactory;
import uk.ac.ebi.pride.tools.filter.io.PrideXmlFilter;
import uk.ac.ebi.pride.validator.PrideXmlValidator;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * @author User #3
 */
public class FileExportForm extends AbstractForm {

    private static final Logger logger = Logger.getLogger(FileExportForm.class);
    private boolean filterOnly = false;

    public FileExportForm(boolean filterOnly) {
        initComponents();
        this.filterOnly = filterOnly;
        filterPanel1.setFilterOnly(filterOnly);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        filterPanel1 = new FilterPanel();

        //======== this ========

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(filterPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(7, 7, 7))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(filterPanel1, GroupLayout.DEFAULT_SIZE, 548, Short.MAX_VALUE)
                                .addContainerGap())
        );
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private FilterPanel filterPanel1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    @Override
    public Collection<ValidatorMessage> validateForm() {
        //nothing to validate
        return Collections.emptyList();
    }

    @Override
    public void clear() {
        //no need to inactivate next button
        filterPanel1.reset();
    }

    @Override
    public void save(ReportReaderDAO dao) {
        /* no op */
    }

    @Override
    public void load(ReportReaderDAO dao) {
        /* no op */
    }

    @Override
    public String getFormName() {
        return "File Export";
    }

    @Override
    public String getFormDescription() {
        return config.getString("fileexport.form.description");
    }

    @Override
    public Icon getFormIcon() {
        return getFormIcon("fileexport.form.icon");
    }

    @Override
    public String getHelpResource() {
        return "help.ui.export";
    }

    @Override
    public void start() {
        //nothing required in this form
        validationListerner.fireValidationListener(true);
    }

    @Override
    public void finish() throws GUIException {

        Set<FileBean> files = ConverterData.getInstance().getDataFiles();

        String outputPath = filterPanel1.getOutputPath();
        File ouptPutFolder = new File(outputPath);
        if (!ouptPutFolder.exists()) {
            ouptPutFolder.mkdir();
        }

        Properties options = ConverterData.getInstance().getOptions();

        for (FileBean fileBean : files) {

            try {

                String finalPrideXmlFile = null;
                String workingPrideXmlFile = null;

                //in the case that filterOnly is set to true, we assume that the pride xml file
                //is already created and that the form is only used to filter xml files in a separate
                //application. we do not therefore need to generate the xml file from the DAO and source file
                if (!filterOnly) {

                    //setup file paths
                    File inputFile = new File(fileBean.getInputFile());
                    String reportFileName = fileBean.getReportFile();
                    if (filterPanel1.isRemoveWorkfiles()) {
                        ConverterData.getInstance().getFilesToDelete().add(reportFileName);
                    }

                    File reportFile = new File(reportFileName);
                    String prideFile = ouptPutFolder.getAbsolutePath() + FileUtils.FILE_SEPARATOR + inputFile.getName() + ConverterData.PRIDE_XML;
                    if (filterPanel1.isGzipped()) {
                        prideFile += FileUtils.gz;
                    }
                    //setup DAO
                    DAO dao = DAOFactory.getInstance().getDAO(inputFile.getAbsolutePath(), ConverterData.getInstance().getDaoFormat());

                    //setup reader
                    ReportReader reader = new ReportReader(reportFile);

                    //update DAO configuration based on report
                    dao.setConfiguration(options);

                    //update DAO to use external spectrum file if required
                    if (fileBean.getSpectrumFile() != null) {
                        dao.setExternalSpectrumFile(fileBean.getSpectrumFile());
                    }

                    //write xml
                    PrideXmlWriter out = new PrideXmlWriter(prideFile, reader, dao, filterPanel1.isGzipped(), filterPanel1.isSubmitToIntact());
                    out.setIncludeOnlyIdentifiedSpectra(filterPanel1.isIncludeOnlyIdentifiedSpectra());

                    logger.warn("Writing PRIDE XML to " + out.getOutputFilePath());

                    NavigationPanel.getInstance().setWorkingMessage("Writing PRIDE XML file: " + prideFile);
                    out.writeXml();

                    //keep for later use
                    workingPrideXmlFile = prideFile;

                } else {
                    workingPrideXmlFile = fileBean.getInputFile();
                }

                if (filterPanel1.isFilterXml()) {

                    //get filter from options
                    PrideXmlFilter filter = filterPanel1.getFilter(workingPrideXmlFile, workingPrideXmlFile);

                    //warn user
                    logger.info("Filtering PRIDE XML file: " + filter.getOutputFilePath());
                    NavigationPanel.getInstance().setWorkingMessage("Filtering PRIDE XML file: " + filter.getOutputFilePath());

                    //run filter
                    filter.writeXml();

                    //update filtered file path
                    finalPrideXmlFile = filter.getOutputFilePath();

                    //if we're removing tempfiles, delete the old pride xml file
                    if (filterPanel1.isRemoveWorkfiles()) {
                        ConverterData.getInstance().getFilesToDelete().add(workingPrideXmlFile);
                    }

                }

                if (finalPrideXmlFile == null) {
                    finalPrideXmlFile = workingPrideXmlFile;
                }

                //store final file path
                fileBean.setOutputFile(finalPrideXmlFile);

                //if only filtering, don't waste time validating file
                if (!filterOnly) {
                    //validate PRIDE XML file

                    //prepare
                    Collection<ValidatorMessage> msgs = new ArrayList<ValidatorMessage>();

                    //is validation turned on/off?
                    boolean isValidating = Boolean.valueOf(config.getString("pride.xml.validation.enabled"));

                    if (isValidating) {
                        //warn user
                        logger.info("Validating PRIDE XML file: " + finalPrideXmlFile);

                        //validate
                        NavigationPanel.getInstance().setWorkingMessage("Validating PRIDE XML file: " + finalPrideXmlFile);
                        PrideXmlValidator validator = ValidatorFactory.getInstance().getPrideXmlValidator();
                        if (filterPanel1.isGzipped()) {
                            msgs = validator.validateGZFile(new File(finalPrideXmlFile));
                        } else {
                            msgs = validator.validate(new File(finalPrideXmlFile));
                        }
                    }

                    //store messages for later display
                    ConverterData.getInstance().setValidationMessages(finalPrideXmlFile, msgs);
                }

                if (filterPanel1.isRemoveWorkfiles()) {
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            //if we have files to delete, delete them at shutdown
                            IOUtilities.deleteFiles(ConverterData.getInstance().getFilesToDelete());
                        }
                    });
                }

            } catch (Exception e) {
                logger.fatal("Error in Converting to PrideXML files, error is " + e.getMessage(), e);
                ErrorDialogHandler.showErrorDialog(this, ErrorLevel.FATAL, "An error occurred while generating the PRIDE XML File.", "Error while generating PRIDE XML file: " + e.getMessage(), "WRAPPER-EXPORT", e);
                throw new GUIException("Error while generating PRIDE XML Files", e);
            }

        }

    }
}
