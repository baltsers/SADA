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

import edu.ksu.cis.indus.staticanalyses.flow.IFGNodeConnector;
import edu.ksu.cis.indus.staticanalyses.flow.IStmtSwitch;
import edu.ksu.cis.indus.staticanalyses.tokens.ITokens;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

/**
 * The expression visitor used in flow sensitive mode of object flow analysis.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @version $Revision: 1.15 $ $Date: 2007/02/10 19:07:03 $
 * @param <T> is the type of the token set object.
 */
class FlowSensitiveExprSwitch<T extends ITokens<T, Value>>
		extends FlowInsensitiveExprSwitch<T> {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowSensitiveExprSwitch.class);

	/**
	 * This is a weak reference to the local def information and it provides the def sites for local variables in the
	 * associated method. This is used in conjunction with flow-sensitive information calculation.
	 */
	protected WeakReference<SimpleLocalDefs> defs = new WeakReference<SimpleLocalDefs>(null);

	/**
	 * This indicates if the method variant is unretrievale due to various reasons such as non-concrete body.
	 */
	protected boolean unRetrievable;

	/**
	 * Creates a new <code>FlowSensitiveExprSwitch</code> instance.
	 * 
	 * @param stmtSwitchParam the statement visitor which uses this instance of expression visitor.
	 * @param nodeConnector the connector to be used to connect the ast and non-ast nodes.
	 * @param type2valueMapper provides the values of a given type in the system.
	 * @pre stmtSwitchParam != null and nodeConnector != null
	 */
	public FlowSensitiveExprSwitch(final IFGNodeConnector<OFAFGNode<T>> nodeConnector,
			final Value2ValueMapper type2valueMapper, final IStmtSwitch stmtSwitchParam) {
		super(nodeConnector, type2valueMapper, stmtSwitchParam);
	}

	/**
	 * Processes the local expression. This implementation connects the nodes at the def sites to the nodes at the use site of
	 * the local.
	 * 
	 * @param e the expression to be processed.
	 * @pre e != null
	 */
	@Override public void caseLocal(final Local e) {
		final OFAFGNode<T> _localNode = method.getASTNode(e, context);
		MethodVariant.setFilterOfBasedOn(_localNode, e.getType(), tokenMgr);

		final Stmt _stmt = context.getStmt();
		final ValueBox _backup = context.setProgramPoint(null);

		if (_stmt.getUseBoxes().contains(_backup)) {
			final Collection<Unit> _defs = getDefsOfAt(e, _stmt);

			for (final Iterator<Unit> _i = _defs.iterator(); _i.hasNext();) {
				final DefinitionStmt _defStmt = (DefinitionStmt) _i.next();
				context.setProgramPoint(_defStmt.getLeftOpBox());

				final Value _leftOp = _defStmt.getLeftOp();
				final OFAFGNode<T> _defNode = method.getASTNode(_leftOp, context);
				MethodVariant.setFilterOfBasedOn(_defNode, _leftOp.getType(), tokenMgr);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Local Def:" + _leftOp + "\n" + _defNode + context);
				}
				_defNode.addSucc(_localNode);
			}
		}

		context.setProgramPoint(_backup);
		setFlowNode(_localNode);
	}

	/**
	 * Returns a new instance of this class.
	 * 
	 * @param o the statement visitor which shall use the created visitor instance.
	 * @return the new visitor instance.
	 * @pre o != null and o[0].oclIsKindOf(IStmtSwitch)
	 * @post result != null
	 */
	@Override public FlowSensitiveExprSwitch<T> getClone(final Object... o) {
		return new FlowSensitiveExprSwitch<T>(connector, valueRetriever, (IStmtSwitch) o[0]);
	}

	/**
	 * Returns the definitions of local variable <code>l</code> that arrive at statement <code>s</code>.
	 * 
	 * @param l the local for which the definitions are requested.
	 * @param s the statement at which the definitions are requested.
	 * @return the list of definitions of <code>l</code> that arrive at statement <code>s</code>.
	 * @pre l != null and s != null
	 */
	public Collection<Unit> getDefsOfAt(final Local l, final Stmt s) {
		Collection<Unit> _result = Collections.emptySet();

		if (!unRetrievable) {
			final SimpleLocalDefs _temp = defs.get();

			if (_temp != null) {
				_result = _temp.getDefsOfAt(l, s);
			} else {
				final SootMethod _method = method.getMethod();

				if (_method.hasActiveBody()) {
					final SimpleLocalDefs _temp2 = new SimpleLocalDefs(new CompleteUnitGraph(_method.retrieveActiveBody()));
					defs = new WeakReference<SimpleLocalDefs>(_temp2);
					_result = _temp2.getDefsOfAt(l, s);
				} else {
					unRetrievable = true;
				}
			}
		}
		return _result;
	}
}

// End of File
