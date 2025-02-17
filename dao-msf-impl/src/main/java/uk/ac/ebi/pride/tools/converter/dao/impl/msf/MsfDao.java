/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ebi.pride.tools.converter.dao.impl.msf;

import com.compomics.thermo_msf_parser_API.highmeminstance.Modification;
import com.compomics.thermo_msf_parser_API.highmeminstance.ProcessingNode;
import com.compomics.thermo_msf_parser_API.highmeminstance.ProcessingNodeParameter;
import com.compomics.thermo_msf_parser_API.highmeminstance.WorkflowInfo;
import com.compomics.thermo_msf_parser_API.lowmeminstance.controllers.FastaLowMemController;
import com.compomics.thermo_msf_parser_API.lowmeminstance.controllers.ModificationLowMemController;
import com.compomics.thermo_msf_parser_API.lowmeminstance.controllers.PeptideLowMemController;
import com.compomics.thermo_msf_parser_API.lowmeminstance.controllers.ProcessingNodeLowMemController;
import com.compomics.thermo_msf_parser_API.lowmeminstance.controllers.ProteinLowMemController;
import com.compomics.thermo_msf_parser_API.lowmeminstance.controllers.RawFileLowMemController;
import com.compomics.thermo_msf_parser_API.lowmeminstance.controllers.SpectrumLowMemController;
import com.compomics.thermo_msf_parser_API.lowmeminstance.controllers.WorkFlowLowMemController;
import com.compomics.thermo_msf_parser_API.lowmeminstance.model.MsfFile;
import com.compomics.thermo_msf_parser_API.lowmeminstance.model.PeptideLowMem;
import com.compomics.thermo_msf_parser_API.lowmeminstance.model.ProteinLowMem;
import com.compomics.thermo_msf_parser_API.lowmeminstance.model.SpectrumLowMem;
import com.compomics.thermo_msf_parser_API.util.Joiner;
import uk.ac.ebi.pride.jaxb.model.Spectrum;
import uk.ac.ebi.pride.tools.converter.dao.DAO;
import uk.ac.ebi.pride.tools.converter.dao.DAOCvParams;
import uk.ac.ebi.pride.tools.converter.dao.DAOProperty;
import uk.ac.ebi.pride.tools.converter.dao.impl.AbstractDAOImpl;
import uk.ac.ebi.pride.tools.converter.dao.impl.msf.converters.IdentificationConverter;
import uk.ac.ebi.pride.tools.converter.dao.impl.msf.converters.PTMConverter;
import uk.ac.ebi.pride.tools.converter.dao.impl.msf.converters.SpectrumConverter;
import uk.ac.ebi.pride.tools.converter.report.model.*;
import uk.ac.ebi.pride.tools.converter.utils.ConverterException;
import uk.ac.ebi.pride.tools.converter.utils.FileUtils;
import uk.ac.ebi.pride.tools.converter.utils.InvalidFormatException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author toorn101
 */
public class MsfDao extends AbstractDAOImpl implements DAO {

    private Properties configuration = new Properties();
    //default value
    private Integer confidenceLevel = 3;
    public static String MSF_FILE_STRING = "Proteome Discoverer .msf file";
    private static MsfFile msfFile;
    private static WorkFlowLowMemController workflowController = new WorkFlowLowMemController();
    private static ProcessingNodeLowMemController processingNodeController = new ProcessingNodeLowMemController();
    private static FastaLowMemController fasta = new FastaLowMemController();
    private static ModificationLowMemController mods = new ModificationLowMemController();
    private static PeptideLowMemController peptides = new PeptideLowMemController();
    private static SpectrumLowMemController spectra = new SpectrumLowMemController();
    private static ProteinLowMemController proteins = new ProteinLowMemController();
    private static RawFileLowMemController rawFiles = new RawFileLowMemController();


    /**
     * formatter to be used in several parts of the DAO
     */
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public MsfDao(File source) {
        try {
            // TODO create connection to file
            msfFile = new MsfFile(source);
            //parser = new Parser(source.getAbsolutePath(), true); 
        } catch (Exception e) {
            throw new ConverterException("Errors while opening msf files.", e);
        }
    }

