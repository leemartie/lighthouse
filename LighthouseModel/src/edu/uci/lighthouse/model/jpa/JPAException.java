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
package edu.uci.lighthouse.model.jpa;

/**
 * Handle data base exception
 */
public class JPAException extends Exception {

	private static final long serialVersionUID = 7676004587653285319L;
	
	/**
	 * Constructor.
	 */
	public JPAException(String  message, Throwable reason){
		super(message, reason);		
	}

	public JPAException(Throwable reason){
		super(reason);		
	}
	
}
