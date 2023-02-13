package io.antmedia.plugin;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonObject;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.security.CustomWebhookAuthSecurity;
import io.antmedia.security.CustomWebhookAuthSecurity;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.IStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.app.SampleFrameListener;
import io.antmedia.app.SamplePacketListener;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.plugin.api.IFrameListener;
import io.antmedia.plugin.api.IStreamListener;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.vertx.core.Vertx;

@Component(value="plugin.myplugin")
public class SamplePlugin implements ApplicationContextAware, IStreamListener {

	public static final String BEAN_NAME = "web.handler";
	protected static Logger logger = LoggerFactory.getLogger(SamplePlugin.class);

	private Vertx vertx;
	private SampleFrameListener frameListener = new SampleFrameListener();
	private SamplePacketListener packetListener = new SamplePacketListener();
	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		vertx = (Vertx) applicationContext.getBean("vertxCore");

		AntMediaApplicationAdapter app = getApplication();
		List<IStreamPublishSecurity> securityList = new ArrayList<>();
		securityList.add(new CustomWebhookAuthSecurity());
		//securityList.add(new CustomPublishSecurity2());
		//securityList.add(new CustomPublishSecurity3());
		app.setStreamPublishSecurityList(securityList);
		app.addStreamListener(this);
	}

	public MuxAdaptor getMuxAdaptor(String streamId)
	{
		AntMediaApplicationAdapter application = getApplication();
		MuxAdaptor selectedMuxAdaptor = null;

		if(application != null)
		{
			List<MuxAdaptor> muxAdaptors = application.getMuxAdaptors();
			for (MuxAdaptor muxAdaptor : muxAdaptors)
			{
				if (streamId.equals(muxAdaptor.getStreamId()))
				{
					selectedMuxAdaptor = muxAdaptor;
					break;
				}
			}
		}

		return selectedMuxAdaptor;
	}

	public void register(String streamId) {
		AntMediaApplicationAdapter app = getApplication();
		logger.info("*************** Stream registered: {} ***************", streamId);
		app.addFrameListener(streamId, frameListener);
		app.addPacketListener(streamId, packetListener);
		List<IStreamPublishSecurity> securityList = new ArrayList<>();
		securityList.add(new CustomWebhookAuthSecurity());
		//securityList.add(new CustomPublishSecurity2());
		//securityList.add(new CustomPublishSecurity3());
		app.setStreamPublishSecurityList(securityList);
		logger.info("*************** Stream registered: {} ***************", streamId);
	}

	public AntMediaApplicationAdapter getApplication() {
		logger.info("hema3", (AntMediaApplicationAdapter) applicationContext.getBean(AntMediaApplicationAdapter.BEAN_NAME));
		return (AntMediaApplicationAdapter) applicationContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
	}

	public IFrameListener createCustomBroadcast(String streamId) {
		AntMediaApplicationAdapter app = getApplication();
		return app.createCustomBroadcast(streamId);
	}

	public String getStats() {
		return frameListener.getStats() + "\t" + packetListener.getStats();
	}


	@Override
	public void streamStarted(String streamId) {
		logger.info("*************** Stream Started: {} ***************", streamId);

		logger.info("*************** donee: {} ***************", streamId);
	}

	@Override
	public void streamFinished(String streamId) {
		logger.info("*************** Stream Finished: {} ***************", streamId);
	}

	@Override
	public void joinedTheRoom(String roomId, String streamId) {
		logger.info("*************** Stream Id:{} joined the room:{} ***************", streamId, roomId);
		DataStore datastore = getApplication().getDataStore();
		ConferenceRoom room = datastore.getConferenceRoom(roomId);

		List<String> roomStreamList = null;
		roomStreamList = room.getRoomStreamList();

		// This is for the Conference Room list
		if(roomStreamList.contains(streamId)) {
			roomStreamList.remove(streamId);
			getApplication().stopPublish(streamId);
			datastore.delete(streamId);

		//	getApplication().leftTheRoom(roomId, streamId);
			logger.info("* inside the condition", room.toString());

		}
		logger.info("***************************************", room.toString());
	}



	@Override
	public void leftTheRoom(String roomId, String streamId) {
		logger.info("*************** Stream Id:{} left the room:{} ***************", streamId, roomId);
	}




}