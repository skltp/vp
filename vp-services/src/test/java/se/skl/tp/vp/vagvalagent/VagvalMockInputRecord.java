package se.skl.tp.vp.vagvalagent;

import se.skl.tp.vp.vagvalrouter.VagvalInput;

public class VagvalMockInputRecord extends VagvalInput {

	public String adress;

	@Override
	public String toString() {
		return super.toString() + " " + adress;
	}

}
