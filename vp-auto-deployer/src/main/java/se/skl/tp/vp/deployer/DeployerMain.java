package se.skl.tp.vp.deployer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Creates deployment descriptors to virtualized services.
 * 
 * @author Peter
 */
public class DeployerMain {

	static String MULE_TEMPLATE = "/tp-config-template-v2.xml";
	static String DEPLOY_DESCRIPTOR_TEMPLATE = "tp2-service-mule-descriptor-riv%d.xml";

	//
	private String muleTemplate;
	private JarOutputStream out;
	private int current = 1;
	private Set<String> set = new HashSet<String>();

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
	private DeployerMain initOutJar(File outFile) throws Exception {
		if (outFile != null) {
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
			this.out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
		} else {
			this.out = null;
		}
		return this;
	}

	//
	public DeployerMain open(File outFile) throws Exception {
		return initTemplate().initOutJar(outFile);
	}

	//
	public DeployerMain close() throws IOException {
		if (out != null) {
			out.close();
			out = null;
		}
		return this;
	}

	//
	private void deploy(Info info) throws Exception {
		if (set.contains(info.path)) {
			System.out.printf("Warning: Descriptor already added for %s (skipped)\n", info.path);
			return;
		}
		String content = String.format(this.muleTemplate, 
				new Date(), 
				info.wsdl,
				info.contract, 
				info.profile, 
				info.path,
				info.namespace,
				info.name,
				info.wsdl);

		if (out != null) {
			JarEntry entry = new JarEntry(String.format(DEPLOY_DESCRIPTOR_TEMPLATE, current++));
			entry.setComment(info.path);
			out.putNextEntry(entry);
			out.write(content.getBytes());
			out.closeEntry();
			set.add(info.path);
		} else {
			System.out.println(content);
		}
	}

	//
	private Element parseWSDL(InputStream is) throws Exception {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		return docBuilderFactory.newDocumentBuilder().parse(is).getDocumentElement();
	}

	//
	private Info extractWSDL(String wsdl, InputStream is) throws Exception {
		Element element = parseWSDL(is);
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
	public DeployerMain prepare(String fileName) throws Exception {
		InputStream is;
		String name;
		if (fileName.endsWith(".jar")) {
			JarFile jarFile = new JarFile(fileName);
			JarEntry entry = getWSDLJarEntry(jarFile);
			is = jarFile.getInputStream(entry);
			name = entry.getName();
		} else if (fileName.endsWith(".wsdl")) {
			is = new FileInputStream(fileName);
			name = fileName;
		} else {
			throw new IllegalArgumentException("Invalid input file \"%s\", .jar or .wsdl expected");
		}
		try {
			Info info = extractWSDL(name, is);
			deploy(info);	
		} finally {
			is.close();
		}
		return this;
	}

	//
	static void usage() {
		System.out.println("usage: java -jar vp-auto-deployer.jar [-out <out-jar>] [-overwrite] [jar with wsdl entry, or wsdl files...]");
		System.out.println("\tIf no out jar file is specified the descriptors are written to stdout.");
		System.out.println("\tUse -overwrite option to overwrite output jar files.");
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
		String name = null;;
		boolean overwrite = false;
		int n = 0;
		for (; n < args.length; ) {
			if ("-out".equals(args[n])) {
				n++;
				name = args[n++];
			} else if ("-overwrite".equals(args[n])) {
				overwrite = true;
				n++;
			} else {
				break;
			}
		}

		File outFileTmp = null;
		File outFile = null;
		if (name != null) {
			outFileTmp = new File(name + ".tmp");
			outFile = new File(name);
			if (outFile.exists() && !overwrite) {
				System.err.printf("Error: target jar file \"%s\" already exists", name);
				usage();
			}	
		}
		DeployerMain deployer = new DeployerMain();
		try {
			deployer.open(outFileTmp);
		} catch (Exception e) {
			System.err.printf("Error: unable to initialize (%s)\n", e);
			System.exit(1);
		}

		try {
			for (int i = n; i < args.length; i++) {
				deployer.prepare(args[i]);
			}
			deployer.close();
			if (outFile != null) {
				if (outFile.exists()) {
					File old = new File(name + ".old");
					if (old.exists()) {
						old.delete();
					}
					if (!outFile.renameTo(old)) {
						throw new IllegalStateException("Error: unable to save backup of current deployment jar file: " + old);
					}
					outFile = new File(name);
				}
				if (!outFileTmp.renameTo(outFile)) {
					throw new IllegalStateException("Error: unable to create outfile: " + outFile);				
				}
			}
		} catch (Exception e) {
			System.err.printf("Error: unexpected error while processing input (%s)\n", e);
			System.exit(1);
		}

		if (outFile != null) {
			System.out.printf("Descriptor jar file \"%s\" successfully created\n", outFile);
		}

		System.exit(0);
	}

}
