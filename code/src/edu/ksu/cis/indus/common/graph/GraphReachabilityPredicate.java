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

package edu.ksu.cis.indus.common.graph;

import edu.ksu.cis.indus.annotations.Immutable;
import edu.ksu.cis.indus.annotations.NonNull;
import edu.ksu.cis.indus.common.collections.IPredicate;

/**
 * This implementation checks if there is a path from the node representing the given object to the node representing an
 * object in the graph in the direction specified at initialization time. <code>evaluate</code> method will return
 * <code>true</code> only if both objects are represented in the node.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.8 $ $Date: 2007/10/09 15:59:13 $
 * @param <N> the type of nodes in the graph processed by this predicate.
 * @param <O> the type of object being represented by the object graph.
 */
public final class GraphReachabilityPredicate<N extends IObjectNode<N, O>, O>
		implements IPredicate<O> {

	/**
	 * The node to which the path leads to.
	 */
	private final N destNode;

	/**
	 * The direction of the path.
	 */
	private final boolean forward;

	/**
	 * The graph in which reachability is calculated.
	 */
	private final IObjectDirectedGraph<N, O> graph;

	/**
	 * Creates a new GraphReachabilityPredicate object.
	 * 
	 * @param theDestNode is a node in <code>theGraph</code>.
	 * @param forwardDir <code>true</code> indicates forward direction (following the edges); <code>false</code> indicates
	 *            backward direction (following the edges in the reverse direction).
	 * @param theGraph of interest.
	 */
	public GraphReachabilityPredicate(@NonNull @Immutable final N theDestNode, final boolean forwardDir,
			@NonNull @Immutable final IObjectDirectedGraph<N, O> theGraph) {
		destNode = theDestNode;
		forward = forwardDir;
		graph = theGraph;
	}

	/**
	 * {@inheritDoc}
	 */
	public  boolean evaluate(@Immutable final O srcObject) {
		final N _srcNode = graph.queryNode(srcObject);
		return destNode != null && _srcNode != null && graph.isReachable(_srcNode, destNode, forward);
	}
}

// End of File
