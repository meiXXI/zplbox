package io.github.meixxi.zplbox.controller.v1;

import io.github.meixxi.zplbox.controller.util.Responses;
import io.github.meixxi.zplbox.controller.v1.model.HtmlRenderingParams;
import io.github.meixxi.zplbox.service.image.ImageService;
import io.github.meixxi.zplbox.service.print.PrintService;
import io.github.meixxi.zplbox.service.render.HtmlRenderService;
import io.github.meixxi.zplbox.service.zpl.ZplService;
import io.github.meixxi.zplbox.util.Urls;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.springframework.http.MediaType.*;

@RestController
@RequestMapping("/v1/html2zpl")
public class Html2ZplController {

    private static final Logger log = LoggerFactory.getLogger(Html2ZplController.class);

    @Autowired
    private HtmlRenderService htmlRenderService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ZplService zplService;

    @Autowired
    private PrintService printService;

    @PostMapping(value = "", consumes = APPLICATION_JSON_VALUE, produces = {TEXT_PLAIN_VALUE, APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<?> createZplLabel(@RequestBody HtmlRenderingParams htmlRenderingParams, HttpServletRequest httpServletRequest) throws IOException {
        log.info("New request 'Create ZPL Label' has been received...");
        URI sourceUri = null;

        try {

            // normalize url
            sourceUri = Urls.normalize(htmlRenderingParams.getUrl(), htmlRenderingParams.getDataBase64());
            log.info("Process file '{}'...", sourceUri);

            // render web content
            BufferedImage bufferedImage = htmlRenderService.render(sourceUri, htmlRenderingParams.getWidthPts(), htmlRenderingParams.getHeightPts());
            log.info("WebContent has been rendered successfully.");

            // convert to monochrome
            bufferedImage = imageService.convertToMonochrome(bufferedImage);
            log.info("Rendered image has been converted to monochrome.");

            // create ZPL data
            String zplData = zplService.createLabel(bufferedImage);
            log.info("ZPL Data has been created.");

            // return zpl data
            return Responses.createOkResponse(zplData, TEXT_PLAIN_VALUE);

        } catch (Exception ex) {
            return Responses.createBadRequestResponse(httpServletRequest, ex);

        } finally {
            if(sourceUri != null && Objects.equals(sourceUri.getScheme(), "file")) {
                Files.delete(Path.of(sourceUri));
                log.info("File '{}' has been cleaned.", sourceUri);
            }
        }
    }

    @PostMapping(value = "/print/{tcpAddress}", consumes = APPLICATION_JSON_VALUE, produces = {APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<?> createSendZplLabel(@PathVariable String tcpAddress, @RequestBody HtmlRenderingParams htmlRenderingParams, HttpServletRequest httpServletRequest) throws IOException {
        log.info("New request 'Create and Send ZPL Label' has been received...");
        URI sourceUri = null;

        try {

            // normalize url
            sourceUri = Urls.normalize(htmlRenderingParams.getUrl(), htmlRenderingParams.getDataBase64());
            log.info("Process file '{}'...", sourceUri);

            // render web content
            BufferedImage bufferedImage = htmlRenderService.render(sourceUri, htmlRenderingParams.getWidthPts(), htmlRenderingParams.getHeightPts());
            log.info("WebContent has been rendered successfully.");

            // convert to monochrome
            bufferedImage = imageService.convertToMonochrome(bufferedImage);
            log.info("Rendered image has been converted to monochrome.");

            // create ZPL data
            String zplData = zplService.createLabel(bufferedImage);
            log.info("ZPL Data has been created.");

            // forward to tcp
            URI uri = new URI("tcp://" + tcpAddress);
            printService.printLabel(zplData, uri.getHost(), uri.getPort());

            // return zpl data
            return Responses.createOkResponse();

        } catch (Exception ex) {
            return Responses.createBadRequestResponse(httpServletRequest, ex);

        } finally {
            if(sourceUri != null && Objects.equals(sourceUri.getScheme(), "file")) {
                Files.delete(Path.of(sourceUri));
                log.info("File '{}' has been cleaned.", sourceUri);
            }
        }
    }


    @PostMapping(value = "/render", consumes = APPLICATION_JSON_VALUE, produces = {IMAGE_PNG_VALUE, APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<?> renderHtml(@RequestBody HtmlRenderingParams renderingParams, HttpServletRequest httpServletRequest) throws IOException {
        log.info("New request 'Render HTML' has been received...");
        URI sourceUri = null;

        try {
            // normalize url
            sourceUri = Urls.normalize(renderingParams.getUrl(), renderingParams.getDataBase64());
            log.info("Process file '{}'...", sourceUri);

            // render web content
            BufferedImage bufferedImage = htmlRenderService.render(sourceUri, renderingParams.getWidthPts(), renderingParams.getHeightPts());
            log.info("WebContent has been rendered successfully.");

            // create response
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", bos);
            return Responses.createOkResponse(bos.toByteArray(), IMAGE_PNG_VALUE);

        } catch (Exception ex) {
            return Responses.createBadRequestResponse(httpServletRequest, ex);

        } finally {
            if(sourceUri != null && Objects.equals(sourceUri.getScheme(), "file")) {
                Files.delete(Path.of(sourceUri));
                log.info("File '{}' has been cleaned.", sourceUri);
            }
        }
    }
}
