package uk.ac.ebi.pride.tools.converter.dao.impl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.ebi.pride.jaxb.model.Data;
import uk.ac.ebi.pride.jaxb.model.IntenArrayBinary;
import uk.ac.ebi.pride.jaxb.model.MzArrayBinary;
import uk.ac.ebi.pride.jaxb.model.Precursor;
import uk.ac.ebi.pride.jaxb.model.PrecursorList;
import uk.ac.ebi.pride.jaxb.model.Spectrum;
import uk.ac.ebi.pride.jaxb.model.SpectrumDesc;
import uk.ac.ebi.pride.jaxb.model.SpectrumInstrument;
import uk.ac.ebi.pride.jaxb.model.SpectrumSettings;
import uk.ac.ebi.pride.tools.converter.dao.DAO;
import uk.ac.ebi.pride.tools.converter.dao.DAOCvParams;
import uk.ac.ebi.pride.tools.converter.dao.DAOProperty;
import uk.ac.ebi.pride.tools.converter.report.model.CV;
import uk.ac.ebi.pride.tools.converter.report.model.Contact;
import uk.ac.ebi.pride.tools.converter.report.model.DatabaseMapping;
import uk.ac.ebi.pride.tools.converter.report.model.Identification;
import uk.ac.ebi.pride.tools.converter.report.model.InstrumentDescription;
import uk.ac.ebi.pride.tools.converter.report.model.PTM;
import uk.ac.ebi.pride.tools.converter.report.model.Param;
import uk.ac.ebi.pride.tools.converter.report.model.Protocol;
import uk.ac.ebi.pride.tools.converter.report.model.Reference;
import uk.ac.ebi.pride.tools.converter.report.model.SearchResultIdentifier;
import uk.ac.ebi.pride.tools.converter.report.model.Software;
import uk.ac.ebi.pride.tools.converter.report.model.SourceFile;
import uk.ac.ebi.pride.tools.converter.report.model.UserParam;
import uk.ac.ebi.pride.tools.converter.utils.ConverterException;
import uk.ac.ebi.pride.tools.converter.utils.FileUtils;
import uk.ac.ebi.pride.tools.converter.utils.InvalidFormatException;
import uk.ac.ebi.pride.tools.jmzreader.JMzReaderException;
import uk.ac.ebi.pride.tools.ms2_parser.Ms2File;
import uk.ac.ebi.pride.tools.ms2_parser.model.Ms2Spectrum;

/**
 * This DAO converts ms2 files into PRIDE XML files. It
 * uses the ms2-parser library to parse the ms2 files.
 *
 * @author jg
 */
public class Ms2DAO extends AbstractPeakListDAO implements DAO {
    /**
     * The sourcefile
     */
    File sourceFile;
    /**
     * The ms2-parser instance used to parse the file.
     */
    Ms2File ms2File;

