/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.cli.client.command;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.obiba.magma.NoSuchDatasourceException;
import org.obiba.magma.support.MagmaEngineFactory;
import org.obiba.opal.cli.client.command.options.ImportCommandOptions;
import org.obiba.opal.core.cfg.OpalConfiguration;
import org.obiba.opal.core.service.ImportService;

public class ImportCommand extends AbstractContextLoadingCommand<ImportCommandOptions> {

  //
  // AbstractContextLoadingCommand Methods
  //

  @Override
  public void executeWithContext() {
    if(options.isFiles()) {
      importFiles();
    } else {
      System.err.println("No input (specify one or more directories of files to import");
    }
  }

  //
  // Methods
  //

  private void importFiles() {
    List<File> filesToImport = resolveFiles();

    if(!filesToImport.isEmpty()) {
      OpalConfiguration opalConfiguration = getBean("opalConfiguration");
      ImportService importService = getBean("importService");

      String destination = options.getDatasource();

      for(File file : filesToImport) {
        System.out.println("Importing " + file.getPath() + "...");

        try {
          // Instantiate a MagmaEngine using the configured factory.
          MagmaEngineFactory magmaEngineFactory = opalConfiguration.getMagmaEngineFactory();
          magmaEngineFactory.create();

          importService.importData(destination, file);
        } catch(NoSuchDatasourceException ex) {
          // Fatal exception - break out of here.
          System.err.println(ex.getMessage());
          break;
        } catch(IllegalArgumentException ex) {
          // Shouldn't never get here -- method resolveFiles() returns only valid files.
          System.err.println(ex.getMessage());
          continue;
        } catch(IOException ex) {
          // Possibly a non-fatal exception - report an error and continue with the next file.
          System.err.println("I/O error: " + ex.getMessage());
          continue;
        }
      }
    } else {
      System.out.println("No files found");
    }
  }

  private List<File> resolveFiles() {
    List<File> files = new ArrayList<File>();

    for(File file : options.getFiles()) {
      if(file.isDirectory()) {
        File[] filesInDir = file.listFiles(new FileFilter() {
          public boolean accept(File dirFile) {
            return dirFile.isFile() && dirFile.getName().toLowerCase().endsWith(".zip");
          }
        });
        files.addAll(Arrays.asList(filesInDir));
      } else if(file.isFile()) {
        files.add(file);
      }
    }

    return files;
  }
}
