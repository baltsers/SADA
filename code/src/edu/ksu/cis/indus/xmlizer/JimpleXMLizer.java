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

package edu.ksu.cis.indus.xmlizer;

import edu.ksu.cis.indus.processing.AbstractProcessor;
import edu.ksu.cis.indus.processing.Context;
import edu.ksu.cis.indus.processing.ProcessingController;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.znerd.xmlenc.XMLOutputter;

import soot.Body;
import soot.Local;
import soot.Modifier;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Trap;
import soot.Type;

import soot.jimple.Stmt;

/**
 * This class can be used to xmlize a system represented as Jimple.
 * 
 * @author <a href="http://www.cis.ksu.edu/~rvprasad">Venkatesh Prasad Ranganath</a>
 * @author $Author: rvprasad $
 * @version $Revision: 1.47 $ $Date: 2007/02/10 19:08:38 $
 */
public class JimpleXMLizer
		extends AbstractProcessor {

	/**
	 * The logger used by instances of this class to log messages.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(JimpleXMLizer.class);

	/**
	 * This indicates if the type that is being processed at present or just befor this point in time is a class or an
	 * interface.
	 */
	private String currType;

	/**
	 * The directory in which xmlized jimple will be dumped. If <code>null</code>, it will be redirected to standard
	 * output.
	 */
	private String dumpDirectory;

	/**
	 * The suffix to be added to jimple files when they dumped.
	 */
	private String fileSuffix;

	/**
	 * The id generator used during xmlization of the jimple.
	 * 
	 * @invariant idGenerator != null
	 */
	private final IJimpleIDGenerator idGenerator;

	/**
	 * This indicates if the processing of a class has begun. This is set in the callback for a class.
	 */
	private boolean processingClass;

	/**
	 * This indicates if the processing of a method has begun. This is set in the callback for a method.
	 */
	private boolean processingMethod;

	/**
	 * The Jimple statement xmlizer.
	 * 
	 * @invariant stmtXmlizer != null
	 */
	private final JimpleStmtXMLizer stmtXmlizer;

	/**
	 * The outputter to be used to write xml data.
	 */
	private XMLOutputter writer;

	/**
	 * Creates a new JimpleXMLizer object.
	 * 
	 * @param generator used to generate ids of xml elements.
	 * @pre generator != null
	 */
	public JimpleXMLizer(final IJimpleIDGenerator generator) {
		idGenerator = generator;
		stmtXmlizer = new JimpleStmtXMLizer(new JimpleValueXMLizer(generator), generator);
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#callback(soot.SootClass)
	 */
	@Override public final void callback(final SootClass clazz) {
		try {
			if (processingClass) {
				writer.endDocument();
			} else {
				processingClass = true;
			}

			// Create new file
			final String _classId = setupFile(clazz);

			if (clazz.isInterface()) {
				currType = "interface";
			} else {
				currType = "class";
			}
			writer.startTag(currType);
			writer.attribute("name", clazz.getJavaStyleName());
			writer.attribute("id", _classId);
			writer.attribute("package", clazz.getJavaPackageName());
			writer.attribute("abstract", String.valueOf(clazz.isAbstract()));
			writer.attribute("accessSpec", getAccessSpecifier(clazz.getModifiers()));

			if (clazz.hasSuperclass()) {
				final SootClass _sc = clazz.getSuperclass();
				writer.startTag("superclass");
				writer.attribute("typeId", idGenerator.getIdForClass(_sc));
				writer.endTag();
			}

			if (clazz.getInterfaceCount() > 0) {
				writer.startTag("interfaceList");

				@SuppressWarnings("unchecked") final Iterator<SootClass> _i = clazz.getInterfaces().iterator();
				for (; _i.hasNext();) {
					final SootClass _inter = _i.next();
					writer.startTag("superinterface");
					writer.attribute("typeId", idGenerator.getIdForClass(_inter));
					writer.endTag();
				}
				writer.endTag();
			}

			processingMethod = false;
		} catch (IOException _e) {
			LOGGER.warn("Error while writing xmlized jimple info.", _e);
		}
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#callback(soot.SootField)
	 */
	@Override public final void callback(final SootField field) {
		try {
			writer.startTag("field");
			writer.attribute("name", field.getName());
			writer.attribute("id", idGenerator.getIdForField(field));
			writer.attribute("typeId", idGenerator.getIdForType(field.getType()));
			writer.attribute("static", String.valueOf(field.isStatic()));
			writer.attribute("final", String.valueOf(field.isFinal()));

			String _accessSpec = "";

			if (field.isPublic()) {
				_accessSpec = "public";
			} else if (field.isPrivate()) {
				_accessSpec = "private";
			} else if (field.isProtected()) {
				_accessSpec = "proctected";
			}
			writer.attribute("accessSpec", _accessSpec);
			writer.endTag();
		} catch (final IOException _e) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Error while writing xmlized jimple info.", _e);
			}
		}
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#callback(soot.SootMethod)
	 */
	@SuppressWarnings("unchecked") @Override public final void callback(final SootMethod method) {
		try {
			if (processingMethod) {
				writer.endTag();
			} else {
				processingMethod = true;
			}
			writer.startTag("method");
			writer.attribute("name", AbstractXMLizer.xmlizeString(method.getName()));
			writer.attribute("id", idGenerator.getIdForMethod(method));

			// capture info about modifiers
			writer.attribute("abstract", String.valueOf(method.isAbstract()));
			writer.attribute("static", String.valueOf(method.isStatic()));
			writer.attribute("native", String.valueOf(method.isNative()));
			writer.attribute("synchronized", String.valueOf(method.isSynchronized()));

			writer.attribute("accessSpec", getAccessSpecifier(method.getModifiers()));

			// capture info about signature
			writer.startTag("signature");
			writer.startTag("returnType");
			writer.attribute("typeId", idGenerator.getIdForType(method.getReturnType()));
			writer.endTag();

			int _j = 0;

			for (final Iterator<Type> _i = method.getParameterTypes().iterator(); _i.hasNext();) {
				writer.startTag("paramType");
				writer.attribute("typeId", idGenerator.getIdForType(_i.next()));
				writer.attribute("position", String.valueOf(_j++));
				writer.endTag();
			}

			for (final Iterator<SootClass> _i = method.getExceptions().iterator(); _i.hasNext();) {
				writer.startTag("exception");
				writer.attribute("typeId", idGenerator.getIdForClass(_i.next()));
				writer.endTag();
			}
			writer.endTag();

			if (method.isConcrete()) {
				xmlizeTrapListAndLocals(method);
			}
		} catch (IOException _e) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Error while writing xmlized jimple info.", _e);
			}
		}
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#callback(soot.jimple.Stmt, edu.ksu.cis.indus.processing.Context)
	 */
	@Override public final void callback(final Stmt stmt, final Context context) {
		stmtXmlizer.setMethod(context.getCurrentMethod());
		stmtXmlizer.apply(stmt);
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#consolidate()
	 */
	@Override public final void consolidate() {
		if (writer != null) {
			try {
				writer.endDocument();
				writer.getWriter().close();
			} catch (final IOException _e) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Error while writing xmlized jimple info.", _e);
				}
			}
		}
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#hookup(edu.ksu.cis.indus.processing.ProcessingController)
	 */
	public final void hookup(final ProcessingController ppc) {
		ppc.registerForAllStmts(this);
		ppc.register(this);
	}

	/**
	 * Sets the options to xmlize jimple.
	 * 
	 * @param directory into which xmlized jimple will be dumped. If the given non-<code>null</code> directory does not
	 *            exist then it will be created. In case of <code>null</code> directory, it will dump the output into
	 *            <code>System.out</code>.
	 * @param suffix is appended to the generated file. If <code>null</code>, no suffix is appended.
	 */
	public final void setDumpOptions(final String directory, final String suffix) {
		dumpDirectory = directory;

		if (directory != null) {
			final File _dir = new File(directory);

			if (!_dir.exists()) {
				_dir.mkdirs();
			}
		}

		if (suffix == null) {
			fileSuffix = "";
		} else {
			fileSuffix = "_" + suffix;
		}
	}

	/**
	 * @see edu.ksu.cis.indus.processing.IProcessor#unhook(edu.ksu.cis.indus.processing.ProcessingController)
	 */
	public final void unhook(final ProcessingController ppc) {
		ppc.unregisterForAllStmts(this);
		ppc.unregister(this);
	}

	/**
	 * Retrieves the string that represents the access specifier in the given modifers.
	 * 
	 * @param modifiers from which the access specifier should be extracted as a string.
	 * @return the stringized form of the access specifier.
	 * @post result != null
	 */
	private String getAccessSpecifier(final int modifiers) {
		String _result = "";

		if (Modifier.isPublic(modifiers)) {
			_result = "public";
		} else if (Modifier.isPrivate(modifiers)) {
			_result = "private";
		} else if (Modifier.isProtected(modifiers)) {
			_result = "proctected";
		}
		return _result;
	}

	/**
	 * Sets up the file to write the xmlized jimple for the given class.
	 * 
	 * @param clazz to be xmlized.
	 * @return the id of the class.
	 * @throws IOException when there is an error while operating on the file corresponding to the class.
	 * @pre clazz != null
	 * @post result != null
	 */
	private String setupFile(final SootClass clazz) throws IOException {
		final String _classId = idGenerator.getIdForClass(clazz);

		if (dumpDirectory == null) {
			if (writer == null) {
				writer = new CustomXMLOutputter(new BufferedWriter(new OutputStreamWriter(System.out)));
			} else {
				writer.reset(writer.getWriter(), writer.getEncoding());
			}
			stmtXmlizer.setWriter(writer);
		} else {
			final String _filename = dumpDirectory + File.separator + _classId + fileSuffix + ".xml";

			try {
				if (writer != null && writer.getWriter() != null) {
					writer.getWriter().close();
				}

				final File _file = new File(_filename);
				writer = new CustomXMLOutputter(new BufferedWriter(new FileWriter(_file)));
				stmtXmlizer.setWriter(writer);
				writer.declaration();
			} catch (final IOException _e) {
				LOGGER.error("Exception while trying to open " + _filename, _e);
				throw _e;
			}
		}
		return _classId;
	}

	/**
	 * Xmlizes traps and locals in the given method.
	 * 
	 * @param method in which to xmlize.
	 * @pre method != null
	 */
	@SuppressWarnings("unchecked") private void xmlizeTrapListAndLocals(final SootMethod method) {
		final Body _body = method.retrieveActiveBody();

		final Collection<Trap> _traps = _body.getTraps();

		try {
			// capture info about traps
			if (!_traps.isEmpty()) {
				writer.startTag("traplist");

				for (final Iterator<Trap> _i = _traps.iterator(); _i.hasNext();) {
					final Trap _trap = _i.next();
					writer.startTag("trap");
					writer.attribute("typeId", idGenerator.getIdForClass(_trap.getException()));
					writer.attribute("beginId", idGenerator.getIdForStmt((Stmt) _trap.getBeginUnit(), method));
					writer.attribute("endId", idGenerator.getIdForStmt((Stmt) _trap.getEndUnit(), method));
					writer.attribute("handlerId", idGenerator.getIdForStmt((Stmt) _trap.getHandlerUnit(), method));
					writer.endTag();
				}
				writer.endTag();
			}

			// capture info about locals
			for (final Iterator<Local> _i = _body.getLocals().iterator(); _i.hasNext();) {
				final Local _l = _i.next();
				writer.startTag("local");
				writer.attribute("id", idGenerator.getIdForLocal(_l, method));
				writer.attribute("name", _l.getName());
				writer.attribute("typeId", idGenerator.getIdForType(_l.getType()));
				writer.endTag();
			}
		} catch (final IOException _e) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Error while writing xmlized jimple info.", _e);
			}
		}
	}
}

// End of File
