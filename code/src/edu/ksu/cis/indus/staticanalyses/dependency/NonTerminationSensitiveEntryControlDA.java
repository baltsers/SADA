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

package edu.ksu.cis.indus.staticanalyses.dependency;

import edu.ksu.cis.indus.common.collections.IPredicate;
import edu.ksu.cis.indus.common.collections.InstanceOfPredicate;
import edu.ksu.cis.indus.common.datastructures.IWorkBag;
import edu.ksu.cis.indus.common.datastructures.LIFOWorkBag;
import edu.ksu.cis.indus.common.datastructures.Pair;
import edu.ksu.cis.indus.common.datastructures.Pair.PairManager;
import edu.ksu.cis.indus.common.soot.BasicBlockGraph;
import edu.ksu.cis.indus.common.soot.BasicBlockGraph.BasicBlock;
import edu.ksu.cis.indus.interfaces.ICallGraphInfo;
import edu.ksu.cis.indus.staticanalyses.InitializationException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.jimple.Stmt;

/**
 * This class provides direct intraprocedural non-termination sensitive backward control dependence information. For more
 * information about the dependence calculated in this implementation, please refer to <a
 * href="http://projects.cis.ksu.edu/docman/view.php/12/95/santos-tr2004-8.pdf">Santos-TR2004-8</a>.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.12 $
 */
