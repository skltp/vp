package se.skl.tp.vp.vagvalrouter;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

public class SslProtocolSocketFactory implements ProtocolSocketFactory {

	private InputStream keystoreInputStream = null;
	private String keystorePassword = null;
	private InputStream truststoreInputStream = null;
	private String truststorePassword = null;
	private SSLContext sslcontext = null;

	private Socket socket;

	public SslProtocolSocketFactory(final InputStream keystoreInputStream,
			final String keystorePassword,
			final InputStream truststoreInputStream,
			final String truststorePassword) {
		this.keystoreInputStream = keystoreInputStream;
		this.keystorePassword = keystorePassword;
		this.truststoreInputStream = truststoreInputStream;
		this.truststorePassword = truststorePassword;
	}

	private static KeyStore createKeyStore(final InputStream is,
			final String password) throws NoSuchAlgorithmException,
			CertificateException, IOException, KeyStoreException {
		KeyStore keystore = KeyStore.getInstance("jks");
		keystore.load(is, password.toCharArray());
		return keystore;
	}

	private static KeyManager[] createKeyManagers(final KeyStore keystore,
			final String password) throws KeyStoreException,
			NoSuchAlgorithmException, UnrecoverableKeyException {
		KeyManagerFactory kmfactory = KeyManagerFactory
				.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmfactory.init(keystore, password != null ? password.toCharArray()
				: null);
		return kmfactory.getKeyManagers();
	}

	private static TrustManager[] createTrustManagers(final KeyStore keystore)
			throws KeyStoreException, NoSuchAlgorithmException {
		TrustManagerFactory tmfactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmfactory.init(keystore);
		return tmfactory.getTrustManagers();
	}

	private SSLContext createSSLContext() {
		try {
			KeyStore keystore = createKeyStore(this.keystoreInputStream,
					this.keystorePassword);
			KeyManager[] keymanagers = createKeyManagers(keystore,
					this.keystorePassword);

			KeyStore trustStore = createKeyStore(this.truststoreInputStream,
					this.truststorePassword);
			TrustManager[] trustmanagers = createTrustManagers(trustStore);

			SSLContext sslcontext = SSLContext.getInstance("TLS");
			sslcontext.init(keymanagers, trustmanagers, null);
			return sslcontext;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private SSLContext getSSLContext() {
		if (this.sslcontext == null) {
			this.sslcontext = createSSLContext();
		}
		return this.sslcontext;
	}

	public Socket getSocket() {
		return socket;
	}

	public Socket createSocket(final String host, final int port,
			final InetAddress localAddress, final int localPort,
			final HttpConnectionParams params) throws IOException,
			UnknownHostException, ConnectTimeoutException {
		if (socket != null) {
			return socket;
		}

		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null");
		}
		int timeout = params.getConnectionTimeout();
		SocketFactory socketfactory = getSSLContext().getSocketFactory();
		if (timeout == 0) {
			socket = socketfactory.createSocket(host, port, localAddress,
					localPort);
		} else {
			socket = socketfactory.createSocket();
			SocketAddress localaddr = new InetSocketAddress(localAddress,
					localPort);
			SocketAddress remoteaddr = new InetSocketAddress(host, port);
			socket.bind(localaddr);
			socket.connect(remoteaddr, timeout);
		}
		return socket;
	}

	public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException, UnknownHostException {
		socket = socket == null ?
				getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort) :
				socket; 
		return socket;
	}

	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		socket = socket == null ?
				getSSLContext().getSocketFactory().createSocket(host, port) :
				socket; 
		return socket;
	}

	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
		socket = socket == null ?
				getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose) :
				socket;
		return socket;
	}
}