/*******************************************************************************
 * Copyright (c) {2009,2011} {Software Design and Collaboration Laboratory (SDCL)
 *				, University of California, Irvine}.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    {Software Design and Collaboration Laboratory (SDCL)
 *	, University of California, Irvine} 
 *			- initial API and implementation and/or initial documentation
 *******************************************************************************/ 
package edu.uci.lighthouse.model;

import javax.persistence.Entity;

@Entity
public class LighthouseField extends LighthouseEntity {

	private static final long serialVersionUID = 6290735567708481729L;

	protected LighthouseField() {
		this("");
	}

	public LighthouseField(String fqn) {
		super(fqn);
	}

}
