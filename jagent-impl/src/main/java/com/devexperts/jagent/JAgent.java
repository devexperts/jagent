package com.devexperts.jagent;

/*
 * #%L
 * JAgent Impl
 * %%
 * Copyright (C) 2015 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.*;
import java.util.*;

/**
 * Java agent abstraction that contains a lot of boilerplate code.
 *
 * Implementation of current class should have {@code (Instrumentation inst, String agentArgs)}
 * to run it via {@code JAgentRunner}.
 * Method {@link #go()} is executed to run agent's code.
 *
 * See {@code Sample}
 */
public abstract class JAgent {
    // Required parameters
    private final Instrumentation inst;
    private final List<ClassFileTransformer> transformers = new ArrayList<>();
    private final Log log;
    private final String agentName;
    private final String version;
    // Instance variables
    private boolean started = false;
    // Optional parameters
    private boolean redefine = false;
    private boolean isVerboseRedefinition = false;

    public JAgent(Instrumentation inst, String agentName, String version, Log log) {
        this.inst = inst;
        this.log = log;
        this.agentName = agentName;
        this.version = version;
    }

    /**
     * Add transformer to be used for redefining.
     *
     * @param transformer transformer to be used.
     */
    public void addTransformer(ClassFileTransformer transformer) {
        checkNotStarted();
        transformers.add(transformer);
    }

    /**
     * Add transformers to be used for redefining.
     *
     * @param transformers collection of transformers to be used.
     */
    public void addTransformers(Collection<ClassFileTransformer> transformers) {
        checkNotStarted();
        this.transformers.addAll(transformers);
    }

    /**
     * Set to {@code true} to redefine classes one at a time instead of all together.
     */
    public void setIsVerboseRedefinition(boolean isVerboseRedefinition) {
        checkNotStarted();
        this.isVerboseRedefinition = isVerboseRedefinition;
    }

    /**
     * Set to {@code true} to enable classes redefinition.
     */
    public void setRedefineClasses(boolean redefine) {
        this.redefine = redefine;
    }

    /**
     * Method that add transformers to instrumentation and redefines already loaded classes.
     * Use {@code JAgentRunner} to invoke it.
     */
    public void go() throws Exception {
        started = true;
        log.info("Loading ", agentName, " ", version, "...");
        if (redefine) {
            log.info("Start redefining with ", agentName);
            // redefine all classes loader so far
            for (ClassFileTransformer transformer : transformers)
                redefine(transformer);
            log.info("Done redefining with ", agentName, ".");
        }
        for (ClassFileTransformer transformer : transformers) {
            inst.addTransformer(transformer);
        }
    }

    private void redefine(ClassFileTransformer transformer)
            throws IllegalClassFormatException, ClassNotFoundException, UnmodifiableClassException {
        ArrayList<Class> classes = new ArrayList<>();
        HashSet<Class> done = new HashSet<>();
        FastByteBuffer buf = new FastByteBuffer();
        CachingClassFileTransformer ourTransformer = transformer instanceof CachingClassFileTransformer ?
            (CachingClassFileTransformer) transformer : null;
        for (int pass = 1; ; pass++) {
            classes.addAll(Arrays.asList(inst.getAllLoadedClasses()));
            List<ClassDefinition> cdl = new ArrayList<>(classes.size());
            log.debug("Redefining classes pass #", pass, "...");
            for (Class clazz : classes) {
                if (clazz.isArray())
                    continue;
                if (!done.add(clazz))
                    continue;
                String name = clazz.getName().replace('.', '/');
                if (ourTransformer != null && !ourTransformer.processClass(name, clazz.getClassLoader()))
                    continue;
                InputStream is = clazz.getResourceAsStream("/" + name + ".class");
                buf.clear();
                if (is != null)
                    try {
                        try {
                            buf.readFrom(is);
                        } finally {
                            is.close();
                        }
                    } catch (IOException e) {
                        log.warn("Failed to read class resource: ", name, e);
                    }
                if (buf.isEmpty()) {
                    log.warn("Cannot read class resource: ", name);
                    continue;
                }
                byte[] result = transformer.transform(
                        clazz.getClassLoader(), name, clazz, clazz.getProtectionDomain(), buf.getBytes());
                if (result != null)
                    cdl.add(new ClassDefinition(clazz, result));
            }
            classes.clear();
            if (cdl.isEmpty())
                break; // all classes were redefined
            log.debug("Redefining classes pass #", pass, "...");

            if (isVerboseRedefinition) {
                for (ClassDefinition cd : cdl) {
                    String name = cd.getDefinitionClass().getName();
                    log.debug("Redefining class ", name);
                    try {
                        inst.redefineClasses(cd);
                    } catch (Exception e) {
                        log.error("Failed to redefine class ", name, e);
                    }
                }
            } else {
                inst.redefineClasses(cdl.toArray(new ClassDefinition[cdl.size()]));
            }
        }
    }

    private void checkNotStarted() {
        if (started)
            throw new IllegalStateException("The agent was started already");
    }
}
