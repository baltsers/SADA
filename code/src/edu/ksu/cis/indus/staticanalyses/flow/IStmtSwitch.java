/*******************************************************************************
 * Indus, a program analysis and transformation toolkit for Java.
 * Copyright (c) 2001, 2007 Venkatesh Prasad Ranganath
 * 
 * All rights reserved.  This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 which accompanies 
 * the distribution containing this program, and is available at 
 * http://www.opensource.org/licenses/eclipse-1.0.php.
 * 
 * For questions about the license, copyright, and software, contact 
 * 	Venkatesh Prasad Ranganath at venkateshprasad.ranganath@gmail.com
 *                                 
 * This software was developed by Venkatesh Prasad Ranganath in SAnToS Laboratory 
 * at Kansas State University.
 *******************************************************************************/

package edu.ksu.cis.indus.staticanalyses.flow;

import edu.ksu.cis.indus.interfaces.IPrototype;
import soot.jimple.Stmt;

/**
 * This interface is provided by flow analysis statement walkers/visitors.
 *
 * @version $Revision: 1.4 $
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 */
public interface IStmtSwitch
		extends IPrototype<IStmtSwitch> {

	/**
	 * Process the given statement. The usual implementation would be visit the expressions in the statement.
	 *
	 * @param stmtToProcess the statement being visited or to be processed.
	 * @pre stmtToProcess != null
	 */
	void process(final Stmt stmtToProcess);
}

// End of File
