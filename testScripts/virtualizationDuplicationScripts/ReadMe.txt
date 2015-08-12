Scripts to mass-produce virtualizations for performance testing with VP.

The dir templatedir contains an unpacked virtualization (created with the
virtualization generator) with a modified:

tp2-service-mule-descriptor.xml

file where a token "###DYNCOUNTER###" has been added and is replaced by the
generation script to allow for duplication of a single virtualization.


Testing and profiling:

1. Make sure the tp2-service-mule-descriptor.xml.template is conform to the
  latest output format from the virtualization generator

2. Run the generation script: groovy gen

3. Run in Eclipse:
  a) add the generated jar's to the project vp-services classpath as External jars
  b) run VP using the VpMuleServer main class.

4. Profiling in VisualVM: attach to the locally running java-process.