public final class NonTerminationSensitiveEntryControlDA
		extends AbstractControlDA {

	/**
	 * This predicate can be used to check if an object of this class type.
	 */
	public static final IPredicate<IDependencyAnalysis<?, ?, ?, ?, ?, ?>> INSTANCEOF_PREDICATE = new InstanceOfPredicate<NonTerminationSensitiveEntryControlDA, IDependencyAnalysis<?, ?, ?, ?, ?, ?>>(
			NonTerminationSensitiveEntryControlDA.class);

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(NonTerminationSensitiveEntryControlDA.class);

	/**
	 * This is a cache that contains the nodes.
	 */
	protected List<BasicBlock> nodesCache;

	/**
	 * This is a cache that contains the nodes with multiple children.
	 */
	protected Collection<BasicBlock> nodesWithChildrenCache;

	/**
	 * This manages pair objects.
	 */
	private final PairManager pairMgr = new PairManager(false, true);

	/*
	 * The dependence information is stored as follows: For each method, a list of collection is maintained. Each location in
	 * the list corresponds to the statement at the same location in the statement list of the method. The collection is the
	 * statements to which the statement at the location of the collection is related via control dependence.
	 */

	/**
	 * Creates an instance of this class.
	 */
	public NonTerminationSensitiveEntryControlDA() {
		super(Direction.BI_DIRECTIONAL);
	}

	/**
	 * Calculates the control dependency information for the methods provided during initialization.
	 * 
	 * @see edu.ksu.cis.indus.staticanalyses.dependency.AbstractDependencyAnalysis#analyze()
	 */
	@Override public void analyze() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("BEGIN: Entry Control Dependence processing");
		}

		analyze(callgraph.getReachableMethods());

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("END: Entry Control Dependence processing");
		}
	}

	/**
	 * Calculates the control dependency information for the provided methods. The use of this method does not require a prior
	 * call to <code>setup</code>.
	 * 
	 * @param methods to be analyzed.
	 * @pre methods != null and not method->includes(null)
	 */
	public void analyze(final Collection<SootMethod> methods) {
		unstable();

		for (final Iterator<SootMethod> _i = methods.iterator(); _i.hasNext();) {
			final SootMethod _currMethod = _i.next();
			final BasicBlockGraph _bbGraph = getBasicBlockGraph(_currMethod);

			if (_bbGraph == null) {
				LOGGER.error("Method " + _currMethod.getSignature() + " did not have a basic block graph.");
				continue;
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Processing method: " + _currMethod.getSignature());
			}

			final BitSet[] _bbCDBitSets = computeControlDependency(_bbGraph);
			fixupMaps(_bbCDBitSets, _currMethod);
		}

		nodesCache = null;
		nodesWithChildrenCache = null;
		pairMgr.reset();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("analyze() - " + toString());
		}

		stable();
	}

	/**
	 * Sets up internal data structures.
	 * 
	 * @throws InitializationException when call graph service is not provided.
	 * @pre info.get(ICallGraphInfo.ID) != null and info.get(ICallGraphInfo.ID).oclIsTypeOf(ICallGraphInfo)
	 * @see edu.ksu.cis.indus.staticanalyses.interfaces.AbstractAnalysis#setup()
	 */
	@Override protected void setup() throws InitializationException {
		super.setup();
		callgraph = (ICallGraphInfo) info.get(ICallGraphInfo.ID);

		if (callgraph == null) {
			throw new InitializationException(ICallGraphInfo.ID + " was not provided.");
		}
	}

	/**
	 * Accumulates the tokens of ancestor nodes for the purpose of direct CD calculation. In this method, the tokens at
	 * ancestors of the control points which were dependees for the given node are injected into the token set of the given
	 * node if the dependees are no longer dependees.
	 * 
	 * @param bb at which to accumulate tokens.
	 * @param tokenSets is the collection of token sets of the nodes in the graph.
	 * @return <code>true</code> if any new tokens were accumulated; <code>false</code>, otherwise.
	 * @pre node != null and tokenSets != null
	 */
	private boolean accumulateTokensAtNode(final BasicBlock bb, final BitSet[][] tokenSets) {
		boolean _result = false;
		final int _nodeIndex = nodesCache.indexOf(bb);
		final Iterator<BasicBlock> _ctrlPoints = nodesWithChildrenCache.iterator();

		for (int _iIndex = nodesWithChildrenCache.size() - 1; _iIndex >= 0; _iIndex--) {
			final BasicBlock _ctrlPointNode = _ctrlPoints.next();
			final int _ctrlPointNodeIndex = nodesCache.indexOf(_ctrlPointNode);
			final BitSet _nodesCtrlPointBitSet = tokenSets[_nodeIndex][_ctrlPointNodeIndex];

			if (_nodesCtrlPointBitSet.cardinality() == getFanoutNumOf(_ctrlPointNode) && _nodeIndex != _ctrlPointNodeIndex) {
				_result |= copyAncestorBitSetsFromTo(_ctrlPointNodeIndex, _nodeIndex, tokenSets);
			}
		}
		return _result;
	}

	/**
	 * Calculates control dependency information from the given token information.
	 * 
	 * @param tokenSets is the collection of token sets of the nodes in the graph.
	 * @return an array of bitsets. The length of the array and each of the bitset in it is equal to the number of nodes in
	 *         the graph. The nth bitset captures the dependence information via set bits. The BitSets capture
	 *         dependent->dependee information.
	 * @pre tokenSets != null
	 * @post result->size() == graph.getNodes().size()
	 * @post result->forall(o | o.size() == graph.getNodes().size())
	 */
	private BitSet[] calculateCDFromTokenInfo(final BitSet[][] tokenSets) {
		// calculate control dependence based on token information
		final BitSet[] _result = new BitSet[nodesCache.size()];
		final Iterator<BasicBlock> _i = nodesWithChildrenCache.iterator();

		for (int _j = nodesWithChildrenCache.size(); _j > 0; _j--) {
			final BasicBlock _controlPoint = _i.next();
			final int _cpIndex = nodesCache.indexOf(_controlPoint);
			final int _succsSize = getFanoutNumOf(_controlPoint);

			for (int _k = nodesCache.size() - 1; _k >= 0; _k--) {
				final BitSet _tokens = tokenSets[_k][_cpIndex];

				if (_tokens != null) {
					final int _cardinality = _tokens.cardinality();

					if (_cardinality > 0 && _cardinality != _succsSize) {
						BitSet _temp = _result[_k];

						if (_temp == null) {
							_temp = new BitSet();
							_result[_k] = _temp;
						}
						_temp.set(_cpIndex);
					}
				}
			}
		}

		return _result;
	}

	/**
	 * Calculates the control dependency from a basic block graph. This calculates the dependence information in terms of
	 * nodes in the graph. This is later translated to statement level information by {@link
	 * NonTerminationSensitiveEntryControlDA#fixupMaps(BitSet[], SootMethod) fixupMaps}.
	 * 
	 * @param graph for which control dependency should be calculated.
	 * @return an array of bitsets. The length of the array and each of the bitset in it is equal to the number of nodes in
	 *         the graph. The nth bitset captures the dependence information via set bits. The BitSets capture
	 *         dependent->dependee information.
	 * @post result->size() == graph.getNodes().size()
	 * @post result->forall(o | o.size() == graph.getNodes().size())
	 */
	private BitSet[] computeControlDependency(final BasicBlockGraph graph) {
		nodesCache = graph.getNodes();
		nodesWithChildrenCache = getNodesWithChildren();

		final int _size = nodesCache.size();
		final BitSet[][] _tokenSets = new BitSet[_size][_size];

		for (int _i = _size - 1; _i >= 0; _i--) {
			for (int _j = _size - 1; _j >= 0; _j--) {
				_tokenSets[_i][_j] = new BitSet();
			}
		}

		final IWorkBag<Pair<BasicBlock, Boolean>> _wb = injectTokensAndGenerateWorkForTokenPropagation(_tokenSets);

		while (_wb.hasWork()) {
			final Pair<BasicBlock, Boolean> _pair = _wb.getWork();
			_wb.addAllWorkNoDuplicates(processNode(_pair, _tokenSets));

			if (LOGGER.isDebugEnabled()) {
				final StringBuffer _msg = new StringBuffer();

				for (int _i = _size - 1; _i >= 0; _i--) {
					_msg.append("{");

					for (int _j = _size - 1; _j >= 0; _j--) {
						_msg.append(_tokenSets[_i][_j].toString() + ",");
					}
					_msg.append("}");
				}

				LOGGER.debug("computeControlDependency() - Node being processed : _node = " + _pair.getFirst()
						+ "nodeIndex = " + nodesCache.indexOf(_pair.getFirst()) + " _tokenSets = " + _msg);
			}
		}

		return calculateCDFromTokenInfo(_tokenSets);
	}

	/**
	 * Injects the tokens corresponding to the ancestors of the node at <code>src</code> into the token sets corresponding
	 * to the same ancestors at the node at <code>dest</code>.
	 * 
	 * @param src is the index of the node whose ancestor's tokens need to be propagated.
	 * @param dest is the index of the node into which the tokens will be propagated to.
	 * @param tokenSets is the collection of token sets of the nodes in the graph.
	 * @return <code>true</code>if bits/tokens were added to the destination node; <code>false</code>, otherwise.
	 * @pre tokenSets != null
	 * @pre 0 &lt;= src &lt; tokensSets.length
	 * @pre 0 &lt;= dest &lt; tokensSets.length
	 */
	private boolean copyAncestorBitSetsFromTo(final int src, final int dest, final BitSet[][] tokenSets) {
		boolean _result = false;
		final BitSet _temp = new BitSet();
		final BitSet[] _srcBitSets = tokenSets[src];
		final BitSet[] _destBitSets = tokenSets[dest];
		final Iterator<BasicBlock> _i = nodesWithChildrenCache.iterator();
		final int _iEnd = nodesWithChildrenCache.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final BasicBlock _ancestor = _i.next();
			final int _ancestorIndex = nodesCache.indexOf(_ancestor);

			if (dest != _ancestorIndex) {
				final BitSet _srcsAncestorBitSet = _srcBitSets[_ancestorIndex];
				final BitSet _destAndAncestorBitSet = _destBitSets[_ancestorIndex];
				_temp.clear();
				_temp.or(_srcsAncestorBitSet);
				_temp.andNot(_destAndAncestorBitSet);
				_result |= !_temp.isEmpty();
				_destAndAncestorBitSet.or(_temp);
			}
		}
		return _result;
	}

	/**
	 * Translates the dependence information as captured in <code>bbCDBitSets</code> to statement level info and populates
	 * the dependeXXMap fields.
	 * 
	 * @param bbCDBitSets is the array that contains the basic block level dependence information as calculated by {@link
	 *            #computeControlDependency(BasicBlockGraph) computeControlDependency}.
	 * @param method for which the maps are being populated.
	 * @pre graph != null and bbCDBitSets != null and method != null
	 * @post dependee2dependent.get(method) != null
	 * @post dependee2dependent.values()->forall(o | o->forall(p | p != null()))
	 * @post dependent2dependee.get(method) != null
	 * @post dependent2dependee.values()->forall(o | o->forall(p | p != null()))
	 */
	private void fixupMaps(final BitSet[] bbCDBitSets, final SootMethod method) {
		final List<Stmt> _sl = getStmtList(method);
		final List<Collection<Stmt>> _mDependee = new ArrayList<Collection<Stmt>>();
		final List<Collection<Stmt>> _mDependent = new ArrayList<Collection<Stmt>>();

		for (int _i = _sl.size(); _i > 0; _i--) {
			_mDependee.add(null);
			_mDependent.add(null);
		}

		boolean _flag = false;

		for (int _i = bbCDBitSets.length - 1; _i >= 0; _i--) {
			final BitSet _cd = bbCDBitSets[_i];
			_flag |= _cd != null;

			if (_cd != null) {
				final Collection<Stmt> _cdp = new ArrayList<Stmt>();
				final BasicBlock _bb = nodesCache.get(_i);

				for (final Iterator<Stmt> _j = _bb.getStmtsOf().iterator(); _j.hasNext();) {
					_mDependee.set(_sl.indexOf(_j.next()), _cdp);
				}

				for (int _j = _cd.nextSetBit(0); _j != -1; _j = _cd.nextSetBit(_j + 1)) {
					final BasicBlock _cdbb = nodesCache.get(_j);
					final Stmt _cdStmt = _cdbb.getTrailerStmt();
					_cdp.add(_cdStmt);

					final int _deIndex = _sl.indexOf(_cdStmt);
					Collection<Stmt> _dees = _mDependent.get(_deIndex);

					if (_dees == null) {
						_dees = new ArrayList<Stmt>();
						_mDependent.set(_deIndex, _dees);
					}
					_dees.addAll(_bb.getStmtsOf());
				}
			}
		}

		if (_flag) {
			dependee2dependent.put(method, new ArrayList<Collection<Stmt>>(_mDependent));
			dependent2dependee.put(method, new ArrayList<Collection<Stmt>>(_mDependee));
		} else {
			dependee2dependent.put(method, null);
			dependent2dependee.put(method, null);
		}
	}

	/**
	 * Returns a collection of nodes with multiple children.
	 * 
	 * @return a collection of nodes
	 * @post result != null
	 * @post result->forall(o | getFanoutNumOf(o) > 1)
	 */
	private Collection<BasicBlock> getNodesWithChildren() {
		final Collection<BasicBlock> _result = new HashSet<BasicBlock>();

		for (final Iterator<BasicBlock> _i = nodesCache.iterator(); _i.hasNext();) {
			final BasicBlock _b = _i.next();

			if (getFanoutNumOf(_b) > 1) {
				_result.add(_b);
			}
		}
		return _result;
	}

	/**
	 * Injects tokens into token sets of successor nodes of nodes with multiple children and adds the successors to a new
	 * workbag which is returned.
	 * 
	 * @param tokenSets is the collection of token sets of the nodes in the graph.
	 * @return a work bag with nodes that contain nodes to process.
	 * @pre tokenSets != null
	 * @post result != null
	 */
	private IWorkBag<Pair<BasicBlock, Boolean>> injectTokensAndGenerateWorkForTokenPropagation(final BitSet[][] tokenSets) {
		final IWorkBag<Pair<BasicBlock, Boolean>> _wb = new LIFOWorkBag<Pair<BasicBlock, Boolean>>();

		for (final Iterator<BasicBlock> _i = nodesWithChildrenCache.iterator(); _i.hasNext();) {
			final BasicBlock _node = _i.next();
			final int _nodeIndex = nodesCache.indexOf(_node);
			final Collection<BasicBlock> _succs = _node.getSuccsOf();
			final Iterator<BasicBlock> _k = _succs.iterator();

			for (int _j = _succs.size(), _count = 0; _j > 0; _j--, _count++) {
				final BasicBlock _succ = _k.next();
				final int _succIndex = nodesCache.indexOf(_succ);
				final BitSet _temp = tokenSets[_succIndex][_nodeIndex];
				_temp.set(_count);
				_wb.addWorkNoDuplicates(pairMgr.getPair(_succ, Boolean.TRUE));
			}
		}
		return _wb;
	}

	/*
	 * In this class, the tokens corresponding to ancestors are blocked at control points. Only when a node accumulates all
	 * tokens of a control point node, the tokens at the control point corresponding to the ancestor of the control point are
	 * injected into the token set of the node.
	 */

	/**
	 * Processes the given node. Basically, it propagates the tokens to it's successor and returns the successor whose token
	 * sets were modified.
	 * 
	 * @param pair to be processed.
	 * @param tokenSets is the collection of token sets of the nodes in the graph. The first subscript is the index of the
	 *            dependent basic block in the sequence of basic blocks. The second subscript is the index of the control
	 *            point basic block in the sequence of basic blocks. The bit set at these subscript indicate the number of
	 *            tokens (corresponding to the successors of the control point) that have been accumulated at the dependent
	 *            basic block.
	 * @return the collection of nodes whose token sets were modified.
	 * @pre pair != null and tokenSets != null
	 * @pre 0 &lt;= graphCache.getNumOfSuccsOf(pair.getFirst()) &lt;= 1
	 * @post result != null
	 * @post pair.getFirst().getSuccsOf().containsAll(result)
	 */
	private Collection<Pair<BasicBlock, Boolean>> processNode(final Pair<BasicBlock, Boolean> pair, final BitSet[][] tokenSets) {
		final BasicBlock _node = pair.getFirst();
		final boolean _addedDueToTokePropagation = (pair.getSecond()).booleanValue();
		final boolean _accumlatedTokens = accumulateTokensAtNode(_node, tokenSets);

		Collection<Pair<BasicBlock, Boolean>> _result = Collections.emptySet();

		if (_addedDueToTokePropagation || _accumlatedTokens) {
			final int _size = getFanoutNumOf(_node);

			if (_size == 1) {
				_result = processNodeWithOneChild(_node, tokenSets);
			} else if (_size > 1) {
				final int _nodeIndex = nodesCache.indexOf(_node);
				_result = new HashSet<Pair<BasicBlock, Boolean>>();

				final int _iEnd = nodesCache.size();

				for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
					final BitSet _bitSet = tokenSets[_iIndex][_nodeIndex];

					if (_bitSet.cardinality() == _size) {
						_result.add(pairMgr.getPair(nodesCache.get(_iIndex), Boolean.valueOf(_accumlatedTokens)));
					}
				}
			}
		}
		return _result;
	}

	/**
	 * Processes the given node. Basically, it propagates the tokens to it's successor and returns the successor whose token
	 * sets were modified.
	 * 
	 * @param node to be processed.
	 * @param tokenSets is the collection of token sets of the nodes in the graph. The first subscript is the index of the
	 *            dependent basic block in the sequence of basic blocks. The second subscript is the index of the control
	 *            point basic block in the sequence of basic blocks. The bit set at these subscript indicate the number of
	 *            tokens (corresponding to the successors of the control point) that have been accumulated at the dependent
	 *            basic block.
	 * @return the collection of nodes whose token sets were modified.
	 * @pre node != null and tokenSets != null and 0 &lt;= node.getSuccsOf().size() &lt;= 1
	 * @post result != null
	 * @post node.getSuccsOf().containsAll(result)
	 */
	private Collection<Pair<BasicBlock, Boolean>> processNodeWithOneChild(final BasicBlock node, final BitSet[][] tokenSets) {
		boolean _addflag = false;
		final int _nodeIndex = nodesCache.indexOf(node);
		final BitSet[] _nodeBitSets = tokenSets[_nodeIndex];
		final BasicBlock _succ = node.getSuccsOf().iterator().next();
		final int _succIndex = nodesCache.indexOf(_succ);
		final BitSet[] _succBitSets = tokenSets[_succIndex];
		final BitSet _temp = new BitSet();
		final Iterator<BasicBlock> _i = nodesWithChildrenCache.iterator();
		final int _iEnd = nodesWithChildrenCache.size();

		for (int _iIndex = 0; _iIndex < _iEnd; _iIndex++) {
			final BasicBlock _ancestor = _i.next();
			final int _ancIndex = nodesCache.indexOf(_ancestor);
			final BitSet _nodeAncestorTokenSet = _nodeBitSets[_ancIndex];
			final BitSet _succAncestorTokenSet = _succBitSets[_ancIndex];

			_temp.clear();
			_temp.or(_nodeAncestorTokenSet);
			_temp.andNot(_succAncestorTokenSet);

			if (!_temp.isEmpty()) {
				_succAncestorTokenSet.or(_temp);
				_addflag |= true;
			}
		}

		final Collection<Pair<BasicBlock, Boolean>> _result;

		if (_addflag) {
			_result = Collections.singleton(pairMgr.getPair(_succ, Boolean.TRUE));
		} else {
			_result = Collections.emptySet();
		}
		return _result;
	}
}

// End of File
