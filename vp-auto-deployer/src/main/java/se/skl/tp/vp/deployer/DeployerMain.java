package se.skl.tp.vp.deployer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Creates deployment descriptors to virtualize RIV services. <p>
 * 
 * RIV services are currently packaged as jar files, and this program reads the WSDL definition and adds a
 * mule deployment descriptor. <p>
 * 
 * <code>
 * usage: java -jar vp-auto-deployer[-<version>]jar [-update] [jar files...]
 * 		-update: force update even if the target jar already contains a descriptor.
 * </code>
 * 
 * @author Peter
 */
public class DeployerMain {

	static String MULE_TEMPLATE = "/tp-config-template-v2.xml";
	static String DEPLOY_DESCRIPTOR_NAME = "tp2-service-mule-descriptor.xml";

	//
	private String muleTemplate;

	/**
	 * Keeps riv infomration.
	 * 
	 * @author Peter
	 *
	 */
	static class Info {
		String name;
		String namespace;
		String wsdl;
		String version;
		String profile;
		String contract;
		String path;

		Info(String name, String namespace, String wsdl) {
			this.name = name;
			this.namespace = namespace;
			this.wsdl = wsdl;
			init();
		}

		private void init() {
			String[] args = this.namespace.split(":");
			if ((args.length < 7) || !"riv".equals(args[1])) {
				throw new IllegalArgumentException("Invalid namespace: " + namespace);
			}
			int len = args.length;
			this.contract = args[len-3];
			this.version = args[len-2];
			this.profile = args[len-1];
			this.path = this.contract + "/" + this.version + "/" + this.profile;
		}
	}

	//
	public DeployerMain() {
		initTemplate();
	}


	//
	private DeployerMain initTemplate() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		InputStream is = this.getClass().getResourceAsStream(MULE_TEMPLATE);
		try {
			for (int b; (b = is.read()) != -1; ) {
				os.write(b);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.muleTemplate = os.toString();		
		return this;
	}



	//
	private void writeDescriptor(String fileName, Info info) throws Exception {
		File src = new File(fileName);
		File tmp = new File(fileName + ".tmp");
		JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
		addToJar(jos, new JarFile(src));
		
		String content = String.format(this.muleTemplate, 
				new Date(), 
				info.wsdl,
				info.contract, 
				info.profile, 
				info.path,
				info.namespace,
				info.name,
				info.wsdl);

		JarEntry deployEntry = new JarEntry(DEPLOY_DESCRIPTOR_NAME);
		deployEntry.setComment("Added by vp-auto-deployer: " + info.path);
		jos.putNextEntry(deployEntry);
		jos.write(content.getBytes());
		jos.closeEntry();
		close(jos);
		
		if (!src.delete()) {
			throw new IllegalArgumentException("Unable to update jar file, permission denied");
		}
		if (!tmp.renameTo(src)) {
			throw new IllegalArgumentException(String.format("Fatal error during update, check %s copy", tmp));			
		}
	}

	//
	private Element parseWSDL(InputStream is) throws Exception {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		return docBuilderFactory.newDocumentBuilder().parse(is).getDocumentElement();
	}

	//
	static void close(Closeable s) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e) {}		
		}
	}
	
	//
	private void addToJar(JarOutputStream jos, JarFile jf) throws IOException {
		for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements(); ) {
			JarEntry entry = e.nextElement();
			// always skip deployment descriptor (for update mode)
			if (DEPLOY_DESCRIPTOR_NAME.equals(entry.getName())) {
				continue;
			}
			jos.putNextEntry(entry);
			InputStream in = jf.getInputStream(entry);
			byte buf[] = new byte[1024];
			for (int len; (len = in.read(buf)) != -1; ) {
				jos.write(buf, 0, len);
			}
		}
	}

	//
	private Info extractWSDL(String wsdl, InputStream is) throws Exception {
		Element element = parseWSDL(is);
		close(is);
		String namespace = element.getAttribute("targetNamespace");		
		NodeList nodeList = element.getElementsByTagName("wsdl:service");
		if (nodeList.getLength() != 1) {
			throw new IllegalArgumentException("Service name not found: " + wsdl);
		}

		String name = ((Element)nodeList.item(0)).getAttribute("name");

		Info info = new Info(name, namespace, wsdl);

		return info;
	}

	//
	private JarEntry getWSDLJarEntry(JarFile jarFile) {
		for (Enumeration<JarEntry> enumeration = jarFile.entries(); enumeration.hasMoreElements(); ) {
			JarEntry entry = enumeration.nextElement();
			if (entry.getName().endsWith(".wsdl")) {
				return entry;
			}
		}
		throw new IllegalArgumentException(String.format("Invalid jar file \"%s\", no WSDL entry found!", jarFile.getName()));
	}


	//
	public DeployerMain deploy(String fileName, boolean update) throws Exception {
		JarFile jarFile = new JarFile(fileName);
		JarEntry wsdlEntry = getWSDLJarEntry(jarFile);
		Info info = extractWSDL(wsdlEntry.getName(), jarFile.getInputStream(wsdlEntry));
		JarEntry deployEntry = jarFile.getJarEntry(DEPLOY_DESCRIPTOR_NAME);
		if (deployEntry == null || update) {
			System.out.printf("Updating %s with deployment descriptor (%s)\n", fileName, DEPLOY_DESCRIPTOR_NAME);
			writeDescriptor(fileName, info);
		}
		return this;
	}

	//
	static void usage() {
		System.out.println("usage: java -jar vp-auto-deployer.jar [-update] [jar files...]");
		System.out.printf("\t-update: force update of existing deployment descriptors (%s).\n", DEPLOY_DESCRIPTOR_NAME);
		System.exit(1);		
	}

	/**
	 * Main program.
	 * 
	 * @param args program arguments.s
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			usage();
		}
		boolean update = false;
		int n = 0;
		if ("-update".equals(args[n])) {
			update = true;
			n++;
		}
		DeployerMain deployer = new DeployerMain();

		for (int i = n; i < args.length; i++) {
			try {
				if (!args[i].endsWith(".jar")) {
					throw new IllegalArgumentException("Invalid input file, .jar file extension expected");
				}
				deployer.deploy(args[i], update);
			} catch (Exception e) {
				System.err.printf("Error: unexpected error while processing file: %s - %s (skipped)\n", args[i], e);
				System.exit(1);
			}
		}

		System.exit(0);
	}

}
