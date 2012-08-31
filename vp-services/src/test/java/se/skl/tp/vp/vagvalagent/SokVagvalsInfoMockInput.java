package se.skl.tp.vp.vagvalagent;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SokVagvalsInfoMockInput {
	
	private static List<VagvalMockInputRecord> _vagvalInputs;
	private static Logger log = LoggerFactory.getLogger(SokVagvalsInfoMockInput.class);

	public void reset() {
		log.debug("Reset mock-vagval-info");
		_vagvalInputs = null;
	}

	public List<VagvalMockInputRecord> getVagvalInputs() {
		log.debug("Get mock-vagval-info, return {} records", ((_vagvalInputs == null) ? "NULL" : _vagvalInputs.size()));
		if(_vagvalInputs == null){
			_vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		}
		return _vagvalInputs;
	}

	public void setVagvalInputs(List<VagvalMockInputRecord> vagvalInputs) {
		log.debug("Set mock-vagval-info, with {} records", ((vagvalInputs == null) ? "NULL" : vagvalInputs.size()));
		_vagvalInputs = vagvalInputs;
	}
}