    /**
     * Creates a new Ms2DAO object based on the
     * given sourceFile.
     *
     * @param sourceFile The ms2 file to parse.
     * @throws InvalidFormatException 
     */
    public Ms2DAO(File sourceFile) throws InvalidFormatException {
        this.sourceFile = sourceFile;

        try {
			ms2File = new Ms2File(sourceFile);
		} catch (JMzReaderException e) {
			throw new InvalidFormatException("Failed to open ms2 file.", e);
		}
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Collection<DAOProperty> getSupportedProperties() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<String> getSpectraIds() {
        // just return an array containing indexes 0..n-1
        ArrayList<String> ids = new ArrayList<String>(ms2File.getSpectraCount());

        for (Integer i = 0; i < ms2File.getSpectraCount(); i++)
            ids.add(i.toString());

        return ids;
    }

    @Override
    public void setConfiguration(Properties props) {
        // no configuration supported for this DAO
    }

    @Override
    public Properties getConfiguration() {
        // no configuration supported for this DAO
        return new Properties();
    }
    
    @Override
	public void setExternalSpectrumFile(String filename) {
		// not applicable to the dao
	}

    @Override
    public String getExperimentTitle() {
        // not supported
        return "";
    }

    @Override
    public String getExperimentShortLabel() {
        // check if comments are available
        return null;
    }

    @Override
    public Param getExperimentParams() {
        Param params = new Param();

        params.getCvParam().add(DAOCvParams.ORIGINAL_MS_FORMAT.getParam("ms2"));

        // add the additional values as userParams
        HashMap<String, String> header = ms2File.getHeader();

        for (String field : header.keySet()) {
            // ignore the required fields
            if ("CreationDate".equals(field) || "Extractor".equals(field) || "ExtractorVersion".equals(field) || "ExtractorOptions".equals(field))
                continue;

            // add the user param
            params.getUserParam().add(new UserParam(field, header.get(field)));
        }

        return params;
    }

    @Override
    public String getSampleName() {
        // not supported
        return null;
    }

    @Override
    public String getSampleComment() {
        // not supported
        return null;
    }

    @Override
    public Param getSampleParams() {
        // no parameters supported
        return new Param();
    }

    @Override
    public SourceFile getSourceFile() {
        // initialize the return variable
        SourceFile file = new SourceFile();

        file.setPathToFile(sourceFile.getAbsolutePath());
        file.setNameOfFile(sourceFile.getName());
        file.setFileType("ms2");

        return file;
    }

    @Override
    public Collection<Contact> getContacts() {
        // not supported
        return null;
    }

    @Override
    public InstrumentDescription getInstrument() {
        // not supported
        return null;
    }

    @Override
    public Software getSoftware() {
        // return an empty software element
        Software software = new Software();

        software.setName(ms2File.getExtractor());
        software.setVersion(ms2File.getExtractorVersion());
        software.setComments(ms2File.getExtractorOptions());

        return software;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<DatabaseMapping> getDatabaseMappings() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Param getProcessingMethod() {
        // not supported
        return null;
    }

    @Override
    public Protocol getProtocol() {
        // not supported
        return null;
    }

    @Override
    public Collection<Reference> getReferences() {
        // not supported
        return null;
    }

    @Override
    public String getSearchDatabaseName() {
        // not supported
        return "";
    }

    @Override
    public String getSearchDatabaseVersion() {
        // not supported
        return "";
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<PTM> getPTMs() {
        // not supported
        return Collections.EMPTY_LIST;
    }

    @Override
    public SearchResultIdentifier getSearchResultIdentifier() {
        // intialize the search result identifier
        SearchResultIdentifier identifier = new SearchResultIdentifier();

        // format the current time
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        identifier.setSourceFilePath(sourceFile.getAbsolutePath());
        identifier.setTimeCreated(formatter.format(new Date(System.currentTimeMillis())));
        identifier.setHash(FileUtils.MD5Hash(sourceFile.getAbsolutePath()));

        return identifier;
    }

    @Override
    public Collection<CV> getCvLookup() {
        // just create a set containing the 2 cvLookups used here
        ArrayList<CV> cvs = new ArrayList<CV>();

        cvs.add(new CV("MS", "PSI Mass Spectrometry Ontology", "1.2", "http://psidev.cvs.sourceforge.net/viewvc/*checkout*/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo"));
        cvs.add(new CV("PRIDE", "PRIDE Controlled Vocabulary", "1.101", "http://ebi-pride.googlecode.com/svn/trunk/pride-core/schema/pride_cv.obo"));

        return cvs;
    }

    @Override
    public int getSpectrumCount(boolean onlyIdentified) {
        return (onlyIdentified) ? 0 : ms2File.getSpectraCount();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Spectrum> getSpectrumIterator(boolean onlyIdentified) throws InvalidFormatException {
        return (onlyIdentified) ? Collections.EMPTY_LIST.iterator() : new Ms2DAOSpectrumIterator();
    }

    private class Ms2DAOSpectrumIterator implements Iterator<Spectrum>, Iterable<Spectrum> {
        private Iterator<Ms2Spectrum> iterator;
        private int currentSpecIndex = 1;
        
        public Ms2DAOSpectrumIterator() throws InvalidFormatException {
        	try {
				iterator = ms2File.getMs2SpectrumIterator();
			} catch (JMzReaderException e) {
				throw new InvalidFormatException(e);
			}
        }

        @Override
        public Iterator<Spectrum> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Spectrum next() {
            Spectrum s = convertMs2Spectrum(iterator.next());
            s.setId(currentSpecIndex++);

            return s;
        }

        @Override
        public void remove() {
            // this function is not supported
        }

    }

    private Spectrum convertMs2Spectrum(Ms2Spectrum ms2Spec) {
        // create the spectrum
        Spectrum spectrum = new Spectrum();

        Map<Double, Double> peakList = ms2Spec.getPeakList();
        
        // convert the peak list to the required byte arrays
        List<Double> masses;
        if (peakList != null)
        	 masses = new ArrayList<Double>(peakList.keySet());
        else
        	masses = Collections.emptyList();
        List<Double> intensities = new ArrayList<Double>();

        Collections.sort(masses);
        
        // add the intensities in the correcto order to the intensity array
        for (Double mz : masses)
        	intensities.add(peakList.get(mz));

        // create the byte arrays
        byte[] massesBytes = doubleCollectionToByteArray(masses);
        byte[] intenBytes = doubleCollectionToByteArray(intensities);

        // create the intensity array
        Data intenData = new Data();
        intenData.setEndian("little");
        intenData.setLength(intenBytes.length);
        intenData.setPrecision("64"); // doubles are 64 bit in java
        intenData.setValue(intenBytes);

        IntenArrayBinary intenArrayBin = new IntenArrayBinary();
        intenArrayBin.setData(intenData);

        // create the mass data array
        Data massData = new Data();
        massData.setEndian("little");
        massData.setLength(massesBytes.length);
        massData.setPrecision("64");
        massData.setValue(massesBytes);

        MzArrayBinary massArrayBinary = new MzArrayBinary();
        massArrayBinary.setData(massData);

        // store the mz and intensities in the spectrum
        spectrum.setIntenArrayBinary(intenArrayBin);
        spectrum.setMzArrayBinary(massArrayBinary);

        // initialize the spectrum description
        SpectrumDesc description = new SpectrumDesc();

        // create the spectrumSettings/spectrumInstrument (mzRangeStop, mzRangeStart, msLevel)
        SpectrumSettings settings = new SpectrumSettings();
        SpectrumInstrument instrument = new SpectrumInstrument();

        instrument.setMsLevel(2);

        // sort the masses to get the minimum and max
        Float rangeStart = new Float(0), rangeStop = new Float(0);
        if (masses.size() > 0) {
	        Collections.sort(masses);
	        rangeStart = new Float(masses.get(0));
	        rangeStop = new Float(masses.get(masses.size() - 1));
        }

        instrument.setMzRangeStart(rangeStart);
        instrument.setMzRangeStop(rangeStop);

        // set the spectrum settings
        settings.setSpectrumInstrument(instrument);
        description.setSpectrumSettings(settings);

        // create the precursor list
        PrecursorList precList = new PrecursorList();

        // currently, there's only one precursor supported
        precList.setCount(1);

        Precursor prec = new Precursor();
        prec.setMsLevel(1);

        Spectrum spec = new Spectrum(); // the precursor spectrum (ref)
        spec.setId(0);
        prec.setSpectrum(spec);

        uk.ac.ebi.pride.jaxb.model.Param ionSelection = new uk.ac.ebi.pride.jaxb.model.Param();

        // add the different precursor parameters if they are available
        ionSelection.getCvParam().add(DAOCvParams.PRECURSOR_MZ.getJaxbParam(ms2Spec.getPrecursorMZ()));
        if (ms2Spec.getCharges().size() > 1) {
            for (Integer c : ms2Spec.getCharges().keySet())
                ionSelection.getCvParam().add(DAOCvParams.POSSIBLE_CHARGE_STATE.getJaxbParam(c));
        } else if (ms2Spec.getCharges().size() == 1) {
            for (Integer c : ms2Spec.getCharges().keySet())
                ionSelection.getCvParam().add(DAOCvParams.CHARGE_STATE.getJaxbParam(c));
        }
        if (ms2Spec.getAdditionalInformation().get("RetTime") != null)
            ionSelection.getCvParam().add(DAOCvParams.RETENTION_TIME.getJaxbParam(ms2Spec.getAdditionalInformation().get("RetTime")));

        ionSelection.getCvParam().add(DAOCvParams.PEAK_LIST_SCANS.getJaxbParam(
                ms2Spec.getLowScan() + ((ms2Spec.getHighScan() != ms2Spec.getLowScan()) ? "-" + ms2Spec.getHighScan() : "")));

        // save the ionselection
        prec.setIonSelection(ionSelection);

        // no activation parameters supported in MGF format
        prec.setActivation(new uk.ac.ebi.pride.jaxb.model.Param());

        // add the (only) precursor to the precursor list and save it in the description item
        precList.getPrecursor().add(prec);
        description.setPrecursorList(precList);

        spectrum.setSpectrumDesc(description);

        return spectrum;
    }

    @Override
    public int getSpectrumReferenceForPeptideUID(String peptideUID) {
        throw new ConverterException("ms2 files do not support peptide identifications.");
    }

    @Override
    public Identification getIdentificationByUID(String identificationUID) {
        throw new ConverterException("ms2 files do not support peptide identifications.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Identification> getIdentificationIterator(
            boolean prescanMode) {
        // not supported
        return Collections.EMPTY_LIST.iterator();
    }
}
