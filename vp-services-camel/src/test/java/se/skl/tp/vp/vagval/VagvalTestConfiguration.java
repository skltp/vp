package se.skl.tp.vp.vagval;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan(basePackages = {"se.skltp.takcache", "se.skl.tp.hsa.cache", "se.skl.tp.vp.vagval", "se.skl.tp.vp.config", "se.skl.tp.vagval", "se.skl.tp.behorighet", "se.skl.tp.vp.errorhandling","se.skl.tp.vp.service"})
public class VagvalTestConfiguration {

}
