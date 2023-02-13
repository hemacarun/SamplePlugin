package io.antmedia.security;

import com.google.gson.JsonObject;
import io.antmedia.AppSettings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.impl.client.HttpClients;
public class CustomWebhookAuthSecurity implements  IStreamPublishSecurity {

    protected static Logger logger = LoggerFactory.getLogger(CustomWebhookAuthSecurity.class);

    @Autowired
    private AppSettings appSettings = null;
    @Override
    public  boolean isPublishAllowed(IScope scope, String streamId, String mode, Map<String, String> queryParams) {
        logger.info("###################Res");
        AtomicBoolean result = new AtomicBoolean(false);
        if (appSettings == null){
            appSettings = (AppSettings) scope.getContext().getBean(AppSettings.BEAN_NAME);
        }
        final String webhookAuthURL = appSettings.getWebhookAuthenticateURL();
        if (webhookAuthURL != null && !webhookAuthURL.isEmpty())
        {
            try (CloseableHttpClient client = getHttpClient())
            {
                JsonObject instance = new JsonObject();
                instance.addProperty("appName", scope.getName());
                instance.addProperty("name", streamId); //this is for backward compatibility for release v2.4.3
                instance.addProperty("streamId", streamId);
                instance.addProperty("roomId", streamId);
                instance.addProperty("mode", mode);
                if(queryParams != null){
                    instance.addProperty("queryParams", queryParams.toString());
                }

                RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2 * 1000).setSocketTimeout(5*1000).build();

                HttpRequestBase post = (HttpRequestBase) RequestBuilder.post().setUri(webhookAuthURL)
                        .setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .setEntity(new StringEntity(instance.toString())).build();
                post.setConfig(requestConfig);

                HttpResponse response= client.execute(post);

                int statuscode = response.getStatusLine().getStatusCode();
                logger.info("Response from webhook is: {} for stream:{}", statuscode, streamId);

                result.set(statuscode==200);

            }
            catch (Exception e) {
                logger.error("Couldn't connect Webhook for Stream Authentication {} " , ExceptionUtils.getStackTrace(e));
            }

        }
        else
        {
            logger.info("AcceptOnlyStreamsWithWebhook is not activated for stream {}", streamId);
            result.set(true);
        }


        if (!result.get()) {
            IConnection connectionLocal = Red5.getConnectionLocal();
            if (connectionLocal != null) {
                connectionLocal.close();
            }
            else {
                logger.warn("Connection object is null for {}", streamId);
            }

        }



        return false;
    }

    public AppSettings getAppSettings() {
        return appSettings;
    }
    public CloseableHttpClient getHttpClient() {
        return HttpClients.createDefault();
    }

}
