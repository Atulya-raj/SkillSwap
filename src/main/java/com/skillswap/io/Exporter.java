package com.skillswap.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Generic interface for exporting lists of domain entities to an output stream.
 *
 * @param <T> domain entity type
 */
public interface Exporter<T> {

    /**
     * Exports a list of items to the destination output stream.
     *
     * @param items list of domain items
     * @param out output stream target
     * @throws IOException on I/O error
     */
    void export(List<T> items, OutputStream out) throws IOException;
}
