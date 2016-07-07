package org.endeavourhealth.sftpreader;

import com.google.common.io.Resources;
import com.jcraft.jsch.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.endeavourhealth.core.configuration.SftpReaderConfiguration;
import org.endeavourhealth.core.messaging.exchange.Exchange;
import org.endeavourhealth.core.messaging.pipeline.PipelineProcessor;
import org.endeavourhealth.core.utility.FileHelper;
import org.slf4j.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TimerTask;
import java.util.Vector;

public class SftpConsumer extends TimerTask {
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SftpConsumer.class);

	private PipelineProcessor pipeline;
	private SftpReaderConfiguration configuration;
	private JSch jSch;

	public SftpConsumer(SftpReaderConfiguration configuration) {
		this.configuration = configuration;
		this.pipeline = new PipelineProcessor(configuration.getPipeline());
		this.jSch = new JSch();
	}

	@Override
	public void run() {
		Session session = null;
		Channel channel = null;

		try {
			session = getSession();
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp channelSftp = (ChannelSftp)channel;
			channelSftp.cd(configuration.getPath());
			Vector fileList = channelSftp.ls(configuration.getFilename());
			for (int i = 0; i<fileList.size(); i++) {
				ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)fileList.get(i);
				InputStream stream = channelSftp.get(entry.getFilename());
				String messageData = IOUtils.toString(stream);
				Exchange exchange = new Exchange(messageData);
				pipeline.execute(exchange);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		} finally {
			if (channel != null && channel.isConnected())
				channel.disconnect();
			if (session != null && session.isConnected())
				session.disconnect();
		}
	}

	private Session getSession() throws JSchException, IOException
	{
		jSch.addIdentity(getClientPrivateKeyPath());

		jSch.setKnownHosts(new ByteArrayInputStream(createKnownHostsString().getBytes()));

		Session session = jSch.getSession(
				configuration.getCredentials().getUsername(),
				configuration.getHost(),
				configuration.getPort());

		return session;
	}

	public String createKnownHostsString() throws IOException
	{
		String hostPublicKey = FileUtils.readFileToString(new File(getHostPublicKeyPath()));
		return configuration.getHost() + " " + hostPublicKey;
	}

	private String getHostPublicKeyPath()
	{
		return resolveFilePath(configuration.getCredentials().getHostPublicKeyFilePath());
	}

	private String getClientPrivateKeyPath() throws IOException
	{
		return resolveFilePath(configuration.getCredentials().getClientPrivateKeyFilePath());
	}

	private String resolveFilePath(String filePath)
	{
		if (!Files.exists(Paths.get(filePath)))
			return Resources.getResource(filePath).getPath();

		return filePath;
	}
}
