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

package edu.ksu.cis.indus.interfaces;

import edu.ksu.cis.indus.annotations.NonNull;
import edu.ksu.cis.indus.annotations.NonNullContainer;
import edu.ksu.cis.indus.common.datastructures.Triple;
import edu.ksu.cis.indus.common.graph.IObjectDirectedGraph;
import edu.ksu.cis.indus.common.graph.IObjectNode;

import java.util.Collection;
import java.util.Map;

import soot.SootMethod;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.MonitorStmt;
import soot.jimple.Stmt;

/**
 * This interface provides the information pertaining to Java monitors in the analyzed system.
 * <p>
 * Subtypes of this class have to return the constant <code>ID</code> defined in this class as a result of
 * <code>getId</code>.
 * </p>
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.29 $
 * @param <N> is the type of the node of the graph used to store the monitor information.
 */
public interface IMonitorInfo<N extends IObjectNode<N, Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod>>>
		extends IStatus, IIdentification {

	/**
	 * This is the interface to monitor graphs.
	 * 
	 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
	 * @author $Author: rvprasad $
	 * @version $Revision: 1.29 $
	 * @param <N> <i>refer to the type parameter of the same name in the super class.</i>
	 */
	interface IMonitorGraph<N extends IObjectNode<N, Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod>>>
			extends IObjectDirectedGraph<N, Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod>> {

		/**
		 * Retrieves the statements enclosed by the given monitor triple, both intra and interprocedurally.
		 * 
		 * @param monitorTriple describes the monitor of interest.
		 * @param transitive <code>true</code> indicates transitive closure is required; <code>false</code>, otherwise.
		 * @return a map from a method to the statements of that method that are enclosed by the given monitor.
		 * @pre monitorTriple.getThird() != null
		 * @post (not transitive) implies result.size() = 1
		 */
		Map<SootMethod, Collection<Stmt>> getInterProcedurallyEnclosedStmts(
				final Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod> monitorTriple, final boolean transitive);

		/**
		 * Retrieves the monitor triples for monitors enclosing the given statement in the given method, both intra and
		 * interprocedurally.
		 * 
		 * @param stmt obviously.
		 * @param method in which the monitor occurs.
		 * @param transitive <code>true</code> indicates transitive closure is required; <code>false</code>, otherwise.
		 * @return a collection of triples
		 * @post (not transitive) implies result.size() = 1
		 */
		Map<SootMethod, Collection<Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod>>> getInterProcedurallyEnclosingMonitorTriples(
				final Stmt stmt, final SootMethod method, final boolean transitive);
	}

	/**
	 * The id of this interface.
	 */
	Comparable<String> ID = "Monitor Information";

	/**
	 * Retrieves the statements enclosed by the given monitor triple. Only the statements occurring in the method in which the
	 * monitor occurs are returned.
	 * 
	 * @param monitorTriple describes the monitor of interest.
	 * @param transitive <code>true</code> indicates transitive closure is required; <code>false</code>, otherwise.
	 * @return a collection of statements.
	 * @pre monitorTriple.getThird() != null
	 */
	Collection<Stmt> getEnclosedStmts(final Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod> monitorTriple,
			final boolean transitive);

	/**
	 * Retrieves the monitor statements enclosing the given statement in the given method. Only the monitors occurring in the
	 * method in which the statement occurs are returned.
	 * 
	 * @param stmt obviously.
	 * @param method in which the monitor occurs.
	 * @param transitive <code>true</code> indicates transitive closure is required; <code>false</code>, otherwise.
	 * @return a collection of statements
	 */
	Collection<MonitorStmt> getEnclosingMonitorStmts(Stmt stmt, SootMethod method, final boolean transitive);

	/**
	 * Retrieves the monitor triples for monitors enclosing the given statement in the given method. Only the monitors
	 * occurring in the method in which the statement occurs are returned.
	 * 
	 * @param stmt obviously.
	 * @param method in which the monitor occurs.
	 * @param transitive <code>true</code> indicates transitive closure is required; <code>false</code>, otherwise.
	 * @return a collection of triples
	 */
	Collection<Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod>> getEnclosingMonitorTriples(Stmt stmt,
			SootMethod method, boolean transitive);

	/**
	 * Retrieves the monitor graph based on the shape of the call graph and the monitors in the method. Each monitor triple is
	 * represented as a node. An outgoing edges indicates that the monitor represented by the destination node is reachable
	 * from within the monitor (it is directly nested or nested in a method reachable via a call in the monitor) in the source
	 * node.
	 * 
	 * @param callgraphInfo to be used to generate an interprocedural graph. If this parameter is <code>null</code>,
	 *            intraprocedural monitor graph is generated.
	 * @return a graph
	 */
	IMonitorGraph<N> getMonitorGraph(final ICallGraphInfo callgraphInfo);

	/**
	 * Returns a collection of <code>Triple</code>s of <code>EnterMonitorStmt</code>, <code>ExitMonitorStmt</code>,
	 * and <code>SootMethod</code> in the system. The third element is the method in which the monitor occurs. In case the
	 * first and the second element of the triple are <code>null</code> then this means the method is a synchronized.
	 * 
	 * @return collection of monitors in the analyzed system.
	 * @post result->forall(o | o.getThird() ! = null)
	 */
	Collection<Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod>> getMonitorTriples();

	/**
	 * Returns a collection of <code>Triple</code>s of <code>EnterMonitorStmt</code>, <code>ExitMonitorStmt</code>,
	 * and <code>SootMethod</code> corresponding to monitor represented by <code>monitorStmt</code> in <code>method</code>.
	 * 
	 * @param monitorStmt obviously.
	 * @param method in which monitorStmt occurs.
	 * @return collection of monitors in the analyzed system.
	 * @post result->forall(o | o.getThird().equals(method))
	 * @post result->forall(o | o.getThird() ! = null)
	 */
	Collection<Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod>> getMonitorTriplesFor(final MonitorStmt monitorStmt,
			final SootMethod method);

	/**
	 * Returns a collection of <code>Triple</code>s of <code>EnterMonitorStmt</code>, <code>ExitMonitorStmt</code>,
	 * and <code>SootMethod</code> corresponding to the monitors in <code>method</code>. The third element is the method
	 * in which the monitor occurs. In case the first and the second element of the triple are <code>null</code> then this
	 * means the method is a synchronized.
	 * 
	 * @param method in which the monitors occur.
	 * @return collection of monitors in the analyzed system.
	 * @post result->forall(o | o.getThird().equals(method))
	 * @post result->forall(o | o.getThird() ! = null)
	 */
	Collection<Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod>> getMonitorTriplesIn(final SootMethod method);

	/**
	 * Retrieves all the monitor triples corresponding to the given monitor.
	 * 
	 * @param monitor of interest
	 * @return the collection of monitor triples
	 */
	Collection<Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod>> getMonitorTriplesOf(
			final Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod> monitor);

	/**
	 * Retrieves the statements that form the given monitor.
	 * 
	 * @param monitor of interest.
	 * @return a collection of statements.
	 * @post monitor.getFirst() = null implies result.isEmpty()
	 */
	Collection<? extends MonitorStmt> getStmtsOfMonitor(final Triple<EnterMonitorStmt, ExitMonitorStmt, SootMethod> monitor);

	/**
	 * Retreives the statements of the given method not enclosed by a monitor in that method.
	 * 
	 * @param method of interest.
	 * @return the collection of statements.
	 */
	Collection<Stmt> getUnenclosedStmtsOf(final SootMethod method);
}

// End of File