    public static Collection<DAOProperty> getSupportedProperties() {
        Collection<DAOProperty> properties = new ArrayList<DAOProperty>();

        DAOProperty<Integer> confidenceLevel = new DAOProperty<Integer>("confidence_level", 3, 1, 3);

        confidenceLevel.setUnit("level");
        confidenceLevel.setAdvanced(false);
        confidenceLevel.setDescription("Allow peptides at a certain confidence level: 1=low confidence, 2=intermediate confidence, 3=high confidence");
        confidenceLevel.setShortDescription("Minimum confidence level: 1=low confidence, 2=intermediate confidence, 3=high confidence");

        properties.add(confidenceLevel);
        return properties;
    }

    @Override
    public void setConfiguration(Properties properties) {
        if (properties != null) {
            configuration = properties;
            try {
                confidenceLevel = Integer.parseInt(configuration.getProperty("confidence_level"));
            } catch (NumberFormatException e) {
                throw new ConverterException("Invalid DAO configuration: bad value for confidence_level -> " + configuration.getProperty("confidence_level"));
            }
        }

    }

    @Override
    public Properties getConfiguration() {
        return configuration;
    }

    @Override
    public String getExperimentTitle() throws InvalidFormatException {
        WorkflowInfo temp = workflowController.getWorkFlowInfo(msfFile);
        String experimentTitle = temp.getWorkflowDescription();
        return experimentTitle;

        //return parser.getWorkFlowInfo().getWorkflowDescription();
    }

    @Override
    public String getExperimentShortLabel() {
        return workflowController.getWorkFlowInfo(msfFile).getWorkflowName();
        //return parser.getWorkFlowInfo().getWorkflowName();
    }

    @Override
    public Param getExperimentParams() {
        Param experimentParam = new Param();
        File inputFile = msfFile.getMsfFile();

        // date of search
        experimentParam.getCvParam().add(DAOCvParams.DATE_OF_SEARCH.getParam(formatter.format(inputFile.lastModified()).toString()));
        //System.out.println(inputFile.lastModified());
        // original MS format param
        experimentParam.getCvParam().add(DAOCvParams.ORIGINAL_MS_FORMAT.getParam(MSF_FILE_STRING));


        return experimentParam;
    }

    @Override
    public String getSampleName() {
        return null; // Let users fill it in themselves
    }

    @Override
    public String getSampleComment() {
        return null;
    }

    @Override
    public Param getSampleParams() {
        return new Param();
    }

    @Override
    public SourceFile getSourceFile() {
        SourceFile source = new SourceFile();
        source.setFileType(MSF_FILE_STRING);
        source.setNameOfFile(msfFile.getMsfFile().getName());
        source.setPathToFile(msfFile.getMsfFile().getAbsolutePath());

        return source;
    }

    @Override
    public Collection<Contact> getContacts() {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        return contacts;
    }

    @Override
    public InstrumentDescription getInstrument() {
        InstrumentDescription i = new InstrumentDescription();
        // We can't find the machine name from the msf
        return i;
    }

    /**
     * Software
     *
     * @return Proteome Discoverer, with version.
     * @throws InvalidFormatException
     */
    @Override
    public Software getSoftware() {
        Software s = new Software();
        s.setName("Proteome Discoverer");
        s.setVersion(workflowController.getWorkFlowInfo(msfFile).getMsfVersionInfo().getSoftwareVersion());
        s.setCompletionTime(formatter.format(workflowController.getWorkFlowInfo(msfFile).getWorkflowMessages().get(workflowController.getWorkFlowInfo(msfFile).getWorkflowMessages().size()-1).getUnixTime() * 1000));

        return s;
    }

