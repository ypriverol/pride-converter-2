package uk.ac.ebi.pride.tools.converter.gui.component.filefilters;

import uk.ac.ebi.pride.tools.converter.utils.FileUtils;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 *
 * @author melih
 *         Date: 31/05/2011
 *         Time: 12:10
 */
public class FastaFileFilter extends FileFilter {
    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }
        String extension = FileUtils.getExtension(file);
        if (extension != null) {
            return (extension.equalsIgnoreCase(FileUtils.txt) || extension.equalsIgnoreCase(FileUtils.fasta));
        } else return false;
    }

    @Override
    public String getDescription() {
        return "Fasta Files";
    }
}
