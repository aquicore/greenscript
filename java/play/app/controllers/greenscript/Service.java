package controllers.greenscript;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import play.Play;
import play.libs.Time;
import play.modules.greenscript.GreenScriptPlugin;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Scope.Flash;

public class Service extends Controller {
    
    public static void getInMemoryCache(String key) {
        String content = GreenScriptPlugin.getInstance().getInMemoryFileContent(key, params.get(GreenScriptPlugin.RESOURCES_PARAM));
        notFoundIfNull(content);

        String etag;
        if (GreenScriptPlugin.getInstance().getMinimizerConfig().getProperty("greenscript.minimize", "false").equals("true")) {
            etag = key;
        } else {
            etag = UUID.nameUUIDFromBytes(content.getBytes()).toString();
        }
        if (Play.mode == Play.Mode.PROD)  {
            cacheFor(etag, "100d");
        }
        Flash.current().keep();

        if (!isModified(etag)) {
            response.status = Http.StatusCode.NOT_MODIFIED;
            return;
        }


        final ByteArrayOutputStream gzip;
        try {
            gzip = getGzipStream(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        response.out = gzip;
        response.setHeader("Content-Length", gzip.size() + "");
        response.setHeader("Content-Encoding", "gzip");

        if (key.endsWith(".js")) {
            response.setContentTypeIfNotSet("text/javascript");
        } else if (key.endsWith(".css")) {
            response.setContentTypeIfNotSet("text/css");
        }
    }


    /**
     * Add cache-control headers
     * @param duration Ex: 3h
     */
    private static void cacheFor(String etag, String duration) {
        int maxAge = Time.parseDuration(duration);
        response.setHeader("Cache-Control", "max-age=" + maxAge);
        response.setHeader("Etag", etag);
    }

    private static boolean isModified(String etag) {
        if (!(request.headers.containsKey("if-none-match"))) {
            return true;
        } else {
            String browserEtag = request.headers.get("if-none-match").value();
            return (!browserEtag.equals(etag));
        }
    }

    private static ByteArrayOutputStream getGzipStream(final String input) throws IOException {
        final InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        final ByteArrayOutputStream stringOutputStream = new ByteArrayOutputStream(input.length());
        final OutputStream gzipOutputStream = new GZIPOutputStream(stringOutputStream);
        final byte[] buf = new byte[5000];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            gzipOutputStream.write(buf, 0, len);
        }
        inputStream.close();
        gzipOutputStream.close();
        return stringOutputStream;
    }

}
