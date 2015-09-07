def zipDir = "zipDir"
def tempDir = "tempDir"
def newVirtDir = "new-vp-services"
def descriptorFile = "tp2-service-mule-descriptor.xml"
def oldVirtDir=new File("old-vp-services")

def jarFiles = []
oldVirtDir.eachFileRecurse(groovy.io.FileType.FILES){ 
	if(it.name.endsWith('.jar')) {
	        jarFiles << it
	}
}



jarFiles.each {
	def writeExtraLine =false; 
	println "File to process: ${it.name}"

	new File(tempDir).mkdirs()
	new AntBuilder().unzip(src:it, dest:zipDir)
 	  
	BufferedReader br = new BufferedReader(new FileReader(zipDir + "/" + descriptorFile))
	BufferedWriter bw = new BufferedWriter(new FileWriter(tempDir + "/" + descriptorFile))
 
	String line = null
	while ((line = br.readLine()) != null) {
	  bw.println(line)
	  if (writeExtraLine) {
		  bw.println("                connector-ref=\"VPInsecureConnector\"")
		  writeExtraLine = false;
	  }
	  if (line.indexOf("<http:inbound-endpoint") > 0) {
		  writeExtraLine = true;
	  }
	}

	br.close()
	bw.close()

	new File(zipDir + "/" + descriptorFile).delete()
	new AntBuilder().copy(todir: zipDir) {
	    fileset(dir: tempDir, includes: "*.xml")
	}

//	def newzipName = it.name - ".jar" + "-patch.jar"
	def newzipName = it.name - ".jar" + "-patch.jar"
	new AntBuilder().zip(destfile: newVirtDir + "/" + newzipName,
	  basedir: zipDir,
	  includes: "**")
  
	new AntBuilder().delete(dir:tempDir,failonerror:false)    
	new AntBuilder().delete(dir:zipDir,failonerror:false) 
}

