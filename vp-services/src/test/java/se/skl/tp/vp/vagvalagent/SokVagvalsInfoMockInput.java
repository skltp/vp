package se.skl.tp.vp.vagvalagent;

import java.util.ArrayList;
import java.util.List;

import se.skl.tp.vp.vagvalrouter.VagvalInput;

public class SokVagvalsInfoMockInput {
	
	private List<VagvalInput> vagvalInputs;

	public List<VagvalInput> getVagvalInputs() {
		if(vagvalInputs == null){
			vagvalInputs = new ArrayList<VagvalInput>();
		}
		return vagvalInputs;
	}

	public void setVagvalInputs(List<VagvalInput> vagvalInputs) {
		this.vagvalInputs = vagvalInputs;
	}

	
	
	

}
