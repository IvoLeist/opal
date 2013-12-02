/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.shell.commands;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.obiba.magma.Datasource;
import org.obiba.magma.ValueSet;
import org.obiba.magma.ValueTable;
import org.obiba.magma.ValueTableWriter;
import org.obiba.magma.ValueTableWriter.ValueSetWriter;
import org.obiba.magma.datasource.crypt.DatasourceEncryptionStrategy;
import org.obiba.magma.datasource.crypt.EncryptedSecretKeyDatasourceEncryptionStrategy;
import org.obiba.magma.datasource.fs.FsDatasource;
import org.obiba.magma.support.DatasourceCopier;
import org.obiba.opal.core.service.UnitKeyStoreService;
import org.obiba.opal.core.unit.FunctionalUnit;
import org.obiba.opal.core.unit.FunctionalUnitService;
import org.obiba.opal.shell.commands.options.SplitCommandOptions;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
@CommandUsage(description = "Splits one or more ZIP files into multiple pieces.",
    syntax = "Syntax: split --unit unit --out DIR _FILE_...")
public class SplitCommand extends AbstractOpalRuntimeDependentCommand<SplitCommandOptions> {

  @Autowired
  private FunctionalUnitService functionalUnitService;

  @Autowired
  private UnitKeyStoreService unitKeyStoreService;

  @Override
  public int execute() {
    // Ensure that options have been set.
    if(options.getFiles() == null) {
      getShell().printf("No input file(s) specified.\n");
      return 1; // error!
    }
    try {
      innerExecute();
      return 0; // success!
    } catch(FileSystemException e) {
      // We can't handle it
      throw new RuntimeException(e);
    }
  }

  private void innerExecute() throws FileSystemException {

    FunctionalUnit unit = functionalUnitService.getFunctionalUnit(options.getUnit());
    if(unit == null) {
      getShell().printf("Functional unit '%s' does not exist.\n", options.getUnit());
      return;
    }

    FileObject unitDir = getFunctionalUnitService().getUnitDirectory(options.getUnit());
    FileObject outputDir = getOutputDir(unitDir);
    if(outputDir == null) {
      return;
    }

    for(String inputName : options.getFiles()) {
      FileObject inputFile = resolveFile(inputName, unitDir);
      if(inputFile.exists()) {
        getShell().printf("Splitting input file %s in chunks of %d entities\n", inputFile.getName().getPath(),
            options.getChunkSize());
        processFile(unit, inputFile, outputDir);
      } else {
        getShell().printf("Skipping non-existant input file %s\n", inputFile.getName());
      }
    }
  }

  @Nullable
  private FileObject getOutputDir(FileObject unitDir) throws FileSystemException {
    FileObject outputDir = resolveFile(options.getOutput(), unitDir);
    if(!outputDir.exists()) {
      try {
        outputDir.createFolder();
      } catch(FileSystemException e) {
        getShell().printf("Could not create output directory: %s\n", outputDir.getName().getPath());
        return null;
      }
    } else if(outputDir.getType() != FileType.FOLDER) {
      getShell().printf("Specified output '%s' is not a directory.\n", outputDir.getName().getPath());
      return null;
    }
    return outputDir;
  }

  private FileObject resolveFile(String filename, FileObject unitDir) throws FileSystemException {
    return filename.startsWith("/") ? getFileSystemRoot().resolveFile(filename) : unitDir.resolveFile(filename);
  }

  private void processFile(FunctionalUnit unit, FileObject inputFile, FileObject outputDir) {
    DatasourceSplitter f = new DatasourceSplitter(unit, inputFile, outputDir);
    try {
      f.split();
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  private class DatasourceSplitter {

    private final FileObject outputDir;

    private final String filenameFormat;

    private final Datasource inputDatasource;

    private final DatasourceCopier dataCopier;

    private int currentChunk = 0;

    private int index = 1;

    private Datasource destination;

    private ValueTableWriter writer;

    private DatasourceSplitter(FunctionalUnit unit, FileObject bigFile, FileObject outputDir) {
      this.outputDir = outputDir;
      // /path/file.zip --> file.zip
      String basename = bigFile.getName().getBaseName();
      // /path/file.zip --> zip
      String extension = bigFile.getName().getExtension();
      // Output filename format is '{inputname}-{index}.zip' where {inputname} is '{basename}' without '.{extension}'
      filenameFormat = basename.replaceAll("\\." + extension + "$", "") + "-%s.zip";

      dataCopier = DatasourceCopier.Builder.newCopier().dontCopyMetadata().dontCopyNullValues().withLoggingListener()
          .withThroughtputListener().build();
      File localInputFile = getOpalRuntime().getFileSystem().getLocalFile(bigFile);

      if(unit.getDatasourceEncryptionStrategy() == null) {
        DatasourceEncryptionStrategy encryptionStrategy = new EncryptedSecretKeyDatasourceEncryptionStrategy();
        encryptionStrategy.setKeyProvider(unitKeyStoreService.getKeyStore(unit.getName()));
        unit.setDatasourceEncryptionStrategy(encryptionStrategy);
      }

      inputDatasource = new FsDatasource(bigFile.getName().getBaseName(), localInputFile,
          unit.getDatasourceEncryptionStrategy());
    }

    public void split() throws IOException {
      inputDatasource.initialise();
      try {
        nextDestination();
        for(ValueTable source : inputDatasource.getValueTables()) {
          prepareDestination(source);
          for(ValueSet valueSet : source.getValueSets()) {
            ValueSetWriter valueSetWriter = writer.writeValueSet(valueSet.getVariableEntity());
            dataCopier.copyValues(source, valueSet, source.getName(), valueSetWriter);
            close(valueSetWriter);
            checkSplitBoundary(source);
          }
        }
        destination.dispose();
      } finally {
        inputDatasource.dispose();
      }
    }

    private void nextDestination() {
      try {
        FileObject newFile = outputDir.resolveFile(String.format(filenameFormat, index++));
        File localOutputFile = getOpalRuntime().getFileSystem().getLocalFile(newFile);
        destination = new FsDatasource(newFile.getName().getBaseName(), localOutputFile);
        destination.initialise();
        getShell().printf("  Writing to %s\n", newFile.getName().getPath());
      } catch(FileSystemException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Initializes the destination for the specified {@code ValueTable}. This will create a new instance of
     * {@code ValueTableWriter} and copy the variables to this new writer.
     *
     * @param source
     * @throws IOException
     */
    private void prepareDestination(ValueTable source) throws IOException {
      writer = dataCopier.createValueTableWriter(source, source.getName(), destination);
      DatasourceCopier.Builder.newCopier().dontCopyValues().build().copyMetadata(source, source.getName(), writer);
    }

    /**
     * Tests the split boundary. If we've hit the boundary, this method will close the current output datasource, create
     * a new one and prepare it using the {@code #prepareDestination(ValueTable)} method.
     *
     * @param source
     * @throws IOException
     */
    private void checkSplitBoundary(ValueTable source) throws IOException {
      // Increment the chunk counter and test boundary.
      // Split if we've written enough value sets.
      if(++currentChunk >= options.getChunkSize()) {
        // Split boundary
        currentChunk = 0;
        close(writer);
        destination.dispose();

        nextDestination();
        prepareDestination(source);
      }
    }

    private void close(Closeable vsw) {
      try {
        vsw.close();
      } catch(IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
