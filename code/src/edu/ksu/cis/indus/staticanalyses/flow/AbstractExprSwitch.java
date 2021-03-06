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

import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Value;
import soot.ValueBox;
import soot.jimple.AbstractJimpleValueSwitch;

/**
 * The expression visitor class. This class provides the default method implementations for all the expressions that need to
 * be dealt at Jimple level. The class is tagged as <code>abstract</code> to force the users to extend the class as
 * required.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.24 $
 * @param <E> is the type of the expression visitor.
 * @param <SYM> is the type of symbol whose flow is being analyzed.
 * @param <T> is the type of the token set object.
 * @param <N> is the type of the summary node in the flow analysis.
 */
public abstract class AbstractExprSwitch<E extends AbstractExprSwitch<E, SYM, T, N, R>, SYM, T extends ITokens<T, SYM>, N extends IFGNode<SYM, T, N>, R>
		extends AbstractJimpleValueSwitch
		implements IExprSwitch<N> {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExprSwitch.class);

	/**
	 * The object used to connect flow graph nodes corresponding to AST and non-AST entities. This provides the flexibility to
	 * use the same implementation of the visitor with different connectors to process LHS and RHS entities.
	 */
	protected final IFGNodeConnector<N> connector;

	/**
	 * This visitor works in the context given by <code>context</code>.
	 */
	protected final Context context;

	/**
	 * The instance of the underlying flow analysis framework.
	 */
	protected final FA<SYM, T, N, R> fa;

	/**
	 * This visitor is used to visit the expressions in the <code>method</code> variant.
	 */
	protected final IMethodVariant<N> method;

	/**
	 * This visitor is used by <code>stmt</code> to walk the embedded expressions.
	 */
	protected final IStmtSwitch stmtSwitch;

	/**
	 * This captures the flow node resulting from visiting an expression.
	 */
	private N resultFlowNode;

	/**
	 * Creates a new <code>AbstractExprSwitch</code> instance. In non-prototype mode, all of the fields (declared in this
	 * class) will be non-null after returning from the constructor.
	 * 
	 * @param stmtVisitor the statement visitor which shall use this expression visitor.
	 * @param connectorToUse the connector to be used by this expression visitor to connect flow graph nodes corresponding to
	 *            AST and non-AST entities.
	 * @pre connectorToUse != null
	 */
	protected AbstractExprSwitch(final IStmtSwitch stmtVisitor, final IFGNodeConnector<N> connectorToUse) {
		this.stmtSwitch = stmtVisitor;
		this.connector = connectorToUse;

		if (stmtSwitch != null) {
			context = ((AbstractStmtSwitch) stmtSwitch).context;
			method = ((AbstractStmtSwitch) stmtSwitch).method;
			fa = ((AbstractStmtSwitch) stmtSwitch).method.getFA();
		} else {
			context = null;
			method = null;
			fa = null;
		}
	}

	/**
	 * Provides the default implementation when any expression is not handled by the visitor. It sets the flow node associated
	 * with the AST as the result.
	 * 
	 * @param o the expression which is not handled by the visitor.
	 */
	@Override public void defaultCase(final Object o) {
		setResult(method.getASTNode((Value) o, context));

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(o + "(" + o.getClass() + ") is not handled.");
		}
	}

	/**
	 * This method will throw <code>UnsupportedOperationException</code>.
	 * 
	 * @return (This method raises an exception.)
	 * @throws UnsupportedOperationException as this method is not supported.
	 */
	public E getClone(@SuppressWarnings("unused") final Object... o) {
		throw new UnsupportedOperationException("Parameterless prototype method is not supported.");
	}

	/**
	 * @see edu.ksu.cis.indus.staticanalyses.flow.IExprSwitch#getFlowNode()
	 */
	public N getFlowNode() {
		return resultFlowNode;
	}

	/**
	 * Processes the expression at the given program point, <code>v</code>.
	 * 
	 * @param v the program point at which the to-be-processed expression occurs.
	 * @pre v != null
	 */
	public void process(final ValueBox v) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Started to process expression: " + v.getValue());
		}

		final ValueBox _temp = context.setProgramPoint(v);
		v.getValue().apply(this);
		context.setProgramPoint(_temp);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Finished processing expression: " + v.getValue() + "\n" + getResult());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFlowNode(final N node) {
		resultFlowNode = node;
	}
}

// End of File
