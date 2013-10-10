/*
 * #%L
 * SCIFIO library for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2011 - 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package io.scif;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Default {@link MetaTable} implementation. Provides a copying constructor and
 * a {@link #putList(String, Object)} implementation.
 * 
 * @see MetaTable
 * @author Mark Hiner
 */
public class DefaultMetaTable extends HashMap<String, Object> implements
	MetaTable
{

	// -- Constructors --

	/**
	 * Basic constructor
	 */
	public DefaultMetaTable() {

	}

	/**
	 * Construct a MetaTable and populate it using an existing map.
	 */
	public DefaultMetaTable(final Map<String, Object> copy) {
		for (final String k : copy.keySet())
			put(k, copy.get(k));
	}

	// -- MetaTable API Methods --

	@Override
	public void putList(final String key, final Object value) {
		Object list = get(key);

		if (list == null) list = new Vector<Object>();

		if (list instanceof Vector) {
			@SuppressWarnings("unchecked")
			Vector<Object> valueList = ((Vector<Object>) list);
			valueList.add(value);
		}
		else {
			final Vector<Object> v = new Vector<Object>();
			v.add(list);
			v.add(value);
			list = v;
		}

		put(key, list);
	}
}
