package io.github.meixxi.zplbox.service.render;

import java.awt.image.BufferedImage;
import java.net.URI;

public interface PdfRenderService {

    /**
     * Rendering a given pdf document.
     * @param sourceUri The pdf source to be rendered.
     * @param dotsPerInch The resolution in dots per inch (dpi).
     * @return The rendered pdf content as buffered image.
     */
    BufferedImage render(URI sourceUri, int dotsPerInch) throws Exception;
}
