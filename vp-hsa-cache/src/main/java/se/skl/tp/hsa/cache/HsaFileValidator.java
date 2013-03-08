/**
 * Copyright 2013 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.hsa.cache;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.PrintWriter;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class validates the XML file. The parameter warning level is used to set a 
 * distance between a node and its parent that generates a warning.
 *
 * usage: java se.skl.tp.hsa.cache.HsaFileValidator
 * -f <arg>   filename
 * -e <arg>   encoding
 * -w <arg>   warning level
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class HsaFileValidator {

	public static void main(String[] args) throws ParseException {
		
		Options options = new Options();
		options.addOption("f", true, "filename");
		options.addOption("e", true, "encoding");
		options.addOption("w", true, "warning level");
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		
		
		String filename = cmd.getOptionValue("f");
		String encoding = cmd.getOptionValue("e");
		String warning  = cmd.getOptionValue("w", "-1");
		
		if(isEmpty(filename) || isEmpty(encoding)) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "java " + HsaFileValidator.class.getName() , options );
			System.exit(-1);
		}		
		HsaCacheImpl impl = (HsaCacheImpl)new HsaCacheFactoryImpl().create(filename, encoding, Integer.parseInt(warning));
		new HsaNodePrinter(impl.getNode("SE0000000000-001T"), 3).printTree(new PrintWriter(System.out));
	}
}
