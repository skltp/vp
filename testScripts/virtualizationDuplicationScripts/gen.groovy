File fin = new File("templatedir/tp2-service-mule-descriptor.xml.template.xml")

200.times { i ->
  def virtDir = "gentmpdir/virt-" + i
  new AntBuilder().copy(todir:virtDir) {
    fileset(dir:'templatedir' )
  }
  
  File fout = new File(virtDir + "/tp2-service-mule-descriptor.xml")
  fout.write(fin.text.replaceAll("###DYNCOUNTER###", "" + i))

  new AntBuilder().zip(destfile: "genvirtdir/virt-" + i + ".jar",
    basedir: "gentmpdir/virt-" + i,
    includes: "**")
    //excludes: "**/*.doc")    
}
