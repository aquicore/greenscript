package controllers.greenscript;

import java.util.UUID;

import play.Play;
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
        final long l = System.currentTimeMillis();
        if (Play.mode == Play.Mode.PROD)  {
            response.cacheFor(etag, "100d", l);
        }
        Flash.current().keep();

        if (!request.isModified(etag, l)) {
            response.status = Http.StatusCode.NOT_MODIFIED;
            return;
        }

        if (key.endsWith(".js")) {
            response.setContentTypeIfNotSet("text/javascript");
        } else if (key.endsWith(".css")) {
            response.setContentTypeIfNotSet("text/css");
        }
        
        renderText(content);
    }

}
