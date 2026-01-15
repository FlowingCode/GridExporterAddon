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
package com.flowingcode.vaadin.addons.gridexporter;

import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import java.io.IOException;

/**
 * Adapter class that converts a {@link StreamResourceWriter} to a
 * {@link DownloadHandler}.
 * This allows gradual migration from the deprecated StreamResource API to the
 * new DownloadHandler
 * API while maintaining backward compatibility.
 *
 * @author Javier Godoy
 */
@SuppressWarnings("serial")
class StreamResourceWriterAdapter implements DownloadHandler {

    private final StreamResourceWriter writer;
    private final String filename;
    private final String contentType;

    /**
     * Creates a new adapter that wraps the given {@link StreamResourceWriter}.
     *
     * @param writer      the StreamResourceWriter to adapt
     * @param filename    the filename for the download
     * @param contentType the MIME content type
     */
    public StreamResourceWriterAdapter(StreamResourceWriter writer, String filename, String contentType) {
        this.writer = writer;
        this.filename = filename;
        this.contentType = contentType;
    }

    @Override
    public void handleDownloadRequest(DownloadEvent event) throws IOException {
        // Set filename and content type in the download event
        if (filename != null) {
            event.setFileName(filename);
        }
        if (contentType != null) {
            event.setContentType(contentType);
        }

        // Delegate to the StreamResourceWriter's accept method
        writer.accept(event.getOutputStream(), event.getSession());
    }
}
