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

package edu.ksu.cis.indus.processing;

import java.util.Collection;
import java.util.List;

import soot.SootMethod;
import soot.jimple.Stmt;

/**
 * This interface is used to retrieve a collection of statement sequences. This is used in conjuction with controlling the
 * order of visiting parts of the system.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.4 $
 */
public interface IStmtSequencesRetriever {

	/**
	 * Retrives a collection of statement sequences.
	 *
	 * @param method for which the statement sequences are requested.
	 * @return a collection of statement sequence.
	 * @pre method != null
	 * @post result != null
	 * @post not result->exists(o | o == null)
	 */
	Collection<List<Stmt>> retrieveStmtSequences(SootMethod method);
}

// End of File
