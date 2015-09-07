Scripts to patch virtualizations and add a connector-ref to http traffic

Testing and profiling:

1. Place virtualizations to patch in directory old-vp-services. Remove jar-files that isn’t a virtualization like schema-jar files.

2. Run the generation script: groovy genPatch

3. Patched virtualizations are put in directory new-vp-services