    /**
     * The computational methods used to process the data PD uses a graph based
     * processing pipeline, with nodes doing the processing I attempt to convert
     * the parameters for the nodes into cvTerms
     *
     * @return
     */
    @Override
    public Param getProcessingMethod() {
        Param result = new Param(); //TODO: go from the processingnodes to a sensible conversion


        // Convert the node parameters into user params

        StringBuilder dotEdges = new StringBuilder();
        List<ProcessingNode> allProcessingNodes = processingNodeController.getAllProcessingNodes(msfFile);
        for (ProcessingNode node : allProcessingNodes) {
            for (ProcessingNodeParameter nodeParameter : node.getProcessingNodeParameters()) {
                UserParam param = new UserParam();
                param.setName(node.getProcessingNodeNumber() + ":" + node.getNodeName() + "(" + node.getNodeGUIDString() + "):" + nodeParameter.getParameterName());
                param.setValue(nodeParameter.getParameterValue());
                result.getUserParam().add(param);
            }

            // Meanwhile, create 'dot' digraph
            String parentNumbers = node.getProcessingNodeParentNumber();
            if (!parentNumbers.equals("")) {
                for (String parentNumber : parentNumbers.split(";")) {
                    ProcessingNode parentNode = processingNodeController.getProcessingNodeByNumber(Integer.parseInt(parentNumber),msfFile);
                    dotEdges.append(parentNode.getNodeName()).append("_").append(parentNode.getProcessingNodeNumber()).append("->").append(node.getNodeName()).append("_").append(node.getProcessingNodeNumber()).append(";");
                }
            }
        }

        UserParam dotGraph = new UserParam();
        dotGraph.setName("Workflow");
        dotGraph.setValue("digraph workflow {" + dotEdges.toString() + "}");
        result.getUserParam().add(dotGraph);

        return result;
    }

    /**
     * Wet lab protocol, can't be found in this file
     *
     * @return
     */
    @Override
    public Protocol getProtocol() {
        return null; // User has to supply
    }

    /**
     * Publication references for this study. Not known from the file.
     *
     * @return
     */
    @Override
    public Collection<Reference> getReferences() {
        return null; // User has to supply
    }

    /**
     * Return the name of the searched database Here, I assume the same database
     * is used in all of the searches that are combined in this file.
     *
     * @return
     * @throws InvalidFormatException
     */
    @Override
    public String getSearchDatabaseName() {
        String fastafiles = Joiner.join(fasta.getVirtualFastaFileNames(msfFile), ",");
        return fastafiles;
    }

    /**
     * The version of the searched database This information is usually not
     * contained in a uniform way, so we return null
     *
     * @return
     * @throws InvalidFormatException
     */
    @Override
    public String getSearchDatabaseVersion() {
        return getSearchDatabaseName();
    }

    /**
     * Return the PTMs for this file
     *
     * @return
     * @throws InvalidFormatException
     */
    @Override
    public Collection<PTM> getPTMs() {
        Collection<PTM> ptms = new ArrayList<PTM>();
        List<Modification> allmods = mods.getAllModifications(msfFile);
        for (Modification mod : allmods) {
            ptms.add(PTMConverter.convert(mod));
        }
        return ptms;
    }

    /**
     * @return @throws InvalidFormatException
     */
    @Override
    public Collection<DatabaseMapping> getDatabaseMappings() {
        Collection<DatabaseMapping> result = new ArrayList<DatabaseMapping>();
        List<String> allFastaFileNames = fasta.getVirtualFastaFileNames(msfFile);
        for (String fastafile : allFastaFileNames) {
            DatabaseMapping mapping = new DatabaseMapping();
            mapping.setSearchEngineDatabaseName(fastafile);
            mapping.setSearchEngineDatabaseVersion(fastafile);

            result.add(mapping);
        }
        return result;
    }

    /**
     * Try to unequivocally identify the search result, based on source file
     * path, creation time (here implemented as 'last modified') and a hash
     * value, here an MD5 hash of the source file.
     *
     * @return
     * @throws InvalidFormatException
     */
    @Override
    public SearchResultIdentifier getSearchResultIdentifier() {
        SearchResultIdentifier identifier = new SearchResultIdentifier();
        File inputFile = new File(msfFile.getMsfFile().getAbsolutePath());

        identifier.setSourceFilePath(inputFile.getAbsolutePath());
        identifier.setTimeCreated(formatter.format(inputFile.lastModified()).toString());
        identifier.setHash(FileUtils.MD5Hash(inputFile.getAbsolutePath()));
        //System.out.println("Identifier: " + identifier);
        return identifier;
    }

