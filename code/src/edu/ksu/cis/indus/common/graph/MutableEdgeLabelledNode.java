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
import edu.ksu.cis.indus.annotations.NonNullContainer;
import edu.ksu.cis.indus.common.collections.MapUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * This is an implementation of the node in mutable edge-labelled directed graphs.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.7 $ $Date: 2007/02/10 19:08:36 $
 * @param <T> the subtype of this type.
 */
public class MutableEdgeLabelledNode<T extends MutableEdgeLabelledNode<T>>
		extends EdgeLabelledNode<T>
		implements IMutableEdgeLabelledNode<T> {

	/**
	 * Creates an instance of this class.
	 * 
	 * @param preds is the reference to the collection of predecessors.
	 * @param succs is the reference to the collection of successors.
	 */
	protected MutableEdgeLabelledNode(@Immutable @NonNull @NonNullContainer final Collection<T> preds,
			@Immutable @NonNull @NonNullContainer final Collection<T> succs) {
		super(preds, succs);
	}

	/**
	 * Removes all nodes that were related to this node via the given label by updating the given map and collection.
	 * 
	 * @param <T1> the type of objects in the collection.
	 * @param label of interest.
	 * @param map that maps labels to predecessor or successor nodes. This is updated.
	 * @param col is a collection of predecessor or successor nodes of this node. This is updated.
	 * @return <code>true</code> if the node was related to this node and it as removed; <code>false</code>, otherwise.
	 */
	private static <T1> boolean removedEdgesLabelled(@NonNull @Immutable final IEdgeLabel label,
			@NonNull @NonNullContainer final Map<IEdgeLabel, Collection<T1>> map,
			@NonNull @NonNullContainer final Collection<T1> col) {
		final boolean _result = map.remove(label) != null;
		retainAllIn(col, map.values());
		return _result;
	}

	/**
	 * Removes the relation between this and the given node by updating the given map and collection.
	 * 
	 * @param <T1> the node type.
	 * @param node of interest.
	 * @param map that maps labels to predecessor or successor nodes. This is updated.
	 * @param col is a collection of predecessor or successor nodes of this node. This is updated.
	 * @return <code>true</code> if the node was related to this node and it as removed; <code>false</code>, otherwise.
	 */
	private static <T1> boolean removeNode(@NonNull @Immutable final T1 node,
			@NonNull @NonNullContainer final Map<IEdgeLabel, Collection<T1>> map,
			@NonNull @NonNullContainer final Collection<T1> col) {
		final Iterator<IEdgeLabel> _i = map.keySet().iterator();
		final int _iEnd = map.keySet().size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final IEdgeLabel _k = _i.next();
			map.get(_k).remove(node);
		}
		return col.remove(node);
	}

	/**
	 * Retains all elements in <code>col</code> that exist in any collection in <code>collections</code>.
	 * 
	 * @param <T1> the type of objects in the collection.
	 * @param col to be updated.
	 * @param collections is a collection of collections.
	 */
	private static <T1> void retainAllIn(@NonNull @NonNullContainer final Collection<T1> col,
			@NonNull @NonNullContainer @Immutable final Collection<Collection<T1>> collections) {
		for (final Collection<T1> _c : collections) {
			col.retainAll(_c);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean addIncomingEdgeLabelledFrom(@NonNull @Immutable final IEdgeLabel label,
			@NonNull @Immutable final T node) {
		predecessors.add(node);
		return MapUtils.putIntoCollectionInMap(label2inNodes, label, node);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean addOutgoingEdgeLabelledTo(@NonNull @Immutable final IEdgeLabel label,
			@NonNull @Immutable final T node) {
		successors.add(node);
		return MapUtils.putIntoCollectionInMap(label2outNodes, label, node);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean addPredecessor(@NonNull @Immutable final T node) {
		return addIncomingEdgeLabelledFrom(IEdgeLabelledDirectedGraph.DUMMY_LABEL, node);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean addSuccessor(@NonNull @Immutable final T node) {
		return addOutgoingEdgeLabelledTo(IEdgeLabelledDirectedGraph.DUMMY_LABEL, node);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean removeIncomingEdgeLabelledFrom(@NonNull @Immutable final IEdgeLabel label,
			@NonNull @Immutable final T node) {
		return removeEdgesLabelledForViaUpdate(label, node, label2inNodes, predecessors);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean removeIncomingEdgesLabelled(@NonNull @Immutable final IEdgeLabel label) {
		return removedEdgesLabelled(label, label2inNodes, predecessors);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean removeOutgoingEdgeLabelledTo(@NonNull @Immutable final IEdgeLabel label,
			@Immutable @NonNull final T node) {
		return removeEdgesLabelledForViaUpdate(label, node, label2outNodes, successors);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean removeOutgoingEdgesLabelled(@Immutable @NonNull final IEdgeLabel label) {
		return removedEdgesLabelled(label, label2outNodes, successors);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean removePredecessor(@Immutable @NonNull final T node) {
		return removeNode(node, label2inNodes, predecessors);
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean removeSuccessor(@NonNull @Immutable final T node) {
		return removeNode(node, label2outNodes, successors);
	}

	/**
	 * Removes the edges with the given label that to/from the given node by updating the given map and collection. The map
	 * and the collection dictate the to/from direction.
	 * 
	 * @param label of interest.
	 * @param node of interest.
	 * @param map that maps labels to predecessor or successor nodes. This is updated.
	 * @param col is a collection of predecessor or successor nodes of this node. This is updated.
	 * @return <code>true</code> if the node existed and was removed; <code>false</code>, otherwise.
	 */
	private boolean removeEdgesLabelledForViaUpdate(@NonNull @Immutable final IEdgeLabel label,
			@NonNull @Immutable final T node, @NonNull @NonNullContainer final Map<IEdgeLabel, Collection<T>> map,
			@NonNull @NonNullContainer final Collection<T> col) {
		final Collection<T> _t = MapUtils.queryCollection(map, label);
		final boolean _result;

		if (_t.isEmpty()) {
			_result = false;
		} else {
			_result = _t.remove(node);

			if (_result) {
				if (_t.isEmpty()) {
					map.remove(label);
				}
				retainAllIn(col, map.values());
			}
		}
		return _result;
	}
}
