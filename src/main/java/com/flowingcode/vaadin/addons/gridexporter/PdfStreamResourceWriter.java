/*-
 * #%L
 * Grid Exporter Add-on
 * %%
 * Copyright (C) 2022 - 2024 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/** */
package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.server.VaadinSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mlope
 */
@SuppressWarnings("serial")
class PdfStreamResourceWriter<T> extends DocxStreamResourceWriter<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PdfStreamResourceWriter.class);

  public PdfStreamResourceWriter(GridExporter<T> exporter, String template) {
    super(exporter, template);
  }

  @Override
  public void accept(OutputStream out, VaadinSession session) throws IOException {
    try {

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      createDoc(session).write(baos);

      WordprocessingMLPackage wordMLPackage =
                        WordprocessingMLPackage.load(new ByteArrayInputStream(baos.toByteArray()));
                    MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();

                    Docx4J.toPDF(wordMLPackage, out);
    } catch (IOException e) {
      LOGGER.error("Problem generating export", e);
    } catch (Docx4JException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
