/**
 * Copyright (c) 2009-2012, Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public
 *   
 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.deployer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Creates deployment descriptors to virtualize RIV services.
 * <p>
 * 
 * RIV services are currently packaged as jar files, and this program reads the
 * WSDL definition and adds a mule deployment descriptor.
 * <p>
 * 
 * <pre>
 * usage: java -jar vp-auto-deployer[-&lt;version&gt;].jar [-update] [jar files...]
 * 		-update: force update even if the target jar already contains a descriptor.
 * </pre>
 * 
 * @author Peter
 * @since VP-2.0
 */
public class DeployerMain {

	static String MULE_TEMPLATE = "/tp-config-template-v2.xml";
	static String DEPLOY_DESCRIPTOR_NAME = "tp2-service-mule-descriptor.xml";

	//
	private String muleTemplate;

	/**
	 * Keeps xsd servicecontract information.
	 * 
	 * 
	 */
	static class XsdInfo {
		String namespace;

		XsdInfo(String namespace) {
			this.namespace = namespace;
		}

		String getNamespaceSeparatedWith(String separatedWith) {
			return namespace.replaceAll(":", separatedWith);
		}
	}

	/**
	 * Keeps riv infomration.
	 * 
	 * @author Peter
	 * 
	 */
	static class WsdlInfo {
		String name;
		String namespace;
		String wsdl;
		String version;
		String profile;
		String method;
		String path;
		String responderName;

		WsdlInfo(String name, String namespace, String wsdl, String responderName) {
			this.name = name;
			this.namespace = namespace;
			this.wsdl = wsdl;
			this.responderName = responderName;
			init();
		}

		private void init() {
			String[] args = this.namespace.split(":");
			if ((args.length < 7) || !"riv".equals(args[1])) {
				throw new IllegalArgumentException("Invalid namespace: " + namespace);
			}
			int len = args.length;
			this.method = args[len - 3];
			this.version = args[len - 2];
			this.profile = args[len - 1];
			this.path = this.method + "/" + this.version + "/" + this.profile;
		}
	}

	//
	public DeployerMain() {
		initTemplate();
	}

