package se.skl.tp.vp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan(basePackages = {"se.skl.tp.vp.certificate","se.skl.tp.vp.httpheader", "se.skl.tp.vp.errorhandling", "se.skltp.takcache", "se.skl.tp.hsa.cache"})
public class TestBeanConfiguration {

}
