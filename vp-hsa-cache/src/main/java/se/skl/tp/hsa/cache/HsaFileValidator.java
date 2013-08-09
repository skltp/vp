/**
 * Copyright (c) 2013 Sveriges Kommuner och Landsting (SKL).
 * 								<http://www.skl.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.hsa.cache;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
 * -w <arg>   warning level
 * -o <atg>   output file
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class HsaFileValidator {

	private HsaFileValidator() {
	}
	
	public static void main(String[] args) throws ParseException, FileNotFoundException {
		
		Options options = new Options();
		options.addOption("f", true, "filename");
		options.addOption("w", true, "warning level");
		options.addOption("o", true, "output file");
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		
		
		String filename    = cmd.getOptionValue("f");
		String warning     = cmd.getOptionValue("w", "-1");
		String outputFile  = cmd.getOptionValue("o");
		
		PrintWriter pw = getPrintWriter(outputFile);
		
		if(isEmpty(filename)) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "java " + HsaFileValidator.class.getName() , options );
			System.exit(-1);
		}		
		
		HsaRelationBuilder relationBuilder = new HsaRelationBuilderWithLog(pw, Integer.parseInt(warning));
		HsaFileParser fileParser = new HsaFileParserWithLog(pw);
		
		new HsaCacheImpl().init(filename, relationBuilder, fileParser);
		
		pw.close();
	}

	private static PrintWriter getPrintWriter(String outputFile) throws FileNotFoundException {
		if(isEmpty(outputFile)) {
			return new PrintWriter(System.err, true);
		} else {
			return new PrintWriter(new FileOutputStream(outputFile), true);
		}
	}
}
