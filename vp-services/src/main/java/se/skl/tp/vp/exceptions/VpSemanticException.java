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

public class VpSemanticException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final VpSemanticErrorCodeEnum errorCode;

	public VpSemanticException(String message, VpSemanticErrorCodeEnum errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public VpSemanticErrorCodeEnum getErrorCode() {
		// never return null, eliminate need for null-checking
		if (errorCode != null) {
			return errorCode;
		}
		return VpSemanticErrorCodeEnum.UNSET;
	}

}
