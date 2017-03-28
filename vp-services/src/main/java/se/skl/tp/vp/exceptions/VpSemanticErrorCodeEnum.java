/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
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
package se.skl.tp.vp.exceptions;

public enum VpSemanticErrorCodeEnum {
	UNSET("UNSET"), 
	VP001("VP001"), 
	VP002("VP002"), 
	VP003("VP003"), 
	VP004("VP004"), 
	VP005("VP005"), 
	VP006("VP006"), 
	VP007("VP007"), 
	VP008("VP008"), 
	VP009("VP009"), 
	VP010("VP010"), 
	VP011("VP011"), 
	VP012("VP012");

	private String code;
	public String getCode() {
		return code;
	}
	
	public VpSemanticErrorCodeEnum getCodeEnum(String code) {
		return VpSemanticErrorCodeEnum.valueOf(code);
	}
	
	VpSemanticErrorCodeEnum(String code) {
		this.code = code;
	}
	
	
}