	//
	private DeployerMain initTemplate() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			close(copy(this.getClass().getResourceAsStream(MULE_TEMPLATE), out));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.muleTemplate = out.toString();
		return this;
	}

	//
	private static InputStream copy(InputStream in, OutputStream out) throws IOException {
		byte buf[] = new byte[1024];
		for (int len; (len = in.read(buf)) != -1;) {
			out.write(buf, 0, len);
		}
		return in;
	}

	//
	private static String getBasename(String fileName) {
		int n = fileName.lastIndexOf('.');
		if (n > 0) {
			return fileName.substring(0, n);
		}
		return fileName;
	}

	//
	private void writeDescriptor(String fileName, WsdlInfo wsdlInfo, XsdInfo xsdInfo) throws Exception {
		File src = new File(fileName);
		File tmp = new File(fileName + ".tmp");
		JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
		addToJar(jos, new JarFile(src));

		String content = String.format(this.muleTemplate, new Date(), wsdlInfo.wsdl, getBasename(src.getName()), wsdlInfo.path,
				xsdInfo.getNamespaceSeparatedWith("."), wsdlInfo.namespace, wsdlInfo.name, wsdlInfo.wsdl);

		JarEntry deployEntry = new JarEntry(DEPLOY_DESCRIPTOR_NAME);
		deployEntry.setComment("Added by vp-auto-deployer: " + wsdlInfo.path);
		jos.putNextEntry(deployEntry);
		jos.write(content.getBytes());
		jos.closeEntry();
		close(jos);

		if (!src.delete()) {
			throw new IllegalArgumentException("Unable to update jar file, permission denied");
		}
		if (!tmp.renameTo(src)) {
			throw new IllegalArgumentException(String.format("Fatal error during update, backup saved as: %s", tmp));
		}
	}

	//
	private Element parseXmlSchema(InputStream is) throws Exception {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		return docBuilderFactory.newDocumentBuilder().parse(is).getDocumentElement();
	}

	//
	static void close(Closeable s) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e) {
			}
		}
	}

	//
	private void addToJar(JarOutputStream jos, JarFile jf) throws IOException {
		for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
			JarEntry entry = e.nextElement();
			// always skip deployment descriptor (for update mode)
			if (DEPLOY_DESCRIPTOR_NAME.equals(entry.getName())) {
				continue;
			}
			jos.putNextEntry(entry);
			copy(jf.getInputStream(entry), jos);
		}
	}

	//
	private WsdlInfo extractWSDL(String wsdl, InputStream wsdlInputStream) throws Exception {
		Element element = parseXmlSchema(wsdlInputStream);
		close(wsdlInputStream);
		String namespace = element.getAttribute("targetNamespace");

		String serviceName = extractTagName(wsdl, element, "wsdl:service");
		String responderName = extractResponderNameFromServiceName(serviceName);

		System.out.println("Service name found: " + serviceName);
		System.out.println("Responder name found: " + responderName);

		WsdlInfo info = new WsdlInfo(serviceName, namespace, wsdl, responderName);

		return info;
	}

	public static String extractResponderNameFromServiceName(String serviceName) {
		// Convention is GetSubjectOfCareScheduleResponderService
		int lastIndex = serviceName.lastIndexOf("Service");
		return serviceName.substring(0, lastIndex);
	}

	private String extractTagName(String wsdl, Element element, String tagName) {
		NodeList wsdlServiceNodeList = element.getElementsByTagName(tagName);
		if (wsdlServiceNodeList.getLength() != 1) {
			throw new IllegalArgumentException(tagName + " name not found: " + wsdl);
		}

		String name = ((Element) wsdlServiceNodeList.item(0)).getAttribute("name");
		return name;
	}

	private XsdInfo extractXSD(InputStream xsdInputStream) throws Exception {
		Element element = parseXmlSchema(xsdInputStream);
		close(xsdInputStream);
		String namespace = element.getAttribute("targetNamespace");

		XsdInfo xsdInfo = new XsdInfo(namespace);

		System.out.println("XSD NS found: " + namespace);

		return xsdInfo;
	}

	//
	private JarEntry getJarEntry(JarFile jarFile, String fileNameContains, String fileExtenstion) {
		for (Enumeration<JarEntry> enumeration = jarFile.entries(); enumeration.hasMoreElements();) {
			JarEntry entry = enumeration.nextElement();
			if (entry.getName().contains(fileNameContains) && entry.getName().endsWith(fileExtenstion)) {
				return entry;
			}
		}
		throw new IllegalArgumentException(String.format("Invalid jar file \"%s\", no file containing name "
				+ fileNameContains + " found!", jarFile.getName()));
	}

	/**
	 * Creates a mule deployment descriptor for RIV-TA service packed in a jar
	 * file.
	 * 
	 * @param fileName
	 *            the jar file name with service WSDL definition.
	 * @param update
	 *            true if existing entries shall be updated, otherwise false.
	 * 
	 * @return instance.
	 * 
	 * @throws Exception
	 *             on any kind of unexpected error.
	 */
	public DeployerMain deploy(String fileName, boolean update) throws Exception {
		JarFile jarFile = new JarFile(fileName);

		JarEntry wsdlEntry = getJarEntry(jarFile, "", ".wsdl");
		WsdlInfo wsdlInfo = extractWSDL(wsdlEntry.getName(), jarFile.getInputStream(wsdlEntry));

		JarEntry xsdlEntry = getJarEntry(jarFile, wsdlInfo.responderName, ".xsd");
		XsdInfo xsdInfo = extractXSD(jarFile.getInputStream(xsdlEntry));

		JarEntry deployEntry = jarFile.getJarEntry(DEPLOY_DESCRIPTOR_NAME);

		if (deployEntry == null || update) {
			System.out.printf("Updating %s with deployment descriptor (%s)\n", fileName, DEPLOY_DESCRIPTOR_NAME);
			writeDescriptor(fileName, wsdlInfo, xsdInfo);
		}
		return this;
	}

	/**
	 * Prints usage information, and exits with exit code 1.
	 */
	public static void usage() {
		System.out.println("vp-auto-deployer: Creates a VP R2 deployment descriptor to virtualize RIV services");
		System.out.println("Be careful: for RIV standard virtualisations only, and also be aware of the fact that all");
		System.out.println("input jars are modified by this utility program.");
		System.out.println("\nusage: java -jar vp-auto-deployer[-<version>].jar [-update] [jar files...]");
		System.out.printf("\t-update: force update of existing deployment descriptors (%s).\n", DEPLOY_DESCRIPTOR_NAME);
		System.exit(1);
	}

	/**
	 * Creates a mule deployment descriptor for all specified RIV-TA service
	 * packed jar files.
	 * 
	 * @param args
	 *            program arguments.
	 * 
	 * @see #usage()
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
				System.err.printf("Warning: unexpected error while processing file: %s - %s (skipped)\n", args[i], e);
			}
		}

		System.exit(0);
	}

}