    @Override
    public Collection<CV> getCvLookup() {
        ArrayList<CV> cvs = new ArrayList<CV>();

        cvs.add(new CV("MS", "PSI Mass Spectrometry Ontology", "3.20.0", "http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo"));
        cvs.add(new CV("PRIDE", "PRIDE Controlled Vocabulary", "1.101", "http://ebi-pride.googlecode.com/svn/trunk/pride-core/schema/pride_cv.obo"));

        return cvs;
    }

    // Get the number of spectra
    @Override
    public int getSpectrumCount(boolean identifiedOnly) {
        int result;
        if (identifiedOnly) {
            result = peptides.returnNumberOfPeptides(msfFile);
        } else {
            result = spectra.getNumberOfSpectra(msfFile);
        }
        return result;
    }

    /**
     * Spectrum iterator only for the identified spectra
     */
    private class IdentifiedOnlySpectrumIterator implements Iterator<Spectrum> {

        private Set<Integer> spectrumIds = new HashSet<Integer>();
        private Iterator<Integer> spectrumIdIterator = null;
        private List<PeptideLowMem> peptidesForProtein;

        public IdentifiedOnlySpectrumIterator(Iterator<ProteinLowMem> proteinIterator) {
            while (proteinIterator.hasNext()) {
                peptidesForProtein = peptides.getPeptidesForProtein(proteinIterator.next(), msfFile);
                for (PeptideLowMem peptide : peptidesForProtein) {
                    spectrumIds.add(peptide.getSpectrumId());
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (spectrumIdIterator == null) {
                spectrumIdIterator = spectrumIds.iterator();
            }
            return spectrumIdIterator.hasNext();
        }

        @Override
        public Spectrum next() {
            SpectrumLowMem spectrum = spectra.getSpectrumForSpectrumID(spectrumIdIterator.next(), msfFile);
            Spectrum result;
            try {
                result = SpectrumConverter.convert(spectrum, msfFile);
            } catch (Exception ex) {
                throw new ConverterException("While converting spectrum " + rawFiles.getRawFileNameForFileID(spectrum.getFileId(), msfFile) + " (id=" + spectrum.getSpectrumId() + ")", ex);
            }
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Don't call remove...");
        }
    }

    @Override
    public Iterator<Spectrum> getSpectrumIterator(final boolean identifiedOnly) {
        Iterator<Spectrum> i;

        if (identifiedOnly) {
            i = new IdentifiedOnlySpectrumIterator(proteins.getAllProteins(msfFile).iterator());
        } else {
            final Iterator<Integer> spectraIter = spectra.getAllSpectraIds(msfFile).iterator();
            i = new Iterator<Spectrum>() {
                @Override
                public boolean hasNext() {
                    return spectraIter.hasNext();
                }

                @Override
                public Spectrum next() {
                    Spectrum result = null;
                    SpectrumLowMem msfSpectrum = spectra.getSpectrumForSpectrumID(spectraIter.next(), msfFile);
                    try {
                        result = SpectrumConverter.convert(msfSpectrum, msfFile);
                    } catch (Exception ex) {
                        throw new RuntimeException("While converting spectrum " + rawFiles.getRawFileNameForFileID(msfSpectrum.getFileId(), msfFile), ex);
                    }
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Don't call remove..");
                }
            };
        }

        return i;
    }

    @Override
    public int getSpectrumReferenceForPeptideUID(String peptideUID) {
        return spectra.getSpectrumForPeptideID(Integer.parseInt(peptideUID), msfFile).getSpectrumId();
    }

    /**
     * Return a protein identification by UID
     *
     * @param identificationUID
     * @return
     * @throws InvalidFormatException
     */
    @Override
    public Identification getIdentificationByUID(String identificationUID) {
        ProteinLowMem p = proteins.getProteinForProteinId(Integer.parseInt(identificationUID),msfFile);
        return IdentificationConverter.convert(p, getSearchDatabaseName(), getSearchDatabaseVersion(), true, confidenceLevel, msfFile);
    }

    /**
     * @param preScanMode
     * @return
     * @throws InvalidFormatException
     */
    @Override
    public Iterator<Identification> getIdentificationIterator(final boolean preScanMode) {
        IdentificationIterator it = new IdentificationIterator(msfFile, getSearchDatabaseName(), getSearchDatabaseVersion(), preScanMode, confidenceLevel);
        return it;
    }

    @Override
    public void setExternalSpectrumFile(String filename) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
