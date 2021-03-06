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

package edu.ksu.cis.indus.staticanalyses.flow.instances.ofa;

import edu.ksu.cis.indus.processing.Context;

import edu.ksu.cis.indus.staticanalyses.flow.IFGNodeConnector;
import edu.ksu.cis.indus.staticanalyses.flow.IMethodVariant;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;

import soot.SootField;
import soot.Value;

import soot.jimple.FieldRef;

/**
 * This class encapsulates the logic to instrument the flow of values corresponding to fields.
 *
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision: 1.18 $
 * @param <T>  is the type of the token set object.
 */
class FieldAccessExprWork<T extends ITokens<T, Value>>
		extends AbstractMemberDataAccessExprWork<T> {

	/**
	 * Creates a new <code>FieldAccessExprWork</code> instance.
	 *
	 * @param callerMethod the method in which the access occurs.
	 * @param accessContext the context in which the access occurs.
	 * @param accessNode the flow graph node associated with the access expression.
	 * @param connectorToBeUsed the connector to use to connect the ast node to the non-ast node.
	 * @param tokenSet used to store the tokens that trigger the execution of this work peice.
	 * @pre callerMethod != null and accessContext != null and accessNode != null and connectorToBeUsed != null and tokenSet !=
	 *      null
	 */
	public FieldAccessExprWork(final IMethodVariant<OFAFGNode<T>> callerMethod, final Context accessContext,
			final OFAFGNode<T> accessNode, final IFGNodeConnector<OFAFGNode<T>> connectorToBeUsed, final T tokenSet) {
		super(callerMethod, accessContext, accessNode, connectorToBeUsed, tokenSet);
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.flow.instances.ofa.AbstractMemberDataAccessExprWork#getFGNodeForMemberData()
	 */
	@Override protected OFAFGNode<T> getFGNodeForMemberData() {
		final SootField _sf = ((FieldRef) accessExprBox.getValue()).getField();
		return caller.getFA().getFieldVariant(_sf, context).getFGNode();
	}
}

// End of File
